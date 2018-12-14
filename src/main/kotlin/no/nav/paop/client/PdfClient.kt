package no.nav.paop.client

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paop.Environment

suspend fun HttpClient.generatePDF(env: Environment, domainObject: Any): ByteArray = post {
    contentType(ContentType.Application.Json)
    body = objectMapper.writeValueAsBytes(domainObject)

    url("${env.pdfGeneratorURL}/v1/genpdf/paop/oppfoelgingsplan")
}
