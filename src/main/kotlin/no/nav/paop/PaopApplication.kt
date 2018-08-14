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
import no.nav.model.navOppfPlan.OppfolgingsplanMetadata
import no.nav.model.oppfolgingsplan2014.Oppfoelgingsplan2M
import no.nav.model.oppfolgingsplan2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.paop.client.PdfClient
import no.nav.paop.client.SarClient
import no.nav.paop.sts.configureSTSFor
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.schemas.reg.flr.IFlrReadOperations
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.jms.Connection
import javax.jms.Queue
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import javax.xml.datatype.DatatypeFactory
import javax.xml.transform.stream.StreamSource

val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

private val log = LoggerFactory.getLogger("nav.paop-application")
val newInstance: DatatypeFactory = DatatypeFactory.newInstance()

class PaopApplication

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()
    createHttpServer(applicationVersion = fasitProperties.appVersion)

    // TODO read for kafak topic
    // aapen-altinn-oppfolgingsplan-Mottatt
    // all of the diffrent types of oppfolgingsplan comes throw here

    connectionFactory(fasitProperties).createConnection(fasitProperties.mqUsername, fasitProperties.mqPassword).use {
        connection ->
        connection.start()

        val session = connection.createSession()
        val arenaQueue = session.createQueue(fasitProperties.arenaIAQueue)
        session.close()

        // FLR
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

        // JOARK
        val journalbehandling = JaxWsProxyFactoryBean().apply {
            address = fasitProperties.journalbehandlingEndpointURL
            features.add(LoggingFeature())
            outInterceptors.add(WSS4JOutInterceptor(interceptorProperties))
            serviceClass = Journalbehandling::class.java
        }.create() as Journalbehandling

        val sarClient = SarClient(fasitProperties.kuhrSarApiURL, fasitProperties.srvPaopUsername,
                fasitProperties.srvPaopPassword)

        listen(PdfClient(fasitProperties.pdfGeneratorURL),
                journalbehandling, sarClient, fastlegeregisteret, arenaQueue, connection)
                .join()
    }
}

fun listen(
    pdfClient: PdfClient,
    journalbehandling: Journalbehandling,
    sarClient: SarClient,
    fastlegeClient: IFlrReadOperations,
    arenaQueue: Queue,
    connection: Connection
) = launch {

    // Then ckeck the input if it is duplicate
    val dataBatch = extractDataBatch("asd")
    val serviceCode = dataBatch.dataUnits.dataUnit.first().formTask.serviceCode
    val serviceEditionCode = dataBatch.dataUnits.dataUnit.first().formTask.serviceEditionCode
    val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
    val oppfolgingslplanType = findOppfolingsplanType(serviceCode, serviceEditionCode)
    val archiveReference = dataBatch.dataUnits.dataUnit.first().archiveReference
    val edilogg = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))}-paop-$archiveReference"

    if (oppfolgingslplanType == Oppfolginsplan.OP2012) {
        val extractOppfolginsplan = extractOppfolginsplan2012(formData)
        // brev to general practitioner
        val letterToGP = extractOppfolginsplan.skjemainnhold.mottaksInformasjon.value.oppfolgingsplanSendesTilFastlege
        if (letterToGP.value == true) // TODO this may not work
        {
            val patientFnr = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fnr
            // TODO do call to FLR and check if the patient has a GP
            // if(patientHasGP)
            // {    call KUHR SAR and get the GP adresse and TSS_ID
            // }
        }
        val letterToNAV = extractOppfolginsplan.skjemainnhold.mottaksInformasjon.value.oppfolgingsplanSendesTiNav
        if (letterToNAV.value == true) // TODO this may not work
        {

            // if (Oppfolginsplan.NAVOPPFL)
            // {
            //       // calls joark and saves the dokument
            // val joarkrequest = createJoarkRequest(dataBatch,formData,Oppfolginsplan.OP2012,  byte64pdf )
            // }
            // Do call to AAREG
            // if ( extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnr is in AAREG && orgstruktur is ok,
            // look at bankkontonummerkanal )
            // {
            // calls joark and saves the dokument
            // val joarkrequest = createJoarkRequest(dataBatch,formData,Oppfolginsplan.OP2012,  byte64pdf )
            // }
            // Send message to ARENA
        } else {
            if (letterToGP.value == true) {
                // if(fastlegefunnet)
                // {
                //        kjør FINN_FASTLEGE_REGEL og send brev ti SYFO Fastlege
                // }
                // else {
                //      kjør følgende regler: OP_KONTROLL_REGEL
                //      og send brev til SYFO arbeisgiver
                // }
            }
        }
    } else if (oppfolgingslplanType == Oppfolginsplan.OP2014) {
        val extractOppfolginsplan = extractOppfolginsplan2014(formData)
        // brev to general practitioner
        val letterToGP = extractOppfolginsplan.skjemainnhold.mottaksInformasjon.value.oppfolgingsplanSendesTilFastlege
    } else if (oppfolgingslplanType == Oppfolginsplan.OP2016) {
        val extractOppfolginsplan = extractOppfolginsplan2016(formData)
        // brev to general practitioner
        val letterToGP = extractOppfolginsplan.skjemainnhold.mottaksInformasjon.value.oppfolgingsplanSendesTilFastlege
    } else if (oppfolgingslplanType == Oppfolginsplan.NAVOPPFPLAN) {
        val extractOppfolginsplan = extractNavOppfPlan(formData)
        // brev to general practitioner
        val letterToGP = extractOppfolginsplan.mottaksinformasjon.isOppfoelgingsplanSendesTilFastlege
    }
}

fun extractDataBatch(dataBatchString: String): DataBatch {
    val dataBatchJaxBContext: JAXBContext = JAXBContext.newInstance(DataBatch::class.java)
    val dataBatchUnmarshaller: Unmarshaller = dataBatchJaxBContext.createUnmarshaller()
    return dataBatchUnmarshaller.unmarshal(StringReader(dataBatchString)) as DataBatch
}

fun extractOppfolginsplan2016(formdataString: String): Oppfoelgingsplan4UtfyllendeInfoM {
    val skjemainnholdJaxBContext: JAXBContext = JAXBContext.newInstance(Oppfoelgingsplan4UtfyllendeInfoM::class.java)
    val skjemainnholdUnmarshaller: Unmarshaller = skjemainnholdJaxBContext.createUnmarshaller()
    return skjemainnholdUnmarshaller.unmarshal(
            StreamSource(StringReader(formdataString)), Oppfoelgingsplan4UtfyllendeInfoM::class.java).value
}

fun extractOppfolginsplan2014(formdataString: String): Oppfoelgingsplan2M {
    val oppfoelgingsplan2MJaxBContext: JAXBContext = JAXBContext.newInstance(Oppfoelgingsplan2M::class.java)
    val oppfoelgingsplan2MUnmarshaller: Unmarshaller = oppfoelgingsplan2MJaxBContext.createUnmarshaller()
    return oppfoelgingsplan2MUnmarshaller.unmarshal(
            StreamSource(StringReader(formdataString)), Oppfoelgingsplan2M::class.java).value
}

fun extractOppfolginsplan2012(formdataString: String): no.nav.model.oppfolgingsplan2012.Oppfoelgingsplan2M {
    val oppfoelgingsplan2MJaxBContext: JAXBContext = JAXBContext.newInstance(no.nav.model.oppfolgingsplan2012.Oppfoelgingsplan2M::class.java)
    val oppfoelgingsplan2MUnmarshaller: Unmarshaller = oppfoelgingsplan2MJaxBContext.createUnmarshaller()
    return oppfoelgingsplan2MUnmarshaller.unmarshal(
            StreamSource(StringReader(formdataString)), no.nav.model.oppfolgingsplan2012.Oppfoelgingsplan2M::class.java).value
}

fun extractNavOppfPlan(formdataString: String): OppfolgingsplanMetadata {
    val oppfolgingsplanMetadataJaxBContext: JAXBContext = JAXBContext.newInstance(OppfolgingsplanMetadata::class.java)
    val oppfolgingsplanMetadataUnmarshaller: Unmarshaller = oppfolgingsplanMetadataJaxBContext.createUnmarshaller()
    return oppfolgingsplanMetadataUnmarshaller.unmarshal(StringReader(formdataString)) as OppfolgingsplanMetadata
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