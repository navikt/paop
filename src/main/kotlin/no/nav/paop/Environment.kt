package no.nav.paop

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

data class Environment(
    val applicationPort: Int = config.getProperty("application.port").toInt(),
    val applicationThreads: Int = config.getProperty("application.threads").toInt(),
    val srvPaopUsername: String = getEnvVar("SRVPAOP_USERNAME"),
    val srvPaopPassword: String = getEnvVar("SRVPAOP_PASSWORD"),
    val journalbehandlingEndpointURL: String = getEnvVar("JOARK_JOURNALBEHANDLING_WS_ENDPOINT_URL"),
    val securityTokenServiceUrl: String = getEnvVar("SECURITYTOKENSERVICE_URL"),
    val appName: String = getEnvVar("APP_NAME"),
    val appVersion: String = getEnvVar("APP_VERSION"),
    val arenaIAQueue: String = getEnvVar("EIA_QUEUE_ARENA_IA_QUEUENAME"),
    val mqHostname: String = getEnvVar("MQGATEWAY03_HOSTNAME"),
    val mqPort: Int = getEnvVar("MQGATEWAY03_PORT").toInt(),
    val mqQueueManagerName: String = getEnvVar("MQGATEWAY03_NAME"),
    val mqChannelName: String = getEnvVar("PAOP_CHANNEL_NAME"),
    val mqUsername: String = getEnvVar("SRVAPPSERVER_USERNAME", "srvappserver"),
    val mqPassword: String = getEnvVar("SRVAPPSERVER_PASSWORD", ""),
    val fastlegeregiserHdirURL: String = getEnvVar("EKSTERN_HELSE_FASTLEGEINFORMASJON_ENDPOINTURL"),
    val pdfGeneratorURL: String = getEnvVar("PDF_GENERATOR_URL", "http://pdf-gen/api"),
    val organisasjonV4EndpointURL: String = getEnvVar("VIRKSOMHET_ORGANISASJON_V4_ENDPOINTURL"),
    val kafkaBootstrapServersURL: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val kafkaTopicOppfolginsplan: String = getEnvVar("PRIVAT_ALTINN_PAOP_TOPIC", "privat-altinn-paop-Mottatt"),
    val dokumentproduksjonV3EndpointURL: String = getEnvVar("DOKUMENTPRODUKSJON_V3_ENDPOINTURL"),
    val partnerEmottakEndpointURL: String = getEnvVar("PARTNERSERVICE_ENDPOINTURL"),
    val adresseregisteretV1EmottakEndpointURL: String = getEnvVar("EKSTERN_HELSE_ADRESSEREGISTERET_V1_ENDPOINTURL"),
    val receiptQueueName: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
    val behandlealtinnmeldingV1EndpointURL: String = getEnvVar("EKSTERN_ALTINN_BEHANDLEALTINNMELDING_V1_ENDPOINTURL"),
    val altinnUserUsername: String = getEnvVar("ALTINNUSER_USERNAME"),
    val altinnUserPassword: String = getEnvVar("ALTINNUSER_PASSWORD")

)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")

private val vaultApplicationPropertiesPath = Paths.get("/var/run/secrets/nais.io/vault/application.properties")

private val config = Properties().apply {
    putAll(Properties().apply {
        load(Environment::class.java.getResourceAsStream("/application.properties"))
    })
    if (Files.exists(vaultApplicationPropertiesPath)) {
        load(Files.newInputStream(vaultApplicationPropertiesPath))
    }
}