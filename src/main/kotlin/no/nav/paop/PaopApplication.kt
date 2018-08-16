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
import no.nav.paop.client.PdfClient
import no.nav.paop.client.PdfType
import no.nav.paop.client.SarClient
import no.nav.paop.client.createJoarkRequest
import no.nav.paop.mapping.mapFormdataToFagmelding
import no.nav.paop.sts.configureSTSFor
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.schemas.reg.flr.IFlrReadOperations
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.jms.Connection
import javax.jms.Queue
import javax.security.auth.callback.CallbackHandler

private val log = LoggerFactory.getLogger("nav.paop-application")
val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()
    createHttpServer(applicationVersion = fasitProperties.appVersion)

    // TODO read from kafka topic
    // aapen-altinn-oppfolgingsplan-Mottatt, maybe change routing in altinnkanal-2 for 3 different topics?
    // all of the different types of oppfolgingsplan comes throguh here

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
        configureSTSFor(fastlegeregisteret, fasitProperties.srvPaopUsername,
                fasitProperties.srvPaopPassword, fasitProperties.securityTokenServiceUrl)

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

        val sarClient = SarClient(fasitProperties.kuhrSarApiURL, fasitProperties.srvPaopUsername,
                fasitProperties.srvPaopPassword)

        listen(PdfClient(fasitProperties.pdfGeneratorURL),
                journalbehandling, fastlegeregisteret, organisasjonV4, sarClient, arenaQueue, connection)
                .join()
    }
}

fun listen(
    pdfClient: PdfClient,
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
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

    if (oppfolgingslplanType == Oppfolginsplan.OP2012) {

        val organisasjonRequest = ValiderOrganisasjonRequest().apply {
            orgnummer = dataBatch.dataUnits.dataUnit.first().reportee
        }

        val validorgStruktur = organisasjonV4.validerOrganisasjon(organisasjonRequest)
        if (validorgStruktur.isGyldigOrgnummer) {
            val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFormdataToFagmelding(formData, Oppfolginsplan.OP2012))
            val joarkRequest = createJoarkRequest(dataBatch, formData, Oppfolginsplan.OP2012, edilogg, archiveReference, fagmelding)
            journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

            // Send message to ARENA

        val extractOppfolginsplan = extractOppfolginsplan2012(formData)
        val letterToGP = extractOppfolginsplan.skjemainnhold.mottaksInformasjon.value.oppfolgingsplanSendesTilFastlege
        val letterToNAV = extractOppfolginsplan.skjemainnhold.mottaksInformasjon.value.oppfolgingsplanSendesTiNav

        if (letterToNAV.value) {
        }
        if (letterToGP.value) {
                val patientFnr = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fnr
                try {

                    val patientToGPContractAssociation = fastlegeregisteret.getPatientGPDetails(patientFnr)
                } catch (e: Exception) {
                    log.error("Call to flr failed", e)
                }

                val fastlegefunnet = true
                if (fastlegefunnet) {
                    // CALL KUHR SAR
                    // And get the TSSID
                    // and the adress for the samhandler
                }
                // dersom validerInnserderIASKJEMA feiler, sendt til backoutkø
                //      kjør FINN_FASTLEGE_REGEL(ligger her: nav-eia-applikasjon\nav-eia-prosessmotor\eia-message-services\src\main\java\no\nav\eia\kontroll\rules\FinnFastlegeRegel.java)
                //      Setter TSS ID, dersom den er god nokk
                //      og kjør så regele OP_KONTROLL_REGEL
                //      og send brev ti SYFO Fastlege(SYFO_BREV_OP_FASTLEGE)
            } else {
                //      og kjør så regele OP_KONTROLL_REGEL
                //      og send brev ti SYFO Fastlege(SYFO_BREV_OP_ARBEIDSGIVER)
                //      Vent på svar send så melding til ARENA(ARENA_IA_MELDING)
            }
        } else {
                // send to backout que
        }
    } else if (oppfolgingslplanType == Oppfolginsplan.OP2014) {
        val extractOppfolginsplan = extractOppfolginsplan2014(formData)
    } else if (oppfolgingslplanType == Oppfolginsplan.OP2016) {
        val extractOppfolginsplan = extractOppfolginsplan2016(formData)
    } else if (oppfolgingslplanType == Oppfolginsplan.NAVOPPFPLAN) {
        val extractOppfolginsplan = extractNavOppfPlan(formData)
        dataBatch.attachments.attachment.first().value // the pdf to store in joark
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
