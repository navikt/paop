package no.nav.paop

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.experimental.runBlocking
import no.nav.model.paop.DataBatch
import org.slf4j.LoggerFactory
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller

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
    log.info("Hello there")

    // TODO read for kafak topic
    // aapen-altinn-oppfolgingsplan-Mottatt
    // all of the diffrent types of oppfolgingsplan comes throw here
    //val dataBatch = extractDataBatchFromString(inputMessageText)

}

fun extractDataBatchFromString(dataBatchString: String): DataBatch{
    val dataBatchJaxBContext: JAXBContext = JAXBContext.newInstance(DataBatch::class.java)
    val dataBatchUnmarshaller: Unmarshaller = dataBatchJaxBContext.createUnmarshaller()
    return dataBatchUnmarshaller.unmarshal(StringReader(dataBatchString)) as DataBatch
}
