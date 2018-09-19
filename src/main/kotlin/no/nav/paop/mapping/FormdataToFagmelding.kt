package no.nav.paop.mapping

import no.nav.paop.Oppfolginsplan
import no.nav.paop.extractOppfolginsplan2012
import no.nav.paop.extractOppfolginsplan2014
import no.nav.paop.extractOppfolginsplan2016
import no.nav.paop.model.BehovForBistandFraAndre
import no.nav.paop.model.BehovForBistandFraNav
import no.nav.paop.model.Fagmelding
import no.nav.paop.model.FornavnEtternavn
import no.nav.paop.model.Nokkelopplysninger
import no.nav.paop.model.OpplysingerOmSykefravaeret
import no.nav.paop.model.OpplysningerOmArbeidstakeren
import no.nav.paop.model.Tiltak
import no.nav.paop.model.TiltaketGjennonforesIPerioden
import no.nav.paop.model.Underskift
import no.nav.paop.model.UtfyllendeInformasjon
import no.nav.paop.model.VurderingEffektAvTiltak
import java.time.ZonedDateTime

fun mapFormdataToFagmelding(formData: String, oppfolgingPlanType: Oppfolginsplan): Fagmelding {

    return Fagmelding(
            nokkelopplysninger = Nokkelopplysninger(
                    virksomhetensnavn = extractOrgnavn(formData, oppfolgingPlanType),
                    organiasjonsnr = extractOrgNr(formData, oppfolgingPlanType),
                    nearmestelederFornavnEtternavn = FornavnEtternavn(
                            fornavn = extractNermesteLederFornavn(formData, oppfolgingPlanType),
                            etternavn = extractNermesteLederEtternavn(formData, oppfolgingPlanType)
                            ),
                    tlfnearmesteleder = extractTelefonNaermesteLeder(formData, oppfolgingPlanType),
                    annenKontaktPersonFornavnEtternavn = FornavnEtternavn(
                            fornavn = extractAnnenKontaktpersonFornavn(formData, oppfolgingPlanType),
                            etternavn = extractAnnenKontaktpersonEtternavn(formData, oppfolgingPlanType)),
                    tlfkontatkperson = extractTelefonKontaktperson(formData, oppfolgingPlanType),
                    virksomhetenerIAVirksomhet = extractVirksomhetenerIAVirksomhet(formData, oppfolgingPlanType),
                    virksomhetenHarBedrifsHelseTjeneste = extractVirksomhetenHarBedrifsHelseTjeneste(formData, oppfolgingPlanType)
            ),
            opplysningerOmArbeidstakeren = OpplysningerOmArbeidstakeren(
                    arbeidstakerenFornavnEtternavn = FornavnEtternavn(
                            fornavn = extractSykmeldtArbeidstakerFornavn(formData, oppfolgingPlanType),
                            etternavn = extractSykmeldtArbeidstakerEtternavn(formData, oppfolgingPlanType)
                    ),
                    fodselsnummer = extractSykmeldtArbeidstakerFnr(formData, oppfolgingPlanType),
                    tlf = extractSykmeldtArbeidstakerTlf(formData, oppfolgingPlanType),
                    stillingAvdeling = extractSykmeldtArbeidstakerstillingAvdeling(formData, oppfolgingPlanType),
                    ordineareArbeidsoppgaver = extractSykmeldtArbeidstakerstillingOrdinaereArbeidsoppgaver(formData, oppfolgingPlanType)
            ),
            opplysingerOmSykefravaeret = OpplysingerOmSykefravaeret(
                    forsteFravearsdag = extractSykefravaerForSykmeldtArbeidstakerFoersteFravaersdag(formData, oppfolgingPlanType),
                    sykmeldingsDato = extractSykefravaerForSykmeldtArbeidstakerSykmeldingsDato(formData, oppfolgingPlanType),
                    sykmeldingsProsentVedSykmeldDato = extractSykefravaerForSykmeldtArbeidstakerSykmeldingsprosentVedSykmeldingsDato(formData, oppfolgingPlanType)
            ),
            tiltak = Tiltak(
                    ordineareArbeidsoppgaverSomKanIkkeKanUtfores = extractTiltakOrdinaereArbeidsoppgaverSomIkkeKanUtfoeres(formData, oppfolgingPlanType),
                    beskrivelseAvTiltak = extractTiltakBeskrivelseAvTiltaket(formData, oppfolgingPlanType),
                    maalMedTiltaket = extractTiltakMaalMedTiltaket(formData, oppfolgingPlanType),
                    tiltaketGjennonforesIPerioden = TiltaketGjennonforesIPerioden(
                            fraDato = extractTiltakTiltaketGjennonforesIPeriodenFra(formData, oppfolgingPlanType),
                            tilDato = extractTiltakTiltaketGjennonforesIPeriodenTil(formData, oppfolgingPlanType)
                            ),
                    tilrettelagtArbeidIkkeMulig = extractTiltakTilrettelagtArbeidIkkeMulig(formData, oppfolgingPlanType),
                    sykmeldingsprosendIPerioden = extractTiltakSykmeldingsprosentIPerioden(formData, oppfolgingPlanType),
                    behovForBistandFraNav = BehovForBistandFraNav(
                            raadOgVeiledning = extractTiltakBistandRaadOgVeiledning(formData, oppfolgingPlanType),
                            dialogmoteMed = extractTiltakBistandDialogMoeteMedNav(formData, oppfolgingPlanType),
                            arbeidsrettedeTiltak = extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler(formData, oppfolgingPlanType),
                            hjelpemidler = extractTiltakBistandHjelpemidler(formData, oppfolgingPlanType)
                    ),
                    behovForBistandFraAndre = BehovForBistandFraAndre(
                            bedriftsHelsetjenesten = extractTiltakBistandBedriftshelsetjenesten(formData, oppfolgingPlanType),
                            andre = extractTiltakBistandAndre(formData, oppfolgingPlanType),
                            andreFritekts = extractTiltakBistandAndreBeskrivelse(formData, oppfolgingPlanType)
                    ),
                    behovForAvklaringMedLegeSykmeleder = extractTiltakBehovForAvklaringLegeSykmelder(formData, oppfolgingPlanType),
                    vurderingEffektAvTiltak = VurderingEffektAvTiltak(
                            vurderingEffektAvTiltakFritekst = extractTiltakVurderingAvTiltak(formData, oppfolgingPlanType),
                            behovForNyeTiltak = extractTiltakBehovForNyeTiltak(formData, oppfolgingPlanType)
                    ),
                    fremdrift = extractTiltakOppfoelgingssamtaler(formData, oppfolgingPlanType)

            ),
            underskrift = Underskift(
                    datoforUnderskift = extractUnderskirftDato(formData, oppfolgingPlanType),
                    arbeidstaker = FornavnEtternavn(
                            fornavn = extractSykmeldtArbeidstakerFornavn(formData, oppfolgingPlanType),
                            etternavn = extractSykmeldtArbeidstakerEtternavn(formData, oppfolgingPlanType)
                    ),
                    arbeidsgiver = FornavnEtternavn(
                            fornavn = extractNermesteLederFornavn(formData, oppfolgingPlanType),
                            etternavn = extractNermesteLederEtternavn(formData, oppfolgingPlanType)
                    ),
                    signertPapirkopiForeliggerPaaArbeidsplasssen = extractSignertPapirkopiForeligger(formData, oppfolgingPlanType)

            ),
            utfyllendeInformasjon = if (oppfolgingPlanType == Oppfolginsplan.OP2016) { UtfyllendeInformasjon(
                        arbeidstakerMedvirkGjeonnforingOppfolginsplan = extractArbeidstakerMedvirketGjennomfoering(formData),
                        hvorforHarIkkeArbeidstakerenMedvirket = extractArbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse(formData),
                        utfyllendeOpplysinger = extractUtfyllendeOpplysninger(formData)
            )
            } else {
                null
            }

    )
}

fun extractOrgnavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.arbeidsgiver.value.orgnavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.arbeidsgiver.value.orgnavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.arbeidsgiver.value.orgnavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
    }

fun extractOrgNr(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.arbeidsgiver.value.orgnr
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.arbeidsgiver.value.orgnr
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.arbeidsgiver.value.orgnr
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractNermesteLederFornavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.arbeidsgiver.value.naermesteLederFornavn.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.arbeidsgiver.value.naermesteLederFornavn.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.arbeidsgiver.value.naermesteLederFornavn.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractNermesteLederEtternavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.naermesteLederEtternavn?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.naermesteLederEtternavn?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.naermesteLederEtternavn?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTelefonNaermesteLeder(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.telefonNaermesteLeder?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.telefonNaermesteLeder?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.telefonNaermesteLeder?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractAnnenKontaktpersonFornavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.annenKontaktpersonFornavn?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.annenKontaktpersonFornavn?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.annenKontaktpersonFornavn?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractAnnenKontaktpersonEtternavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.annenKontaktpersonEtternavn?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.annenKontaktpersonEtternavn?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.annenKontaktpersonEtternavn?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTelefonKontaktperson(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.telefonKontaktperson?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.telefonKontaktperson?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.telefonKontaktperson?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractVirksomhetenerIAVirksomhet(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.virksomhetErIABedrift?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.virksomhetErIABedrift?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.virksomhetErIABedrift?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractVirksomhetenHarBedrifsHelseTjeneste(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.value?.virksomhetHarBedriftshelsetjeneste?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.value?.virksomhetHarBedriftshelsetjeneste?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.value?.virksomhetHarBedriftshelsetjeneste?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerFornavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.fornavn?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.fornavn?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.fornavn?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerEtternavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.etternavn?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.etternavn?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.etternavn?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerFnr(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.sykmeldtArbeidstaker.value.fnr
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.sykmeldtArbeidstaker.value.fnr
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.sykmeldtArbeidstaker.value.fnr
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerTlf(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.tlf?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.tlf?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.tlf?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerstillingAvdeling(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.stillingAvdeling?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.stillingAvdeling?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.stillingAvdeling?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerstillingOrdinaereArbeidsoppgaver(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.ordinaereArbeidsoppgaver?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.ordinaereArbeidsoppgaver?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.value?.ordinaereArbeidsoppgaver?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykefravaerForSykmeldtArbeidstakerFoersteFravaersdag(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.foersteFravaersdag?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.foersteFravaersdag?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.foersteFravaersdag?.value?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykefravaerForSykmeldtArbeidstakerSykmeldingsDato(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.sykmeldingsdato?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.sykmeldingsdato?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.sykmeldingsdato?.value?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykefravaerForSykmeldtArbeidstakerSykmeldingsprosentVedSykmeldingsDato(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.sykmeldingsprosentVedSykmeldingsDato?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.sykmeldingsprosentVedSykmeldingsDato?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.value?.sykmeldingsprosentVedSykmeldingsDato?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakOrdinaereArbeidsoppgaverSomIkkeKanUtfoeres(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBeskrivelseAvTiltaket(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.beskrivelseAvTiltaket?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.beskrivelseAvTiltaket?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.beskrivelseAvTiltaket?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakMaalMedTiltaket(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.maalMedTiltaket?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.maalMedTiltaket?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.maalMedTiltaket?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakTiltaketGjennonforesIPeriodenFra(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.value?.periodeFra?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.value?.periodeFra?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.value?.periodeFra?.value?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakTiltaketGjennonforesIPeriodenTil(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.value?.periodeTil?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.value?.periodeTil?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.value?.periodeTil?.value?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakTilrettelagtArbeidIkkeMulig(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tilrettelagtArbeidIkkeMulig?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tilrettelagtArbeidIkkeMulig?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.tilrettelagtArbeidIkkeMulig?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakSykmeldingsprosentIPerioden(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.sykmeldingsprosentIPerioden?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.sykmeldingsprosentIPerioden?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.sykmeldingsprosentIPerioden?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandRaadOgVeiledning(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandRaadOgVeiledning.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandRaadOgVeiledning.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandRaadOgVeiledning.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandDialogMoeteMedNav(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandDialogMoeteMedNav.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandDialogMoeteMedNav.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandDialogMoeteMedNav.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandArbeidsrettedeTiltakOgVirkemidler.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandArbeidsrettedeTiltakOgVirkemidler.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandArbeidsrettedeTiltakOgVirkemidler.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandHjelpemidler(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandHjelpemidler.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandHjelpemidler.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandHjelpemidler.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandBedriftshelsetjenesten(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandBedriftshelsetjenesten.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandBedriftshelsetjenesten.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandBedriftshelsetjenesten.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandAndre(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandAndre.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandAndre.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandAndre.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandAndreBeskrivelse(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.bistandAndreBeskrivelse?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.bistandAndreBeskrivelse?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.bistandAndreBeskrivelse?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBehovForAvklaringLegeSykmelder(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.behovForAvklaringLegeSykmelder?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.behovForAvklaringLegeSykmelder?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.behovForAvklaringLegeSykmelder?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakVurderingAvTiltak(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.vurderingAvTiltak?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.vurderingAvTiltak?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.vurderingAvTiltak?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBehovForNyeTiltak(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.behovForNyeTiltak?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.behovForNyeTiltak?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.behovForNyeTiltak?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakOppfoelgingssamtaler(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.oppfoelgingssamtaler?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.oppfoelgingssamtaler?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.oppfoelgingssamtaler?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractUnderskirftDato(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.underskriftsdato?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.underskriftsdato?.value?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.underskriftsdato?.value?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSignertPapirkopiForeligger(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.signertPapirkopiForeligger?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.signertPapirkopiForeligger?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.value?.tiltaksinformasjon?.firstOrNull()?.signertPapirkopiForeligger?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractArbeidstakerMedvirketGjennomfoering(formData: String): Boolean? =
        extractOppfolginsplan2016(formData).skjemainnhold?.arbeidstakersDeltakelse?.value?.arbeidstakerMedvirketGjennomfoering?.value

fun extractArbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse(formData: String): String? =
        extractOppfolginsplan2016(formData).skjemainnhold?.arbeidstakersDeltakelse?.value?.arbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse?.value

fun extractUtfyllendeOpplysninger(formData: String): String? =
        extractOppfolginsplan2016(formData).skjemainnhold?.utfyllendeOpplysninger?.value
