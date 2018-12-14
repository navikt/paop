package no.nav.paop.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class OpprettSak(
    val tema: String,
    val aktoerId: String,
    val orgnr: String?,
    val applikasjon: String,
    val opprettetAv: String,
    val opprettetTidspunkt: LocalDateTime,
    val fagsakNr: String
)
