package no.nav.paop.model

import java.time.ZonedDateTime


data class Fagmelding(
        val nokkelopplysninger : Nokkelopplysninger,
        val opplysningerOmArbeidstakeren: OpplysningerOmArbeidstakeren,
        val opplysingerOmSykefravaeret: OpplysingerOmSykefravaeret,
        val tiltak: Tiltak,
        val underskrift: Underskift

)

data class Nokkelopplysninger(
        val virksomhetensnavn: String,
        val organiasjonsnr: String,
        val nearmestelederFornavn: String,
        val nearmestelederEtternavn: String,
        val tlfnearmesteleder: String,
        val annenKontaktPersonFornavn: String?,
        val annenKontaktPersonEtternavn: String?,
        val tlfkontatkperson: String,
        val virksomhetenerIAVirksomhet: Boolean,
        val virksomhetenHarBedrifsHelseTjeneste: Boolean
)

data class OpplysningerOmArbeidstakeren(
        val arbeidstakerenFornavn: String,
        val arbeidstakerenEtternavn: String,
        val fodselsnummer: String,
        val tlf: String,
        val stillingAvdeling: String,
        val ordineareArbeidsoppgaver: String

)

data class OpplysingerOmSykefravaeret(
        val forsteFravearsdag: ZonedDateTime,
        val sykmeldingsDato: ZonedDateTime,
        val sykmeldingsProsentVedSykmeldDato: Int
)

data class Tiltak(
       val ordineareArbeidsoppgaverSomKanIkkeKanUtfores: String,
       val beskrivelseAvTiltak: String,
       val maalMedTiltaket: String,
       val tiltaketGjennonforesIPerioden: TiltaketGjennonforesIPerioden,
       val sykmeldingsprosendIPerioden: Int,
       val behovForBistandFraNav: BehovForBistandFraNav,
       val behovForBistandFraAndre: BehovForBistandFraAndre,
       val behovForAvklaringMedLegeSykmeleder: String,
       val tilrettelagtArbeidIkkeMulig: String,
       val vurderingEffektAvTiltak: VurderingEffektAvTiltak,
       val fremdrift: String
)

data class TiltaketGjennonforesIPerioden(
        val fraDato: ZonedDateTime,
        val tilDato: ZonedDateTime
)

data class BehovForBistandFraNav(
        val raadOgVeiledning: String,
        val dialogmoteMed: String,
        val arbeidsrettedeTiltak: String,
        val Hjelpemidler: String
)

data class BehovForBistandFraAndre(
        val bedriftsHelsetjenesten: String,
        val andre: Boolean,
        val andreFritekts: String
)

data class VurderingEffektAvTiltak(
        val vurderingEffektAvTiltakFritekst: String,
        val behovForNyeTiltak: Boolean
)

data class Underskift(
        val datoforUnderskift: ZonedDateTime,
        val arbeidstaker: String,
        val arbeidsgiver: String,
        val signertPapirkopiForeliggerPaaArbeidsplasssen: Boolean
)