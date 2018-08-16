package no.nav.paop.model

import java.time.ZonedDateTime

data class Fagmelding(
    val nokkelopplysninger: Nokkelopplysninger,
    val opplysningerOmArbeidstakeren: OpplysningerOmArbeidstakeren,
    val opplysingerOmSykefravaeret: OpplysingerOmSykefravaeret,
    val tiltak: Tiltak,
    val underskrift: Underskift,
    val utfyllendeInformasjon: UtfyllendeInformasjon

)

data class Nokkelopplysninger(
    val virksomhetensnavn: String,
    val organiasjonsnr: String,
    val nearmestelederFornavnEtternavn: FornavnEtternavn,
    val tlfnearmesteleder: String,
    val annenKontaktPersonFornavnEtternavn: FornavnEtternavn,
    val tlfkontatkperson: String,
    val virksomhetenerIAVirksomhet: Boolean,
    val virksomhetenHarBedrifsHelseTjeneste: Boolean
)

data class OpplysningerOmArbeidstakeren(
    val arbeidstakerenFornavnEtternavn: FornavnEtternavn,
    val fodselsnummer: String,
    val tlf: String,
    val stillingAvdeling: String,
    val ordineareArbeidsoppgaver: String

)

data class OpplysingerOmSykefravaeret(
    val forsteFravearsdag: ZonedDateTime,
    val sykmeldingsDato: ZonedDateTime,
    val sykmeldingsProsentVedSykmeldDato: String
)

data class Tiltak(
    val ordineareArbeidsoppgaverSomKanIkkeKanUtfores: String,
    val beskrivelseAvTiltak: String,
    val maalMedTiltaket: String,
    val tiltaketGjennonforesIPerioden: TiltaketGjennonforesIPerioden,
    val sykmeldingsprosendIPerioden: String,
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
    val raadOgVeiledning: Boolean,
    val dialogmoteMed: Boolean,
    val arbeidsrettedeTiltak: Boolean,
    val hjelpemidler: Boolean
)

data class BehovForBistandFraAndre(
    val bedriftsHelsetjenesten: Boolean,
    val andre: Boolean,
    val andreFritekts: String
)

data class VurderingEffektAvTiltak(
    val vurderingEffektAvTiltakFritekst: String,
    val behovForNyeTiltak: Boolean
)

data class Underskift(
    val datoforUnderskift: ZonedDateTime,
    val arbeidstaker: FornavnEtternavn,
    val arbeidsgiver: FornavnEtternavn,
    val signertPapirkopiForeliggerPaaArbeidsplasssen: Boolean
)

data class FornavnEtternavn(
    val fornavn: String,
    val etternavn: String
)

data class UtfyllendeInformasjon(
    val arbeidstakerMedvirkGjeonnforingOppfolginsplan: Boolean,
    val hvorforHarIkkeArbeidstakerenMedvirket: String?,
    val utfyllendeOpplysinger: String

)
