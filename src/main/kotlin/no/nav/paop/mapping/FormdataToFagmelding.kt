package no.nav.paop.mapping

import no.nav.model.oppfolgingsplan2016.Skjemainnhold
import no.nav.paop.model.ArbeidstakersDeltakelse
import no.nav.paop.model.BehovForBistandFraAndre
import no.nav.paop.model.BehovForBistandFraNav
import no.nav.paop.model.Fagmelding
import no.nav.paop.model.FornavnEtternavn
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.model.Nokkelopplysninger
import no.nav.paop.model.OpplysingerOmSykefravaeret
import no.nav.paop.model.OpplysningerOmArbeidstakeren
import no.nav.paop.model.Tiltak
import no.nav.paop.model.TiltaketGjennonforesIPerioden
import no.nav.paop.model.Underskift
import no.nav.paop.model.VurderingEffektAvTiltak
import java.time.ZonedDateTime
import javax.xml.datatype.XMLGregorianCalendar

fun mapFormdataToFagmelding(skjemainnhold: Skjemainnhold, incomingMetadata: IncomingMetadata): Fagmelding = Fagmelding(
        nokkelopplysninger = Nokkelopplysninger(
                virksomhetensnavn = incomingMetadata.senderOrgName,
                organiasjonsnr = incomingMetadata.senderOrgId,
                nearmestelederFornavnEtternavn = FornavnEtternavn(
                        fornavn = skjemainnhold.arbeidsgiver.naermesteLederFornavn,
                        etternavn = skjemainnhold.arbeidsgiver.naermesteLederEtternavn
                ),
                tlfnearmesteleder = skjemainnhold.arbeidsgiver.telefonNaermesteLeder,
                annenKontaktPersonFornavnEtternavn = FornavnEtternavn(
                        fornavn = skjemainnhold.arbeidsgiver.annenKontaktpersonFornavn,
                        etternavn = skjemainnhold.arbeidsgiver.annenKontaktpersonEtternavn
                ),
                tlfkontatkperson = skjemainnhold.arbeidsgiver.telefonKontaktperson,
                virksomhetenerIAVirksomhet = skjemainnhold.arbeidsgiver.isVirksomhetErIABedrift,
                virksomhetenHarBedrifsHelseTjeneste = skjemainnhold.arbeidsgiver.isVirksomhetHarBedriftshelsetjeneste
        ),
        opplysningerOmArbeidstakeren = OpplysningerOmArbeidstakeren(
                arbeidstakerenFornavnEtternavn = FornavnEtternavn(
                        fornavn = skjemainnhold.sykmeldtArbeidstaker.fornavn,
                        etternavn = skjemainnhold.sykmeldtArbeidstaker.etternavn
                ),
                fodselsnummer = skjemainnhold.sykmeldtArbeidstaker.fnr,
                tlf = skjemainnhold.sykmeldtArbeidstaker.tlf,
                stillingAvdeling = skjemainnhold.sykmeldtArbeidstaker.stillingAvdeling,
                ordineareArbeidsoppgaver = skjemainnhold.sykmeldtArbeidstaker.ordinaereArbeidsoppgaver
        ),
        opplysingerOmSykefravaeret = OpplysingerOmSykefravaeret(
                forsteFravearsdag = skjemainnhold.sykefravaerForSykmeldtArbeidstaker.foersteFravaersdag?.toZonedDateTime(),
                sykmeldingsDato = skjemainnhold.sykefravaerForSykmeldtArbeidstaker.sykmeldingsdato?.toZonedDateTime(),
                sykmeldingsProsentVedSykmeldDato = skjemainnhold.sykefravaerForSykmeldtArbeidstaker.sykmeldingsprosentVedSykmeldingsDato
        ),
        tiltak = skjemainnhold.tiltak.tiltaksinformasjon.map {
            Tiltak(
                    ordineareArbeidsoppgaverSomKanIkkeKanUtfores = it.ordinaereArbeidsoppgaverSomIkkeKanUtfoeres,
                    beskrivelseAvTiltak = it.beskrivelseAvTiltaket,
                    maalMedTiltaket = it.maalMedTiltaket,
                    tiltaketGjennonforesIPerioden = TiltaketGjennonforesIPerioden(
                            fraDato = it?.tidsrom?.periodeFra?.toZonedDateTime(),
                            tilDato = it?.tidsrom?.periodeTil?.toZonedDateTime()
                    ),
                    tilrettelagtArbeidIkkeMulig = it.tilrettelagtArbeidIkkeMulig,
                    sykmeldingsprosendIPerioden = it.sykmeldingsprosentIPerioden,
                    behovForBistandFraNav = BehovForBistandFraNav(
                            raadOgVeiledning = it.isBistandRaadOgVeiledning,
                            dialogmoteMed = it.isBistandDialogMoeteMedNav,
                            arbeidsrettedeTiltak = it.isBistandArbeidsrettedeTiltakOgVirkemidler,
                            hjelpemidler = it.isBistandHjelpemidler
                    ),
                    behovForBistandFraAndre = BehovForBistandFraAndre(
                            bedriftsHelsetjenesten = it.isBistandBedriftshelsetjenesten,
                            andre = it.isBistandAndre,
                            andreFritekts = it.bistandAndreBeskrivelse
                    ),
                    behovForAvklaringMedLegeSykmeleder = it.behovForAvklaringLegeSykmelder,
                    vurderingEffektAvTiltak = VurderingEffektAvTiltak(
                            behovForNyeTiltak = it.isBehovForNyeTiltak,
                            vurderingEffektAvTiltakFritekst = it.vurderingAvTiltak
                    ),
                    fremdrift = it.oppfoelgingssamtaler,
                    underskrift = Underskift(
                            datoforUnderskift = it.underskriftsdato?.toZonedDateTime(),
                            signertPapirkopiForeliggerPaaArbeidsplasssen = it.isSignertPapirkopiForeligger
                    )
            )
        },
        arbeidstakersDeltakelse = skjemainnhold.arbeidstakersDeltakelse?.let {
            ArbeidstakersDeltakelse(
                    arbeidstakerMedvirkGjeonnforingOppfolginsplan = it.isArbeidstakerMedvirketGjennomfoering,
                    hvorforHarIkkeArbeidstakerenMedvirket = it.arbeidstakerIkkeMedvirketGjennomfoeringBegrunnelse
            )
        },
        utfyllendeInfo = skjemainnhold.utfyllendeOpplysninger
)

fun XMLGregorianCalendar.toZonedDateTime(): ZonedDateTime = toGregorianCalendar().toZonedDateTime()
