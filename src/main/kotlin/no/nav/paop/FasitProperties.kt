package no.nav.paop

data class FasitProperties(
    val srvPaopUsername: String = getEnvVar("SRVPAOP_USERNAME"),
    val srvPaopPassword: String = getEnvVar("SRVPAOP_PASSWORD"),
    val aaregHentOrganisasjonEndpointURL: String = getEnvVar("AAREG_HENT_ORGANISASJON_ENDPOINTURL"),
    val aaregWSUsername: String = getEnvVar("AAREGPOLICYUSER_USERNAME"),
    val aaregWSPassword: String = getEnvVar("AAREGPOLICYUSER_PASSWORD"),
    val journalbehandlingEndpointURL: String = getEnvVar("JOARK_JOURNALBEHANDLING_WS_ENDPOINT_URL"),
    val securityTokenServiceUrl: String = getEnvVar("SECURITYTOKENSERVICE_URL"),
    val appName: String = getEnvVar("APP_NAME"),
    val appVersion: String = getEnvVar("APP_VERSION")
)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")
