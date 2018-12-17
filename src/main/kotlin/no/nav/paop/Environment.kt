package no.nav.paop

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

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

data class Environment(
    val behandleJournalV2EndpointURL: String = getEnvVar("BEHANDLEJOURNAL_V2_ENDPOINTURL"),
    val appName: String = getEnvVar("APP_NAME"),
    val appVersion: String = getEnvVar("APP_VERSION"),
    val mqHostname: String = getEnvVar("MQGATEWAY03_HOSTNAME"),
    val mqPort: Int = getEnvVar("MQGATEWAY03_PORT").toInt(),
    val mqQueueManagerName: String = getEnvVar("MQGATEWAY03_NAME"),
    val mqChannelName: String = getEnvVar("PAOP_CHANNEL_NAME"),
    val fastlegeregiserHdirURL: String = getEnvVar("EKSTERN_HELSE_FASTLEGEINFORMASJON_ENDPOINTURL"),
    val pdfGeneratorURL: String = getEnvVar("PDF_GENERATOR_URL", "http://pdf-gen/api"),
    val sakURL: String = getEnvVar("SAK_URL", "http://sak/api"),
    val organisasjonV4EndpointURL: String = getEnvVar("VIRKSOMHET_ORGANISASJON_V4_ENDPOINTURL"),
    val kafkaBootstrapServersURL: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),
    val dokumentproduksjonV3EndpointURL: String = getEnvVar("DOKUMENTPRODUKSJON_V3_ENDPOINTURL"),
    val partnerEmottakEndpointURL: String = getEnvVar("PARTNERSERVICE_ENDPOINTURL"),
    val adresseregisteretV1EmottakEndpointURL: String = getEnvVar("EKSTERN_HELSE_ADRESSEREGISTERET_V1_ENDPOINTURL"),
    val receiptQueueName: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
    val organisasjonEnhetV2EndpointURL: String = getEnvVar("VIRKSOMHET_ORGANISASJONENHET_V2_ENDPOINTURL"),
    val personV3EndpointURL: String = getEnvVar("VIRKSOMHET_PERSON_V3_ENDPOINTURL"),
    val securityTokenServiceUrl: String = getEnvVar("SECURITYTOKENSERVICE_URL"),

    val applicationPort: Int = config.getProperty("application.port").toInt(),
    val applicationThreads: Int = config.getProperty("application.threads").toInt(),
    val kafkaIncommingTopicOppfolginsplan: String = config.getProperty("kafka.appen.altinn.oppfolgingsplan.topic"),
    val kafkaOutgoingTopicOppfolginsplan: String = config.getProperty("kafka.privat.syfo.oppfolingsplan.topic"),
    val srvPaopUsername: String = config.getProperty("serviceuser.username"),
    val srvPaopPassword: String = config.getProperty("serviceuser.password"),
    val mqUsername: String = config.getProperty("mq.username"),
    val mqPassword: String = config.getProperty("mq.password")

)