package no.nav.paop

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import no.nav.model.dataBatch.DataBatch
import no.nav.paop.client.PdfClient
import no.nav.paop.client.PdfType
import no.nav.paop.client.Samhandler
import no.nav.paop.client.SamhandlerPraksis
import no.nav.paop.client.SarClient
import no.nav.paop.client.createArenaBrevTilArbeidsgiver
import no.nav.paop.client.createArenaOppfolgingsplan
import no.nav.paop.client.createJoarkRequest
import no.nav.paop.mapping.extractOrgNr
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFnr
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFornavn
import no.nav.paop.mapping.mapFormdataToFagmelding
import no.nav.paop.sts.configureSTSFor
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Dokumentbestillingsinformasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagsystemer
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Queue
import javax.jms.Session
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.Marshaller

private val log = LoggerFactory.getLogger("nav.paop-application")
val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
class PaopApplication

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()
    createHttpServer(applicationVersion = fasitProperties.appVersion)

    // TODO read from kafka topic
    val consumerProperties = kafkaProperties("/kafka_producer.properties", fasitProperties)

    KafkaConsumer<String, String>(consumerProperties).apply {
        subscribe(listOf(fasitProperties.kafkaTopicOppfolginsplan))
        while (true) {
            poll(1000).forEach { record -> log.info("Consumed a message: ${record.value()}") }
        }
    }

    connectionFactory(fasitProperties).createConnection(fasitProperties.mqUsername, fasitProperties.mqPassword).use {
        connection ->
        connection.start()

        val session = connection.createSession()
        val arenaQueue = session.createQueue(fasitProperties.arenaIAQueue)
        session.close()

        val fastlegeregisteret = JaxWsProxyFactoryBean().apply {
            address = fasitProperties.fastlegeregiserHdirURL
            features.add(LoggingFeature())
            serviceClass = IFlrReadOperations::class.java
        }.create() as IFlrReadOperations
        configureSTSFor(fastlegeregisteret, fasitProperties.srvEiaUsername,
                fasitProperties.srvEiaPassword, fasitProperties.securityTokenServiceUrl)

        val interceptorProperties = mapOf(
                WSHandlerConstants.USER to fasitProperties.srvPaopUsername,
                WSHandlerConstants.ACTION to WSHandlerConstants.USERNAME_TOKEN,
                WSHandlerConstants.PASSWORD_TYPE to WSConstants.PW_TEXT,
                WSHandlerConstants.PW_CALLBACK_REF to CallbackHandler {
                    (it[0] as WSPasswordCallback).password = fasitProperties.srvPaopPassword
                }
        )

        val organisasjonV4 = JaxWsProxyFactoryBean().apply {
            address = fasitProperties.organisasjonv4EndpointURL
            features.add(LoggingFeature())
            serviceClass = OrganisasjonV4::class.java
        }.create() as OrganisasjonV4
        configureSTSFor(organisasjonV4, fasitProperties.srvPaopUsername,
                fasitProperties.srvPaopPassword, fasitProperties.securityTokenServiceUrl)

        val journalbehandling = JaxWsProxyFactoryBean().apply {
            address = fasitProperties.journalbehandlingEndpointURL
            features.add(LoggingFeature())
            outInterceptors.add(WSS4JOutInterceptor(interceptorProperties))
            serviceClass = Journalbehandling::class.java
        }.create() as Journalbehandling

        val dokumentProduksjonV3 = JaxWsProxyFactoryBean().apply {
            address = fasitProperties.organisasjonv4EndpointURL
            features.add(LoggingFeature())
            serviceClass = DokumentproduksjonV3::class.java
        }.create() as DokumentproduksjonV3
        configureSTSFor(dokumentProduksjonV3, fasitProperties.srvPaopUsername,
                fasitProperties.srvPaopPassword, fasitProperties.securityTokenServiceUrl)

        val sarClient = SarClient(fasitProperties.kuhrSarApiURL, fasitProperties.srvPaopUsername,
                fasitProperties.srvPaopPassword)

        listen(PdfClient(fasitProperties.pdfGeneratorURL),
                journalbehandling, fastlegeregisteret, organisasjonV4, dokumentProduksjonV3, sarClient, arenaQueue, connection)
                .join()
    }
}

fun listen(
    pdfClient: PdfClient,
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
    dokumentProduksjonV3: DokumentproduksjonV3,
    sarClient: SarClient,
    arenaQueue: Queue,
    connection: Connection
) = launch {
    val dataBatch = extractDataBatch("asd")
    val serviceCode = dataBatch.dataUnits.dataUnit.first().formTask.serviceCode
    val serviceEditionCode = dataBatch.dataUnits.dataUnit.first().formTask.serviceEditionCode
    val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
    val oppfolgingslplanType = findOppfolingsplanType(serviceCode, serviceEditionCode)
    val archiveReference = dataBatch.dataUnits.dataUnit.first().archiveReference
    val edilogg = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))}-paop-$archiveReference"
    val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val arenaProducer = session.createProducer(arenaQueue)

    if (oppfolgingslplanType == Oppfolginsplan.OP2012 || oppfolgingslplanType == Oppfolginsplan.OP2014 || oppfolgingslplanType == Oppfolginsplan.OP2016) {
        val organisasjonRequest = ValiderOrganisasjonRequest().apply {
            orgnummer = extractOrgNr(formData, oppfolgingslplanType)
        }
        if (organisasjonV4.validerOrganisasjon(organisasjonRequest).isGyldigOrgnummer) {
            val letterToGP = extractOppfolgingsplanSendesTilFastlege(formData, oppfolgingslplanType)
            val letterToNAV = extractOppfolgingsplanSendesTiNav(formData, oppfolgingslplanType)

        if (letterToNAV == true) {
            val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFormdataToFagmelding(formData, oppfolgingslplanType))
            val joarkRequest = createJoarkRequest(dataBatch, formData, oppfolgingslplanType, edilogg, archiveReference, fagmelding)
            journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

            sendArenaOppfolginsplan(arenaProducer, session, formData, dataBatch, edilogg)
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
                    val samhandler = sarClient.getSamhandler(gpFNR.toString())
                    val samhandlerPraksis = findSamhandlerPraksis(samhandler, orgnrGp)
                    val tssid = samhandlerPraksis?.tss_ident
                    val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                        dokumentbestillingsinformasjon = Dokumentbestillingsinformasjon().apply {
                        dokumenttypeId = "brev"
                        bestillendeFagsystem = Fagsystemer().apply {
                            value = "PAOP"
                        }
                        bruker = Person().apply {
                            navn = extractSykmeldtArbeidstakerFornavn(formData, oppfolgingslplanType)
        // norskIdent
                        }
                        }
                    }

                    try {
                        val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                        letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg)
                    } catch (e: Exception) {
                        log.error("Call to dokprod returned Exception", e)
                    }
                }
            } else {
            val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
            }

            try {
                val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg)
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
            val organisasjonRequest = ValiderOrganisasjonRequest().apply {
                orgnummer = extractOppfolginsplan.bedriftsNr
            }
            if (organisasjonV4.validerOrganisasjon(organisasjonRequest).isGyldigOrgnummer) {

                if (letterToNAV) {
                    val fagmelding = dataBatch.attachments.attachment.first().value
                    val joarkRequest = createJoarkRequest(dataBatch, formData, oppfolgingslplanType, edilogg, archiveReference, fagmelding)
                    journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

                    sendArenaOppfolginsplan(arenaProducer, session, formData, dataBatch, edilogg)
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
                        // CALL KUHR SAR
                        val samhandler = sarClient.getSamhandler(gpFNR.toString())
                        val samhandlerPraksis = findSamhandlerPraksis(samhandler, orgnrGp)
                        val tssid = samhandlerPraksis?.tss_ident
                        val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                        }
                        try {
                            val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                            letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg)
                        } catch (e: Exception) {
                            log.error("Call to dokprod returned Exception", e)
                        }
                    }
                } else {
                    val brevrequest = ProduserIkkeredigerbartDokumentRequest().apply {
                    }

                    try {
                        val brevRespone = dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
                        letterSentNotificationToArena(arenaProducer, session, formData, dataBatch, edilogg)
                    } catch (e: Exception) {
                        log.error("Call to dokprod returned Exception", e)
                    }
                }
                }
            }
        } else {
            val fagmelding = dataBatch.attachments.attachment.first().value
            val joarkRequest = createJoarkRequest(dataBatch, formData, oppfolgingslplanType, edilogg, archiveReference, fagmelding)
            journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

            sendArenaOppfolginsplan(arenaProducer, session, formData, dataBatch, edilogg)
        }
}

fun connectionFactory(fasitProperties: FasitProperties) = MQConnectionFactory().apply {
    hostName = fasitProperties.mqHostname
    port = fasitProperties.mqPort
    queueManager = fasitProperties.mqQueueManagerName
    transportType = WMQConstants.WMQ_CM_CLIENT
    // TODO mq crypo
    // sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = fasitProperties.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun sendArenaOppfolginsplan(
    producer: MessageProducer,
    session: Session,
    formdata: String,
    databatch: DataBatch,
    edilogg: String
) = producer.send(session.createTextMessage().apply {
    val info = createArenaOppfolgingsplan(databatch, formdata, edilogg)
    text = arenaMarshaller.toString(info)
})

fun letterSentNotificationToArena(
    producer: MessageProducer,
    session: Session,
    formdata: String,
    databatch: DataBatch,
    edilogg: String
) = producer.send(session.createTextMessage().apply {
    val info = createArenaBrevTilArbeidsgiver(databatch, formdata, edilogg)
    text = arenabrevMarshaller.toString(info)
})

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

fun findSamhandlerPraksis(samhandlers: List<Samhandler>, orgnrGp: Int): SamhandlerPraksis? = samhandlers
            .filter {
                it.breg_hovedenhet?.organisasjonsnummer == orgnrGp.toString()
            }
            .flatMap {
                it.samh_praksis
            }
            .filter {
                it.samh_praksis_status_kode == "aktiv"
            }
            .filter {
                it.samh_praksis_periode
                        .filter { it.gyldig_fra <= LocalDateTime.now() }
                        .filter { it.gyldig_til == null || it.gyldig_til >= LocalDateTime.now() }
                        .any()
            }
            .firstOrNull {
                it.samh_praksis_status_kode == "LE"
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

private fun kafkaProperties(path: String, fasitProperties: FasitProperties) = Properties().apply {
    load(PaopApplication::class.java.getResourceAsStream(path))
    setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"" + fasitProperties.srvPaopUsername + "\" " +
            "password=\"" + fasitProperties.srvPaopUsername + "\";")
    setProperty("bootstrap.servers", fasitProperties.kafkaBootstrapServersURL)
}