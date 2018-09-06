package no.nav.paop.client

import no.nav.model.arenaBrevTilArbeidsgiver.ArenaBrevTilArbeidsgiver
import no.nav.model.arenaOppfolging.ArbeidsgiverType
import no.nav.model.arenaOppfolging.ArenaOppfolgingPlan
import no.nav.model.arenaOppfolging.DokumentInfoType
import no.nav.model.arenaOppfolging.EiaDokumentInfoType
import no.nav.model.dataBatch.DataBatch
import no.nav.paop.Oppfolginsplan
import no.nav.paop.PaopConstant
import no.nav.paop.extractOppfolginsplan2012
import no.nav.paop.extractOppfolginsplan2014
import no.nav.paop.extractOppfolginsplan2016
import no.nav.paop.mapping.extractOrgNr
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFnr
import no.nav.paop.mapping.extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler
import no.nav.paop.mapping.extractTiltakBistandDialogMoeteMedNav
import no.nav.paop.mapping.extractTiltakBistandHjelpemidler
import no.nav.paop.mapping.extractTiltakBistandRaadOgVeiledning
import no.nav.paop.newInstance
import java.util.GregorianCalendar

fun createArenaOppfolgingsplan(dataBatch: DataBatch, formData: String, edilogg: String, oppfolginsplanType: Oppfolginsplan):
        ArenaOppfolgingPlan = ArenaOppfolgingPlan().apply {
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
                        arbeidsgiverOrgNr = extractOrgNr(formData, oppfolginsplanType)
                    }
                }
                avsenderSystem = EiaDokumentInfoType.AvsenderSystem().apply {
                    systemNavn = extractAvsenderSystemSystemnavn(formData, oppfolginsplanType)
                    systemVersjon = extractAvsenderSystemSystemVersjon(formData, oppfolginsplanType)
                }
            }
            bedriftsNr = extractOrgNr(formData, oppfolginsplanType)
            fodselsNr = extractSykmeldtArbeidstakerFnr(formData, oppfolginsplanType)
            bistandNav = ArenaOppfolgingPlan.BistandNav().apply {
                isBistandNavHjelpemid = extractTiltakBistandHjelpemidler(formData, oppfolginsplanType)
                isBistandNavVeil = extractTiltakBistandRaadOgVeiledning(formData, oppfolginsplanType)
                isBistandNavDialogmote = extractTiltakBistandDialogMoeteMedNav(formData, oppfolginsplanType)
                isBistandNavVirke = extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler(formData, oppfolginsplanType)

            }
}

fun createArenaBrevTilArbeidsgiver(dataBatch: DataBatch, formData: String, edilogg: String,oppfolginsplanType: Oppfolginsplan ):
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
                arbeidsgiverOrgNr = extractOrgNr(formData, oppfolginsplanType)
            }
        }
    }
    bedriftsNr = extractOrgNr(formData, oppfolginsplanType)
    fodselsNr = extractSykmeldtArbeidstakerFnr(formData, oppfolginsplanType)
}

fun extractAvsenderSystemSystemnavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.avsenderSystem?.value?.systemNavn?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.avsenderSystem?.value?.systemNavn?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.avsenderSystem?.value?.systemNavn?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractAvsenderSystemSystemVersjon(formData: String, oppfolgingPlanType: Oppfolginsplan): String? =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.avsenderSystem?.value?.systemVersjon?.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.avsenderSystem?.value?.systemVersjon?.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.avsenderSystem?.value?.systemVersjon?.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }