package no.nav.paop.model

import kotlinx.serialization.Serializable

@Serializable
data class OpprettSak(
    val tema: String,
    val orgnr: String?,
    val applikasjon: String,
    val opprettetAv: String,
    val fagsakNr: String
)

@Serializable
data class ResponeSak(
    val id: String,
    val tema: String,
    val orgnr: String?,
    val applikasjon: String,
    val opprettetAv: String,
    val fagsakNr: String,
    val aktoerId: String?,
    val opprettetTidspunkt: String?
)