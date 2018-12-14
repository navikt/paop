package no.nav.paop.model

import kotlinx.serialization.Serializable

@Serializable
data class OpprettSak(
    val tema: String,
    val applikasjon: String,
    val aktoerId: String,
    val orgnr: String?,
    val fagsakNr: String
)
