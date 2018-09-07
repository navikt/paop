package no.nav.paop

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.emottak.schemas.HentPartnerIDViaOrgnummerRequest
import no.nav.emottak.schemas.PartnerResource
import no.nav.model.dataBatch.DataBatch
import no.nav.paop.client.PdfClient
import no.nav.paop.client.PdfType
import no.nav.paop.client.createArenaBrevTilArbeidsgiver
import no.nav.paop.client.createArenaOppfolgingsplan
import no.nav.paop.client.createJoarkRequest
import no.nav.paop.mapping.extractOrgNr
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFnr
import no.nav.paop.mapping.mapFormdataToFagmelding
import no.nav.paop.metrics.RETRY_COUNTER
import no.nav.paop.metrics.WS_CALL_TIME
import no.nav.paop.sts.configureSTSFor
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Dokumentbestillingsinformasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagomraader
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagsystemer
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.NorskPostadresse
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.ValiderOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.ValiderOrganisasjonUgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.FinnOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.adresseregisteret.ICommunicationPartyService
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Queue
import javax.jms.Session
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.Marshaller
import javax.xml.ws.WebServiceException

private val log = LoggerFactory.getLogger("nav.paop-application")
val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

val retryInterval = arrayOf(1000L, 1000L * 60, 2000L * 60, 2000L * 60, 5000L * 60)
class PaopApplication

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val env = Environment()
    createHttpServer(applicationVersion = env.appVersion)

    val consumerProperties = readConsumerConfig(env)
    val consumer = KafkaConsumer<String, ExternalAttachment>(consumerProperties)
    consumer.subscribe(listOf(env.kafkaTopicOppfolginsplan))

    connectionFactory(env).createConnection(env.mqUsername, env.mqPassword).use {
        connection ->
        connection.start()

        val session = connection.createSession()
        val arenaQueue = session.createQueue(env.arenaIAQueue)
        session.close()

        val interceptorProperties = mapOf(
                WSHandlerConstants.USER to env.srvPaopUsername,
                WSHandlerConstants.ACTION to WSHandlerConstants.USERNAME_TOKEN,
                WSHandlerConstants.PASSWORD_TYPE to WSConstants.PW_TEXT,
                WSHandlerConstants.PW_CALLBACK_REF to CallbackHandler {
                    (it[0] as WSPasswordCallback).password = env.srvPaopPassword
                }
        )

        val fastlegeregisteret = JaxWsProxyFactoryBean().apply {
            address = env.fastlegeregiserHdirURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            serviceClass = IFlrReadOperations::class.java
        }.create() as IFlrReadOperations
        configureSTSFor(fastlegeregisteret, env.srvEiaUsername,
                env.srvEIaPassword, env.securityTokenServiceUrl)

        val organisasjonV4 = JaxWsProxyFactoryBean().apply {
            address = env.organisasjonV4EndpointURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            serviceClass = OrganisasjonV4::class.java
        }.create() as OrganisasjonV4
        configureSTSFor(organisasjonV4, env.srvPaopUsername,
                env.srvPaopPassword, env.securityTokenServiceUrl)

        val journalbehandling = JaxWsProxyFactoryBean().apply {
            address = env.journalbehandlingEndpointURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            outInterceptors.add(WSS4JOutInterceptor(interceptorProperties))
            serviceClass = Journalbehandling::class.java
        }.create() as Journalbehandling

        val dokumentProduksjonV3 = JaxWsProxyFactoryBean().apply {
            address = env.dokumentproduksjonV3EndpointURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            serviceClass = DokumentproduksjonV3::class.java
        }.create() as DokumentproduksjonV3
        configureSTSFor(dokumentProduksjonV3, env.srvPaopUsername,
                env.srvPaopPassword, env.securityTokenServiceUrl)

        val adresseRegisterV1 = JaxWsProxyFactoryBean().apply {
            address = env.adresseregisteretV1EmottakEndpointURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            serviceClass = ICommunicationPartyService::class.java
        }.create() as ICommunicationPartyService
        configureSTSFor(adresseRegisterV1, env.srvPaopUsername,
                env.srvPaopPassword, env.securityTokenServiceUrl)

        val partnerEmottak = JaxWsProxyFactoryBean().apply {
            address = env.partnerEmottakEndpointURL
            features.add(LoggingFeature())
            features.add(WSAddressingFeature())
            outInterceptors.add(WSS4JOutInterceptor(interceptorProperties))
            serviceClass = PartnerResource::class.java
        }.create() as PartnerResource

        listen(PdfClient(env.pdfGeneratorURL),
                journalbehandling, fastlegeregisteret, organisasjonV4, dokumentProduksjonV3, adresseRegisterV1, partnerEmottak, arenaQueue, connection, consumer)
                .join()
    }
}

fun listen(
    pdfClient: PdfClient,
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
    dokumentProduksjonV3: DokumentproduksjonV3,
    adresseRegisterV1: ICommunicationPartyService,
    partnerEmottak: PartnerResource,
    arenaQueue: Queue,
    connection: Connection,
    consumer: KafkaConsumer<String, ExternalAttachment>
) = launch {

    while (true) {
        consumer.poll(Duration.ofMillis(0)).forEach {
            log.info("Recived a kafka message:")

            val dataBatch = dataBatchUnmarshaller.unmarshal(StringReader(it.value().getBatch())) as DataBatch
            val serviceCode = it.value().getServiceCode()
            val serviceEditionCode = it.value().getServiceEditionCode()
            val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
            var oppfolgingslplanType = findOppfolingsplanType(serviceCode, serviceEditionCode)
            val archiveReference = it.value().getArchiveReference()
            val edilogg = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))}-paop-$archiveReference"
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val arenaProducer = session.createProducer(arenaQueue)
            oppfolgingslplanType = Oppfolginsplan.OP2016 // TODO slette denne etter testing

            if (oppfolgingslplanType == Oppfolginsplan.OP2012 || oppfolgingslplanType == Oppfolginsplan.OP2014 || oppfolgingslplanType == Oppfolginsplan.OP2016) {
                val validerOrganisasjonRequest = ValiderOrganisasjonRequest().apply {
                    orgnummer = extractOrgNr(formData, oppfolgingslplanType)
                }
                log.info("validerOrganisasjon request org nr: ", validerOrganisasjonRequest.orgnummer.toString())

                val gyldindOrgnummer = try {
                    organisasjonV4.validerOrganisasjon(validerOrganisasjonRequest).isGyldigOrgnummer
                } catch (e: ValiderOrganisasjonOrganisasjonIkkeFunnet) {
                log.error("validerOrganisasjon failed: ", e)
                } catch (e: ValiderOrganisasjonUgyldigInput) {
                    log.error("validerOrganisasjon failed: ", e)
                } catch (e: Exception) {
                    log.error("validerOrganisasjon failed: ", e)
                }

                if (gyldindOrgnummer == true) {
                    val letterToGP = extractOppfolgingsplanSendesTilFastlege(formData, oppfolgingslplanType)
                    val letterToNAV = extractOppfolgingsplanSendesTiNav(formData, oppfolgingslplanType)

                    if (letterToNAV == true) {
                        val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFormdataToFagmelding(formData, oppfolgingslplanType))
                        val joarkRequest = createJoarkRequest(formData, oppfolgingslplanType, edilogg, fagmelding)
                        journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                        sendArenaOppfolginsplan(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
                    }
                    if (letterToGP == true) {
                        var fastlegefunnet = false
                        val patientFnr = extractSykmeldtArbeidstakerFnr(formData, oppfolgingslplanType)
                        var patientToGPContractAssociation = PatientToGPContractAssociation()
                        try {
                            patientToGPContractAssociation = fastlegeregisteret.getPatientGPDetails(patientFnr)
                            fastlegefunnet = true
                        } catch (e: Exception) {
                            log.error("Call to flr returned Exception", e)
                        }

                        if (fastlegefunnet && patientToGPContractAssociation.gpContract != null) {
                            val gpFNR = patientToGPContractAssociation.doctorCycles.value.gpOnContractAssociation.first().gp.value.nin
                            val orgnrGp = patientToGPContractAssociation.gpContract.value.gpOffice.value.organizationNumber

                            val herIdFlr = patientToGPContractAssociation.gpHerId

                            val getCommunicationPartyDetailsResponse = adresseRegisterV1.getOrganizationPersonDetails(herIdFlr.value)

                            // Should only return one org
                            val herIDAdresseregister = getCommunicationPartyDetailsResponse.organizations.value.organization.first().herId

                            val hentPartnerIDViaOrgnummerRequest = HentPartnerIDViaOrgnummerRequest().apply {
                                orgnr = extractOrgNr(formData, oppfolgingslplanType)
                            }

                            val hentPartnerIDViaOrgnummerResponse = partnerEmottak.hentPartnerIDViaOrgnummer(hentPartnerIDViaOrgnummerRequest)

                            val canReviceDialogmelding = hentPartnerIDViaOrgnummerResponse.partnerInformasjon.firstOrNull {
                                it.heRid.toInt() == herIDAdresseregister
                            }
                            if (canReviceDialogmelding != null) {
                                // send dialogmelding to Emootak
                            }

                            val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                                dokumentbestillingsinformasjon = Dokumentbestillingsinformasjon().apply {
                                    dokumenttypeId = "brev"
                                    bestillendeFagsystem = Fagsystemer().apply {
                                        value = "PAOP"
                                    }
                                    bruker = Person().apply {
                                        navn = "NAV Servicesenter"
                                        ident = "NAV ORGNR"
                                    }
                                    mottaker = Person().apply {
                                        navn = extractGPName(patientToGPContractAssociation)
                                        ident = herIdFlr.toString()
                                    }
                                    journalsakId = edilogg
                                    sakstilhoerendeFagsystem = Fagsystemer().apply {
                                        value = "ARENA"
                                    }
                                    dokumenttilhoerendeFagomraade = Fagomraader().apply {
                                        value = "Sykefravær"
                                    }
                                    journalfoerendeEnhet = "N/A"
                                    adresse = NorskPostadresse().apply {
                                        adresselinje1 = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().streetAddress.value
                                        land = Landkoder().apply {
                                            value = "NO"
                                        }
                                        postnummer = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().postalCode.toString()
                                        poststed = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().city.value
                                    }
                                    isFerdigstillForsendelse = true
                                    isInkludererEksterneVedlegg = false
                                    brevdata = "The message to send out"
                                }
                            }

                            try {
                                val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                                letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
                            } catch (e: Exception) {
                                log.error("Call to dokprod returned Exception", e)
                            }
                        }
                    } else {

                        val hentOrganisasjonRequest = HentOrganisasjonRequest().apply {
                            orgnummer = extractOrgNr(formData, oppfolgingslplanType)
                        }
                        val hentOrganisasjonResponse = organisasjonV4.hentOrganisasjon(hentOrganisasjonRequest)

                        val finnOrganisasjonRequest = FinnOrganisasjonRequest().apply {
                            navn = hentOrganisasjonResponse.organisasjon.navn.toString()
                        }
                        val finnOrganisasjonResponse = organisasjonV4.finnOrganisasjon(finnOrganisasjonRequest)

                        val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                            dokumentbestillingsinformasjon = Dokumentbestillingsinformasjon().apply {
                                dokumenttypeId = "brev"
                                bestillendeFagsystem = Fagsystemer().apply {
                                    value = "PAOP"
                                }
                                bruker = Person().apply {
                                    navn = "NAV Servicesenter"
                                    ident = "NAV ORGNR"
                                }
                                mottaker = Person().apply {
                                    navn = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()?.redigertNavn
                                    ident = extractOrgNr(formData, oppfolgingslplanType)
                                }
                                journalsakId = edilogg
                                sakstilhoerendeFagsystem = Fagsystemer().apply {
                                    value = "ARENA"
                                }
                                dokumenttilhoerendeFagomraade = Fagomraader().apply {
                                    value = "Sykefravær"
                                }
                                journalfoerendeEnhet = "N/A"
                                adresse = NorskPostadresse().apply {
                                    adresselinje1 = "stat"
                                    land = Landkoder().apply {
                                        value = "NOR"
                                    }
                                    postnummer = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()?.postnummer?.value
                                    poststed = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()?.poststed
                                }
                                isFerdigstillForsendelse = true
                                isInkludererEksterneVedlegg = false
                                brevdata = "The message to send out"
                            }
                        }

                        try {
                            val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                            letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
                        } catch (e: Exception) {
                            log.error("Call to dokprod returned Exception", e)
                        }
                    }
                }
            } else if (oppfolgingslplanType == Oppfolginsplan.NAVOPPFPLAN) {
                val extractOppfolginsplan = extractNavOppfPlan(formData)
                val navmal = !extractOppfolginsplan.isBistandHjelpemidler
                val letterToGP = extractOppfolginsplan.mottaksinformasjon.isOppfoelgingsplanSendesTilFastlege
                val letterToNAV = extractOppfolginsplan.mottaksinformasjon.isOppfoelgingsplanSendesTiNav

                if (navmal) {
                    val validerOrganisasjonRequest = ValiderOrganisasjonRequest().apply {
                        orgnummer = extractOrgNr(formData, oppfolgingslplanType)
                    }
                    val validerOrganisasjonResponse = organisasjonV4.validerOrganisasjon(validerOrganisasjonRequest)

                    if (validerOrganisasjonResponse.isGyldigOrgnummer) {

                        if (letterToNAV) {
                            val fagmelding = dataBatch.attachments.attachment.first().value
                            val joarkRequest = createJoarkRequest(formData, oppfolgingslplanType, edilogg, fagmelding)
                            journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                            sendArenaOppfolginsplan(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
                        }
                        if (letterToGP) {
                            var fastlegefunnet = false
                            val patientFnr = extractOppfolginsplan.fodselsNr
                            var patientToGPContractAssociation = PatientToGPContractAssociation()
                            try {
                                patientToGPContractAssociation = fastlegeregisteret.getPatientGPDetails(patientFnr)
                                fastlegefunnet = true
                            } catch (e: Exception) {
                                log.error("Call to flr returned Exception", e)
                            }

                            if (fastlegefunnet && patientToGPContractAssociation.gpContract != null) {
                                val gpFNR = patientToGPContractAssociation.doctorCycles.value.gpOnContractAssociation.first().gp.value.nin
                                val orgnrGp = patientToGPContractAssociation.gpContract.value.gpOffice.value.organizationNumber

                                val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                                }
                                try {
                                    val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                                    letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
                                } catch (e: Exception) {
                                    log.error("Call to dokprod returned Exception", e)
                                }
                            }
                        } else {
                            val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                            }

                            try {
                                val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                                letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
                            } catch (e: Exception) {
                                log.error("Call to dokprod returned Exception", e)
                            }
                        }
                    }
                }
            } else {
                val fagmelding = dataBatch.attachments.attachment.first().value
                val joarkRequest = createJoarkRequest(formData, oppfolgingslplanType, edilogg, fagmelding)
                journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)
                sendArenaOppfolginsplan(arenaProducer, session, formData, dataBatch, edilogg, oppfolgingslplanType)
            }
        }
        delay(100)
    }
}

fun connectionFactory(environment: Environment) = MQConnectionFactory().apply {
    hostName = environment.mqHostname
    port = environment.mqPort
    queueManager = environment.mqQueueManagerName
    transportType = WMQConstants.WMQ_CM_CLIENT
    // TODO mq crypo
    // sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = environment.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun sendArenaOppfolginsplan(
    producer: MessageProducer,
    session: Session,
    formdata: String,
    databatch: DataBatch,
    edilogg: String,
    oppfolgingPlanType: Oppfolginsplan
) = producer.send(session.createTextMessage().apply {
    val info = createArenaOppfolgingsplan(databatch, formdata, edilogg, oppfolgingPlanType)
    text = arenaMarshaller.toString(info)
})

fun letterSentNotificationToArena(
    producer: MessageProducer,
    session: Session,
    formdata: String,
    databatch: DataBatch,
    edilogg: String,
    oppfolgingPlanType: Oppfolginsplan
) = producer.send(session.createTextMessage().apply {
    val info = createArenaBrevTilArbeidsgiver(databatch, formdata, edilogg, oppfolgingPlanType)
    text = arenabrevMarshaller.toString(info)
})

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

fun extractOppfolgingsplanSendesTilFastlege(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractOppfolgingsplanSendesTiNav(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractGPName(patientToGPContractAssociation: PatientToGPContractAssociation): String? =
        "${patientToGPContractAssociation.doctorCycles.value.gpOnContractAssociation.first().gp.value.firstName.value} " +
                "${patientToGPContractAssociation.doctorCycles.value.gpOnContractAssociation.first().gp.value.middleName.value}" +
                "${patientToGPContractAssociation.doctorCycles.value.gpOnContractAssociation.first().gp.value.lastName.value}"

fun <T> retryWithInterval(interval: Array<Long>, callName: String, blocking: suspend () -> T): Deferred<T> {
    return async {
        for (time in interval) {
            try {
                WS_CALL_TIME.labels(callName).startTimer().use {
                    return@async blocking()
                }
            } catch (e: WebServiceException) {
                if (e.cause !is IOException)
                    throw e
                log.warn("Caught IO exception trying to reach {}, retrying", callName, e)
            }
            RETRY_COUNTER.labels(callName).inc()
            Thread.sleep(time)
        }

        WS_CALL_TIME.labels(callName).startTimer().use {
            blocking()
        }
    }
}