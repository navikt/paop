package no.nav.paop.mapping

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

fun mapFormdataToFagmelding(formData: String): Fagmelding {
    val extractOppfolginsplan = extractOppfolginsplan2016(formData)

    return Fagmelding(
            nokkelopplysninger = Nokkelopplysninger(
                    virksomhetensnavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnavn,
                    organiasjonsnr = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnr,
                    nearmestelederFornavnEtternavn = FornavnEtternavn(
                            fornavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.naermesteLederFornavn.value,
                            etternavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.naermesteLederEtternavn.value
                            ),
                    tlfnearmesteleder = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.telefonNaermesteLeder.value,
                    annenKontaktPersonFornavnEtternavn = FornavnEtternavn(
                            fornavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.annenKontaktpersonFornavn.value,
                            etternavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.annenKontaktpersonEtternavn.value),
                    tlfkontatkperson = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.telefonKontaktperson.value,
                    virksomhetenerIAVirksomhet = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.virksomhetErIABedrift.value,
                    virksomhetenHarBedrifsHelseTjeneste = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.virksomhetHarBedriftshelsetjeneste.value
            ),
            opplysningerOmArbeidstakeren = OpplysningerOmArbeidstakeren(
                    arbeidstakerenFornavnEtternavn = FornavnEtternavn(
                            fornavn = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fornavn.value,
                            etternavn = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.etternavn.value
                    ),
                    fodselsnummer = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fnr,
                    tlf = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.tlf.value,
                    stillingAvdeling = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.stillingAvdeling.value,
                    ordineareArbeidsoppgaver = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.ordinaereArbeidsoppgaver.value
            ),
            opplysingerOmSykefravaeret = OpplysingerOmSykefravaeret(
                    forsteFravearsdag = extractOppfolginsplan.skjemainnhold.sykefravaerForSykmeldtArbeidstaker.value.foersteFravaersdag.value.toGregorianCalendar().toZonedDateTime(),
                    sykmeldingsDato = extractOppfolginsplan.skjemainnhold.sykefravaerForSykmeldtArbeidstaker.value.sykmeldingsdato.value.toGregorianCalendar().toZonedDateTime(),
                    sykmeldingsProsentVedSykmeldDato = extractOppfolginsplan.skjemainnhold.sykefravaerForSykmeldtArbeidstaker.value.sykmeldingsprosentVedSykmeldingsDato.value
            ),
            tiltak = Tiltak(
                    ordineareArbeidsoppgaverSomKanIkkeKanUtfores = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().ordinaereArbeidsoppgaverSomIkkeKanUtfoeres.value,
                    beskrivelseAvTiltak = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().beskrivelseAvTiltaket.value,
                    maalMedTiltaket = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().maalMedTiltaket.value,
                    tiltaketGjennonforesIPerioden = TiltaketGjennonforesIPerioden(
                            fraDato = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().tidsrom.value.periodeFra.value.toGregorianCalendar().toZonedDateTime(),
                            tilDato = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().tidsrom.value.periodeTil.value.toGregorianCalendar().toZonedDateTime()
                            ),
                    tilrettelagtArbeidIkkeMulig = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().tilrettelagtArbeidIkkeMulig.value,
                    sykmeldingsprosendIPerioden = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().sykmeldingsprosentIPerioden.value,
                    behovForBistandFraNav = BehovForBistandFraNav(
                            raadOgVeiledning = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandRaadOgVeiledning.value,
                            dialogmoteMed = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandDialogMoeteMedNav.value,
                            arbeidsrettedeTiltak = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandArbeidsrettedeTiltakOgVirkemidler.value,
                            hjelpemidler = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandHjelpemidler.value
                    ),
                    behovForBistandFraAndre = BehovForBistandFraAndre(
                            bedriftsHelsetjenesten = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandBedriftshelsetjenesten.value,
                            andre = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandAndre.value,
                            andreFritekts = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().bistandAndreBeskrivelse.value
                    ),
                    behovForAvklaringMedLegeSykmeleder = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().behovForAvklaringLegeSykmelder.value,
                    vurderingEffektAvTiltak = VurderingEffektAvTiltak(
                            vurderingEffektAvTiltakFritekst = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().vurderingAvTiltak.value,
                            behovForNyeTiltak = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().behovForNyeTiltak.value
                    ),
                    fremdrift = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().oppfoelgingssamtaler.value

            ),
            underskrift = Underskift(
                    datoforUnderskift = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().underskriftsdato.value.toGregorianCalendar().toZonedDateTime(),
                    arbeidstaker = FornavnEtternavn(
                            fornavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.naermesteLederFornavn.value,
                            etternavn = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.naermesteLederEtternavn.value
                    ),
                    arbeidsgiver = FornavnEtternavn(
                            fornavn = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fornavn.value,
                            etternavn = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.etternavn.value
                    ),
                    signertPapirkopiForeliggerPaaArbeidsplasssen = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first().signertPapirkopiForeligger.value

            ),
            utfyllendeInformasjon = UtfyllendeInformasjon(
                    arbeidstakerMedvirkGjeonnforingOppfolginsplan = extractOppfolginsplan.skjemainnhold.arbeidstakersDeltakelse.value.arbeidstakerMedvirketGjennomfoering.value,
                    hvorforHarIkkeArbeidstakerenMedvirket = extractOppfolginsplan.skjemainnhold.arbeidstakersDeltakelse.value.arbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse.value,
                    utfyllendeOpplysinger = extractOppfolginsplan.skjemainnhold.utfyllendeOpplysninger.value
            )

    )
}
