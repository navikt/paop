package no.nav.paop

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.prometheus.client.hotspot.DefaultExports
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
import no.nav.paop.mapping.extractOrgnavn
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFnr
import no.nav.paop.mapping.mapFormdataToFagmelding
import no.nav.paop.ws.configureBasicAuthFor
import no.nav.paop.ws.configureSTSFor
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Dokumentbestillingsinformasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagomraader
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagsystemer
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.NorskPostadresse
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
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
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.jms.MessageProducer
import javax.jms.Session
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.Marshaller
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

private val log = LoggerFactory.getLogger("nav.paop-application")
val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

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
        configureSTSFor(fastlegeregisteret, env.srvPaopUsername,
                env.srvPaopPassword, env.securityTokenServiceUrl)

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
            serviceClass = PartnerResource::class.java
        }.create() as PartnerResource
        configureBasicAuthFor(partnerEmottak, env.srvPaopUsername, env.srvPaopPassword)

        val arenaProducer = session.createProducer(arenaQueue)

        listen(PdfClient(env.pdfGeneratorURL), journalbehandling, fastlegeregisteret, organisasjonV4,
                dokumentProduksjonV3, adresseRegisterV1, partnerEmottak, arenaProducer, session, consumer).join()
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
    arenaProducer: MessageProducer,
    session: Session,
    consumer: KafkaConsumer<String, ExternalAttachment>
) = launch {

    while (true) {
        consumer.poll(Duration.ofMillis(0)).forEach {
            log.info("Recived a kafka message")

            val dataBatch = dataBatchUnmarshaller.unmarshal(StringReader(it.value().getBatch())) as DataBatch
            val serviceCode = it.value().getServiceCode()
            val serviceEditionCode = it.value().getServiceEditionCode()
            val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
            var oppfolgingsplanType = findOppfolingsplanType(serviceCode, serviceEditionCode)
            val archiveReference = it.value().getArchiveReference()
            val ediLoggId = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))}-paop-$archiveReference"
            oppfolgingsplanType = Oppfolginsplan.OP2016 // TODO: Delete after initial testing


            val validOrganizationNumber = try {
                organisasjonV4.validerOrganisasjon(ValiderOrganisasjonRequest().apply {
                    orgnummer = extractOrgNr(formData, oppfolgingsplanType)
                }).isGyldigOrgnummer
            } catch (e: Exception) {
                log.error("Failed to validate organization number due to an exception", e)
                false
            }

            if (!validOrganizationNumber) {
                // TODO: Do something else then silently fail
                return@forEach
            }

            if (oppfolgingsplanType in arrayOf(Oppfolginsplan.OP2012, Oppfolginsplan.OP2014, Oppfolginsplan.OP2016)) {
                if (isFollowupPlanForNAV(formData, oppfolgingsplanType)) {
                    val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFormdataToFagmelding(formData, oppfolgingsplanType))
                    val joarkRequest = createJoarkRequest(formData, oppfolgingsplanType, ediLoggId, fagmelding)
                    journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                    sendArenaOppfolginsplan(arenaProducer, session, formData, archiveReference, ediLoggId, oppfolgingsplanType)
                }
                if (isFollowupPlanForFastlege(formData, oppfolgingsplanType)) {
                    handleDoctorFollowupPlanAltinn(fastlegeregisteret, dokumentProduksjonV3, adresseRegisterV1,
                            partnerEmottak, arenaProducer, session, formData, archiveReference, ediLoggId,
                            oppfolgingsplanType)
                } else {
                    handleNonFastlegeFollowupPlan(organisasjonV4, dokumentProduksjonV3, arenaProducer, session,
                            formData, archiveReference, ediLoggId, oppfolgingsplanType)
                }
            } else if (oppfolgingsplanType == Oppfolginsplan.NAVOPPFPLAN) {
                val extractOppfolginsplan = extractNavOppfPlan(formData)
                val usesNavTemplate = !extractOppfolginsplan.isBistandHjelpemidler

                if (usesNavTemplate) {
                    if (extractOppfolginsplan.mottaksinformasjon.isOppfoelgingsplanSendesTiNav) {
                        val fagmelding = dataBatch.attachments.attachment.first().value
                        val joarkRequest = createJoarkRequest(formData, oppfolgingsplanType, ediLoggId, fagmelding)
                        journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                        sendArenaOppfolginsplan(arenaProducer, session, formData, archiveReference, ediLoggId, oppfolgingsplanType)
                    }
                    if (extractOppfolginsplan.mottaksinformasjon.isOppfoelgingsplanSendesTilFastlege) {
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
                            val orgname = extractGPName(patientToGPContractAssociation)!!
                            val orgpostnummer = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().postalCode.toString()
                            val orgpoststed = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().city.value.toString()

                            createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, formData, oppfolgingsplanType, orgname, orgpostnummer, orgpoststed, archiveReference, ediLoggId)
                        }
                    } else {

                        val hentOrganisasjonRequest = HentOrganisasjonRequest().apply {
                            orgnummer = extractOrgNr(formData, oppfolgingsplanType)
                        }
                        val hentOrganisasjonResponse = organisasjonV4.hentOrganisasjon(hentOrganisasjonRequest)

                        val finnOrganisasjonRequest = FinnOrganisasjonRequest().apply {
                            navn = hentOrganisasjonResponse.organisasjon.navn.toString()
                        }
                        val finnOrganisasjonResponse = organisasjonV4.finnOrganisasjon(finnOrganisasjonRequest)

                        val orgname = extractOrgnavn(formData, oppfolgingsplanType)!!
                        val orgpostnummer = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()!!.postnummer.value
                        val orgpoststed = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()!!.poststed
                        createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, formData, oppfolgingsplanType, orgname, orgpostnummer, orgpoststed, archiveReference, ediLoggId)
                    }
                }
            } else {
                val fagmelding = dataBatch.attachments.attachment.first().value
                val joarkRequest = createJoarkRequest(formData, oppfolgingsplanType, ediLoggId, fagmelding)
                journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)
                sendArenaOppfolginsplan(arenaProducer, session, formData, archiveReference, ediLoggId, oppfolgingsplanType)
            }
        }
        delay(100)
    }
}

fun createPhysicalLetter(
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session,
    formData: String,
    oppfolgingsplanType: Oppfolginsplan,
    organizationName: String,
    postnummer: String,
    poststed: String,
    archiveReference: String,
    ediLoggId: String
) {
    val brevrequest = createProduserIkkeredigerbartDokumentRequest(formData, oppfolgingsplanType, organizationName, postnummer, poststed, ediLoggId)
    try {
        dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
        letterSentNotificationToArena(arenaProducer, session, formData, archiveReference, ediLoggId, oppfolgingsplanType)
    } catch (e: Exception) {
        log.error("Call to dokprod returned Exception", e)
    }
}

fun handleNonFastlegeFollowupPlan(
    organisasjonV4: OrganisasjonV4,
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session,
    formData: String,
    archiveReference: String,
    ediLoggId: String,
    oppfolgingsplanType: Oppfolginsplan
) {
    val hentOrganisasjonRequest = HentOrganisasjonRequest().apply {
        orgnummer = extractOrgNr(formData, oppfolgingsplanType)
    }
    val hentOrganisasjonResponse = organisasjonV4.hentOrganisasjon(hentOrganisasjonRequest)

    val finnOrganisasjonRequest = FinnOrganisasjonRequest().apply {
        navn = hentOrganisasjonResponse.organisasjon.navn.toString()
    }
    val finnOrganisasjonResponse = organisasjonV4.finnOrganisasjon(finnOrganisasjonRequest)

    val orgname = extractOrgnavn(formData, oppfolgingsplanType)!!
    val orgpostnummer = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()!!.postnummer.value
    val orgpoststed = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()!!.poststed

    createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, formData, oppfolgingsplanType, orgname,
            orgpostnummer, orgpoststed, archiveReference, ediLoggId)
}

fun handleDoctorFollowupPlanAltinn(
    fastlegeregisteret: IFlrReadOperations,
    dokumentProduksjonV3: DokumentproduksjonV3,
    adresseRegisterV1: ICommunicationPartyService,
    partnerEmottak: PartnerResource,
    arenaProducer: MessageProducer,
    session: Session,
    formData: String,
    archiveReference: String,
    ediLoggId: String,
    oppfolgingsplanType: Oppfolginsplan
) {
    val patientFnr = extractSykmeldtArbeidstakerFnr(formData, oppfolgingsplanType)
    val patientToGPContractAssociation = try {
        fastlegeregisteret.getPatientGPDetails(patientFnr)
    } catch (e: Exception) {
        log.error("Call to flr returned Exception", e)
        // TODO: We shouldn't just fail here
        null
    }

    if (patientToGPContractAssociation != null) {
        val gpName = extractGPName(patientToGPContractAssociation)!!
        val gpOfficePostnr = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().postalCode.toString()
        val gpOfficePoststed = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().city.value

        val herIdFlr = patientToGPContractAssociation.gpHerId.value

        val getCommunicationPartyDetailsResponse = adresseRegisterV1.getOrganizationPersonDetails(herIdFlr)

        // Should only return one org
        val herIDAdresseregister = getCommunicationPartyDetailsResponse.organizations.value.organization.first().herId
        val gpOrganizationNumber = getCommunicationPartyDetailsResponse.organizations.value.organization.first().organizationNumber

        val hentPartnerIDViaOrgnummerRequest = HentPartnerIDViaOrgnummerRequest().apply {
            orgnr = gpOrganizationNumber.toString()
        }

        val hentPartnerIDViaOrgnummerResponse = partnerEmottak.hentPartnerIDViaOrgnummer(hentPartnerIDViaOrgnummerRequest)

        val canReceiveDialogMessage = hentPartnerIDViaOrgnummerResponse.partnerInformasjon.firstOrNull {
            it.heRid.toInt() == herIDAdresseregister
        }
        if (canReceiveDialogMessage != null) {
            // TODO: Send a dialogmelding to Emottak and fastlege
        } else {
            createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, formData, oppfolgingsplanType, gpName,
                    gpOfficePostnr, gpOfficePoststed, archiveReference, ediLoggId)
        }
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
    archiveReference: String,
    edilogg: String,
    oppfolgingPlanType: Oppfolginsplan
) = producer.send(session.createTextMessage().apply {
    val info = createArenaOppfolgingsplan(formdata, archiveReference, edilogg, oppfolgingPlanType)
    text = arenaMarshaller.toString(info)
})

fun letterSentNotificationToArena(
    producer: MessageProducer,
    session: Session,
    formdata: String,
    archiveReference: String,
    edilogg: String,
    oppfolgingPlanType: Oppfolginsplan
) = producer.send(session.createTextMessage().apply {
    val info = createArenaBrevTilArbeidsgiver(formdata, archiveReference, edilogg, oppfolgingPlanType)
    text = arenabrevMarshaller.toString(info)
})

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

fun isFollowupPlanForFastlege(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean = when (oppfolgingPlanType) {
    Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
    Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
    Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
    else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
} ?: false

fun isFollowupPlanForNAV(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean = when (oppfolgingPlanType) {
    Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
    Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
    Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
    else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
} ?: false

fun extractGPName(patientToGPContractAssociation: PatientToGPContractAssociation): String? =
        patientToGPContractAssociation.doctorCycles.value.gpOnContractAssociation.first().gp.value.let {
            "${it.firstName.value} ${it.middleName.value} ${it.lastName.value}"
        }

val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().let {
    it.isNamespaceAware = true
    it.newDocumentBuilder()
}
fun wrapFormData(formData: String): Element = documentBuilder.parse(InputSource(StringReader(formData))).documentElement

fun createProduserIkkeredigerbartDokumentRequest(
    formData: String,
    oppfolgingPlanType: Oppfolginsplan,
    mottakernavn: String?,
    postnummerString: String?,
    poststedString: String?,
    edilogg: String
): ProduserIkkeredigerbartDokumentRequest = ProduserIkkeredigerbartDokumentRequest().apply {
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
            navn = mottakernavn
            ident = extractOrgNr(formData, oppfolgingPlanType)
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
            postnummer = postnummerString
            poststed = poststedString
        }
        isFerdigstillForsendelse = true
        isInkludererEksterneVedlegg = false
    }
    brevdata = wrapFormData(formData)
}
