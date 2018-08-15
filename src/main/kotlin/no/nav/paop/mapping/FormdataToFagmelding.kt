package no.nav.paop.mapping


import no.nav.paop.extractOppfolginsplan2012
import no.nav.paop.model.Fagmelding


fun mapFormdataToFagmelding(formData: String): Fagmelding {
    val extractOppfolginsplan = extractOppfolginsplan2012(formData)

    return Fagmelding(
            virksomhetensnavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnavn
    )
}


