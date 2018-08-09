package no.nav.paop

data class FasitProperties(
    val srvPaopUsername: String = getEnvVar("SRVPAOP_USERNAME"),
    val srvPaopPassword: String = getEnvVar("SRVPAOP_PASSWORD"),
    val appName: String = getEnvVar("APP_NAME"),
    val appVersion: String = getEnvVar("APP_VERSION")
)

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")
