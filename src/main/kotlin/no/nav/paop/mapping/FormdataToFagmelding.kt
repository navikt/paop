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
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.arbeidsgiver.orgnavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.arbeidsgiver.orgnavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.arbeidsgiver.orgnavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
    }

fun extractOrgNr(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.arbeidsgiver.orgnr
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.arbeidsgiver.orgnr
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.arbeidsgiver.orgnr
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractNermesteLederFornavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.arbeidsgiver.naermesteLederFornavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.arbeidsgiver.naermesteLederFornavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.arbeidsgiver.naermesteLederFornavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractNermesteLederEtternavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.naermesteLederEtternavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.naermesteLederEtternavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.naermesteLederEtternavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTelefonNaermesteLeder(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.telefonNaermesteLeder
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.telefonNaermesteLeder
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.telefonNaermesteLeder
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractAnnenKontaktpersonFornavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.annenKontaktpersonFornavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.annenKontaktpersonFornavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.annenKontaktpersonFornavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractAnnenKontaktpersonEtternavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.annenKontaktpersonEtternavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.annenKontaktpersonEtternavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.annenKontaktpersonEtternavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTelefonKontaktperson(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.telefonKontaktperson
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.telefonKontaktperson
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.telefonKontaktperson
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractVirksomhetenerIAVirksomhet(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.isVirksomhetErIABedrift
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.isVirksomhetErIABedrift
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.isVirksomhetErIABedrift
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractVirksomhetenHarBedrifsHelseTjeneste(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.arbeidsgiver?.isVirksomhetHarBedriftshelsetjeneste
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.arbeidsgiver?.isVirksomhetHarBedriftshelsetjeneste
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.arbeidsgiver?.isVirksomhetHarBedriftshelsetjeneste
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerFornavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.fornavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.fornavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.fornavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerEtternavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.etternavn
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.etternavn
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.etternavn
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerFnr(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.sykmeldtArbeidstaker.fnr
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.sykmeldtArbeidstaker.fnr
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.sykmeldtArbeidstaker.fnr
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerTlf(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.tlf
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.tlf
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.tlf
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerstillingAvdeling(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.stillingAvdeling
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.stillingAvdeling
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.stillingAvdeling
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykmeldtArbeidstakerstillingOrdinaereArbeidsoppgaver(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykmeldtArbeidstaker?.ordinaereArbeidsoppgaver
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykmeldtArbeidstaker?.ordinaereArbeidsoppgaver
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykmeldtArbeidstaker?.ordinaereArbeidsoppgaver
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykefravaerForSykmeldtArbeidstakerFoersteFravaersdag(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.foersteFravaersdag?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.foersteFravaersdag?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.foersteFravaersdag?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykefravaerForSykmeldtArbeidstakerSykmeldingsDato(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsdato?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsdato?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsdato?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSykefravaerForSykmeldtArbeidstakerSykmeldingsprosentVedSykmeldingsDato(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsprosentVedSykmeldingsDato
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsprosentVedSykmeldingsDato
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.sykefravaerForSykmeldtArbeidstaker?.sykmeldingsprosentVedSykmeldingsDato
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakOrdinaereArbeidsoppgaverSomIkkeKanUtfoeres(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBeskrivelseAvTiltaket(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.beskrivelseAvTiltaket
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.beskrivelseAvTiltaket
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.beskrivelseAvTiltaket
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakMaalMedTiltaket(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.maalMedTiltaket
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.maalMedTiltaket
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.maalMedTiltaket
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakTiltaketGjennonforesIPeriodenFra(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.periodeFra?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.periodeFra?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.periodeFra?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakTiltaketGjennonforesIPeriodenTil(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.periodeTil?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.periodeTil?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tidsrom?.periodeTil?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakTilrettelagtArbeidIkkeMulig(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tilrettelagtArbeidIkkeMulig
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tilrettelagtArbeidIkkeMulig
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.tilrettelagtArbeidIkkeMulig
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakSykmeldingsprosentIPerioden(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.sykmeldingsprosentIPerioden
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.sykmeldingsprosentIPerioden
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.sykmeldingsprosentIPerioden
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandRaadOgVeiledning(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandRaadOgVeiledning
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandRaadOgVeiledning
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandRaadOgVeiledning
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandDialogMoeteMedNav(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandDialogMoeteMedNav
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandDialogMoeteMedNav
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandDialogMoeteMedNav
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandArbeidsrettedeTiltakOgVirkemidler
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandArbeidsrettedeTiltakOgVirkemidler
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandArbeidsrettedeTiltakOgVirkemidler
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandHjelpemidler(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandHjelpemidler
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandHjelpemidler
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandHjelpemidler
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandBedriftshelsetjenesten(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandBedriftshelsetjenesten
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandBedriftshelsetjenesten
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandBedriftshelsetjenesten
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandAndre(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandAndre
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandAndre
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.tiltak.tiltaksinformasjon.first().isBistandAndre
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBistandAndreBeskrivelse(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.bistandAndreBeskrivelse
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.bistandAndreBeskrivelse
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.bistandAndreBeskrivelse
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBehovForAvklaringLegeSykmelder(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.behovForAvklaringLegeSykmelder
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.behovForAvklaringLegeSykmelder
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.behovForAvklaringLegeSykmelder
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakVurderingAvTiltak(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.vurderingAvTiltak
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.vurderingAvTiltak
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.vurderingAvTiltak
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakBehovForNyeTiltak(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.isBehovForNyeTiltak
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.isBehovForNyeTiltak
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.isBehovForNyeTiltak
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractTiltakOppfoelgingssamtaler(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.oppfoelgingssamtaler
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.oppfoelgingssamtaler
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.oppfoelgingssamtaler
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractUnderskirftDato(formData: String, oppfolgingPlanType: Oppfolginsplan): ZonedDateTime? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.underskriftsdato?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.underskriftsdato?.toGregorianCalendar()?.toZonedDateTime()
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.underskriftsdato?.toGregorianCalendar()?.toZonedDateTime()
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractSignertPapirkopiForeligger(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.isSignertPapirkopiForeligger
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.isSignertPapirkopiForeligger
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.tiltak?.tiltaksinformasjon?.firstOrNull()?.isSignertPapirkopiForeligger
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractArbeidstakerMedvirketGjennomfoering(formData: String): Boolean? =
        extractOppfolginsplan2016(formData).skjemainnhold?.arbeidstakersDeltakelse?.isArbeidstakerMedvirketGjennomfoering

fun extractArbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse(formData: String): String? =
        extractOppfolginsplan2016(formData).skjemainnhold?.arbeidstakersDeltakelse?.arbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse

fun extractUtfyllendeOpplysninger(formData: String): String? =
        extractOppfolginsplan2016(formData).skjemainnhold?.utfyllendeOpplysninger
