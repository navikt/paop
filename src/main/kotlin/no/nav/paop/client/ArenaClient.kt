package no.nav.paop.client

import no.nav.model.arenaBrevTilArbeidsgiver.ArenaBrevTilArbeidsgiver
import no.nav.model.arenaOppfolging.ArbeidsgiverType
import no.nav.model.arenaOppfolging.ArenaOppfolgingPlan
import no.nav.model.arenaOppfolging.DokumentInfoType
import no.nav.model.arenaOppfolging.EiaDokumentInfoType
import no.nav.model.dataBatch.DataBatch
import no.nav.paop.PaopConstant
import no.nav.paop.extractOppfolginsplan2012
import no.nav.paop.newInstance
import java.util.GregorianCalendar

fun createArenaOppfolgingsplan(dataBatch: DataBatch, formData: String, edilogg: String):
        ArenaOppfolgingPlan = ArenaOppfolgingPlan().apply {
            val extractOppfolginsplan = extractOppfolginsplan2012(formData)
            eiaDokumentInfo = EiaDokumentInfoType().apply {
                dokumentInfo = DokumentInfoType().apply {
                    dokumentType = PaopConstant.dokumentType2913.string
                    dokumentreferanse = dataBatch.dataUnits.dataUnit.first().archiveReference
                    ediLoggId = edilogg
                    dokumentDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
                }
                behandlingInfo = null
                avsender = EiaDokumentInfoType.Avsender().apply {
                    arbeidsgiver = ArbeidsgiverType().apply {
                        arbeidsgiverOrgNr = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnr
                    }
                }
                avsenderSystem = EiaDokumentInfoType.AvsenderSystem().apply {
                    systemNavn = extractOppfolginsplan.skjemainnhold.avsenderSystem.value.systemNavn
                    systemVersjon = extractOppfolginsplan.skjemainnhold.avsenderSystem.value.systemVersjon
                }
            }
            bedriftsNr = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnr
            fodselsNr = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fnr
            bistandNav = ArenaOppfolgingPlan.BistandNav().apply {
                isBistandNavHjelpemid = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first()
                        .bistandHjelpemidler.value
                isBistandNavVeil = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first()
                        .bistandRaadOgVeiledning.value
                isBistandNavDialogmote = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first()
                        .bistandDialogMoeteMedNav.value
                isBistandNavVirke = extractOppfolginsplan.skjemainnhold.tiltak.value.tiltaksinformasjon.first()
                        .bistandArbeidsrettedeTiltakOgVirkemidler.value
            }
}

fun createArenaBrevTilArbeidsgiver(dataBatch: DataBatch, formData: String, edilogg: String):
        ArenaBrevTilArbeidsgiver = ArenaBrevTilArbeidsgiver().apply {
    val extractOppfolginsplan = extractOppfolginsplan2012(formData)
    eiaDokumentInfo = no.nav.model.arenaBrevTilArbeidsgiver.EiaDokumentInfoType().apply {
        dokumentInfo = no.nav.model.arenaBrevTilArbeidsgiver.DokumentInfoType().apply {
            dokumentType = "EIA.OFP_AG"
            dokumentTypeVersjon = "1.0"
            dokumentreferanse = dataBatch.dataUnits.dataUnit.first().archiveReference
            ediLoggId = edilogg
        }
        avsender = no.nav.model.arenaBrevTilArbeidsgiver.EiaDokumentInfoType.Avsender().apply {
            arbeidsgiver = no.nav.model.arenaBrevTilArbeidsgiver.ArbeidsgiverType().apply {
                arbeidsgiverOrgNr = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnr
            }
        }
    }
    bedriftsNr = extractOppfolginsplan.skjemainnhold.arbeidsgiver.value.orgnr
    fodselsNr = extractOppfolginsplan.skjemainnhold.sykmeldtArbeidstaker.value.fnr
}