package no.nav.paop.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.config
import io.ktor.client.features.auth.basic.BasicAuth
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paop.model.OpprettSak
import no.nav.paop.model.ResponeSak
import java.util.UUID

@io.ktor.util.KtorExperimentalAPI
class SakClient(private val endpointUrl: String, private val serviceUsername: String, private val servicePassword: String) {
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
        install(BasicAuth) {
            username = serviceUsername
            password = servicePassword
        }
    }

suspend fun generateSAK(sak: OpprettSak): ResponeSak = client.post {
    contentType(ContentType.Application.Json)
    headers {
        append("X-Correlation-ID", UUID.randomUUID().toString())
    }
    body = sak
    url("$endpointUrl/v1/saker")
}
}
