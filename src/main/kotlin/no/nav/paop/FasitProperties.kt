package no.nav.paop

data class FasitProperties(
    val srvPaopUsername: String = getEnvVar("SRVPALE_USERNAME"),
    val srvPaopPassword: String = getEnvVar("SRVPALE_PASSWORD"),
    val appName: String = getEnvVar("APP_NAME"),
    val appVersion: String = getEnvVar("APP_VERSION")
)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")
