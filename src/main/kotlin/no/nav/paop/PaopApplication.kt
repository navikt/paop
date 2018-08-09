package no.nav.paop

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.runBlocking
import no.nav.model.dataBatch.DataBatch
import no.nav.model.navOppfPlan.OppfolgingsplanMetadata
import no.nav.model.oppfolgingsplan2014.Oppfoelgingsplan2M
import no.nav.model.oppfolgingsplan2016.Oppfoelgingsplan4UtfyllendeInfoM
import org.slf4j.LoggerFactory
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller
import javax.xml.transform.stream.StreamSource

val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

private val log = LoggerFactory.getLogger("nav.paop-application")

class PaopApplication

fun main(args: Array<String>) = runBlocking {
    DefaultExports.initialize()
    val fasitProperties = FasitProperties()
    createHttpServer(applicationVersion = fasitProperties.appVersion)

    // TODO read for kafak topic
    // aapen-altinn-oppfolgingsplan-Mottatt
    // all of the diffrent types of oppfolgingsplan comes throw here
    // val dataBatch = extractDataBatchFromString(inputMessageText)
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