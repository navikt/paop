package no.nav.paop.model

import no.nav.model.oppfolgingsplan2016.Skjemainnhold

data class ReceivedOppfolginsplan(
    val oppfolginsplan: Skjemainnhold,
    val pdf: ByteArray,
    val userPersonNumber: String,
    val senderOrgId: String,
    val navLogId: String
)
