package no.nav.paop

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondWrite
import io.ktor.routing.accept
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
private val prometheusContentType = ContentType.parse(TextFormat.CONTENT_TYPE_004)

data class SelftestStatus(val status: String, val applicationVersion: String)

fun createHttpServer(port: Int = 8080, applicationVersion: String): ApplicationEngine = embeddedServer(Netty, port) {
    install(ContentNegotiation) {
        jackson {}
    }
    routing {
        accept(ContentType.Application.Json) {
            get("/is_alive") {
                call.respond(SelftestStatus(status = "I'm alive", applicationVersion = applicationVersion))
            }

            get("/is_ready") {
                call.respond(SelftestStatus(status = "I'm ready", applicationVersion = applicationVersion))
            }
        }

        get("/is_alive") {
            call.respondText("I'm alive.", ContentType.Text.Plain)
        }

        get("/is_ready") {
            call.respondText("I'm ready.", ContentType.Text.Plain)
        }

        get("/prometheus") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
            call.respondWrite(prometheusContentType) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }
    }
}.start()
