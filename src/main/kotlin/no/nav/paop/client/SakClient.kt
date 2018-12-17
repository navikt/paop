package no.nav.paop.client

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paop.Environment
import no.nav.paop.log

suspend fun HttpClient.generateSAK(env: Environment, domainObject: Any): String = post {
    contentType(ContentType.Application.Json)
    body = objectMapper.writeValueAsBytes(domainObject)
    log.info("Generate sak request:", objectMapper.writeValueAsString(domainObject))

    url("${env.sakURL}/v1/saker")
}
