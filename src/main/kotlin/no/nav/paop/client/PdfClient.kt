package no.nav.paop.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.config
import io.ktor.client.features.auth.basic.BasicAuth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

@io.ktor.util.KtorExperimentalAPI
class PdfClient(private val endpointUrl: String, private val serviceUsername: String, private val servicePassword: String) {
    private val client = HttpClient(CIO.config {
        maxConnectionsCount = 1000 // Maximum number of socket connections.
        endpoint.apply {
            maxConnectionsPerRoute = 100
            pipelineMaxSize = 20
            keepAliveTime = 5000
            connectTimeout = 5000
            connectRetryAttempts = 5
        }
    }) {
        install(BasicAuth) {
            username = serviceUsername
            password = servicePassword
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
    }

    suspend fun generatePDF(domainObject: Any): ByteArray = client.post {
        contentType(ContentType.Application.Json)
        body = objectMapper.writeValueAsBytes(domainObject)

        url("$endpointUrl/v1/genpdf/paop/oppfoelgingsplan")
    }
}

val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)