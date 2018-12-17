package no.nav.paop.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paop.Environment
import no.nav.paop.log
import java.util.UUID

class SakClient(private val endpointUrl: String, private val stsClient: StsOidcClient) {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }

suspend fun generateSAK(env: Environment, domainObject: Any): String = client.post {
    contentType(ContentType.Application.Json)
    val oidcToken = stsClient.oidcToken()
    headers {
        append("Authorization", "Bearer ${oidcToken.access_token}")
        append("X-Correlation-ID", UUID.randomUUID().toString())
    }
    body = objectMapper.writeValueAsBytes(domainObject)
    log.info("Generate sak request: ${objectMapper.writeValueAsString(domainObject)}")
    url("${env.sakURL}/v1/saker")
}
}
