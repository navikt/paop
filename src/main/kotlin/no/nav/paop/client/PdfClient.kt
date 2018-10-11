package no.nav.paop.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.logstash.logback.argument.StructuredArguments
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.IOException

val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

class PdfClient(private val baseUrl: String) {
    private val client: OkHttpClient = OkHttpClient()
    private val log = LoggerFactory.getLogger("nav.pdfClient")

    fun generatePDF(pdfType: PdfType, domainObject: Any): ByteArray {
        val request = Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsBytes(domainObject)))
                .url("$baseUrl/v1/genpdf/pale/${pdfType.pdfGenName()}")
                .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val bytes = response.body()?.bytes()
            if (bytes != null) {
                return bytes
            }
            throw IOException("Received no body from the PDF generator")
        } else {
            log.error("Received an error while contacting the PDF generator {}", StructuredArguments.keyValue("errorBody", response.body()?.string()))
            throw IOException("Unable to contact the PDF generator, got status code ${response.code()}")
        }
    }
}

enum class PdfType {
    FAGMELDING,
    BEHANDLINGSVEDLEGG
}

fun PdfType.pdfGenName(): String =
        name.toLowerCase()
