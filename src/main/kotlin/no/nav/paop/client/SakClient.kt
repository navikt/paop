package no.nav.paop.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.config
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
import no.nav.paop.model.OpprettSak
import java.util.UUID

class SakClient(private val endpointUrl: String) {
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
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

@UseExperimental(InternalAPI::class)
suspend fun generateSAK(sak: OpprettSak, serviceUsername: String, servicePassword: String): String = client.post {
    contentType(ContentType.Application.Json)
    val authString = "$serviceUsername:$servicePassword"
    val authBuf = encodeBase64(authString.toByteArray(Charsets.ISO_8859_1))
    headers {
        append("Authorization", "Basic $authBuf")
        append("X-Correlation-ID", UUID.randomUUID().toString())
    }
    body = sak
    url("$endpointUrl/v1/saker")
}
}
