package no.nav.paop.client

import no.nav.model.arenaBrevTilArbeidsgiver.ArenaBrevTilArbeidsgiver
import no.nav.model.arenaOppfolging.ArbeidsgiverType
import no.nav.model.arenaOppfolging.ArenaOppfolgingPlan
import no.nav.model.arenaOppfolging.DokumentInfoType
import no.nav.model.arenaOppfolging.EiaDokumentInfoType
import no.nav.paop.ArenaBistand
import no.nav.paop.IncomingMetadata
import no.nav.paop.Oppfolginsplan
import no.nav.paop.PaopConstant
import no.nav.paop.arenaMarshaller
import no.nav.paop.arenabrevMarshaller
import no.nav.paop.extractOppfolginsplan2012
import no.nav.paop.extractOppfolginsplan2014
import no.nav.paop.extractOppfolginsplan2016
import no.nav.paop.newInstance
import no.nav.paop.toString
import java.util.GregorianCalendar
import javax.jms.MessageProducer
import javax.jms.Session

fun createArenaOppfolgingsplan(
    metadata: IncomingMetadata,
    arenaBistand: ArenaBistand
): ArenaOppfolgingPlan = ArenaOppfolgingPlan().apply {
            eiaDokumentInfo = EiaDokumentInfoType().apply {
                dokumentInfo = DokumentInfoType().apply {
                    dokumentType = PaopConstant.dokumentType2913.string
                    dokumentreferanse = metadata.archiveReference
                    ediLoggId = metadata.archiveReference
                    dokumentDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
                }
                behandlingInfo = null
                avsender = EiaDokumentInfoType.Avsender().apply {
                    arbeidsgiver = ArbeidsgiverType().apply {
                        arbeidsgiverOrgNr = metadata.senderOrgId
                    }
                }
                avsenderSystem = EiaDokumentInfoType.AvsenderSystem().apply {
                    systemNavn = metadata.senderSystemName
                    systemVersjon = metadata.senderSystemVersion
                }
            }
            bedriftsNr = metadata.senderOrgId
            fodselsNr = metadata.userPersonNumber
            bistandNav = ArenaOppfolgingPlan.BistandNav().apply {
                isBistandNavHjelpemid = arenaBistand.bistandNavHjelpemidler
                isBistandNavVeil = arenaBistand.bistandNavVeiledning
                isBistandNavDialogmote = arenaBistand.bistandDialogmote
                isBistandNavVirke = arenaBistand.bistandVirkemidler
            }
}
fun createArenaBrevTilArbeidsgiver(
    metadata: IncomingMetadata
): ArenaBrevTilArbeidsgiver = ArenaBrevTilArbeidsgiver().apply {
    eiaDokumentInfo = no.nav.model.arenaBrevTilArbeidsgiver.EiaDokumentInfoType().apply {
        dokumentInfo = no.nav.model.arenaBrevTilArbeidsgiver.DokumentInfoType().apply {
            dokumentType = "EIA.OFP_AG"
            dokumentTypeVersjon = "1.0"
            dokumentreferanse = metadata.archiveReference
            ediLoggId = metadata.archiveReference
        }
        avsender = no.nav.model.arenaBrevTilArbeidsgiver.EiaDokumentInfoType.Avsender().apply {
            arbeidsgiver = no.nav.model.arenaBrevTilArbeidsgiver.ArbeidsgiverType().apply {
                arbeidsgiverOrgNr = metadata.senderOrgId
            }
        }
    }
    bedriftsNr = metadata.senderOrgId
    fodselsNr = metadata.userPersonNumber
}

fun extractAvsenderSystemSystemnavn(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.avsenderSystem.value.systemNavn.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.avsenderSystem.value.systemNavn.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.avsenderSystem.value.systemNavn.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun extractAvsenderSystemSystemVersjon(formData: String, oppfolgingPlanType: Oppfolginsplan): String =
        when (oppfolgingPlanType) {
            Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold.avsenderSystem.value.systemVersjon.value
            Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold.avsenderSystem.value.systemVersjon.value
            Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold.avsenderSystem.value.systemVersjon.value
            else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
        }

fun sendArenaOppfolginsplan(
    producer: MessageProducer,
    session: Session,
    incomingMetadata: IncomingMetadata,
    arenaBistand: ArenaBistand
) = producer.send(session.createTextMessage().apply {
    val info = createArenaOppfolgingsplan(incomingMetadata, arenaBistand)
    text = arenaMarshaller.toString(info)
})

fun letterSentNotificationToArena(
    producer: MessageProducer,
    session: Session,
    metadata: IncomingMetadata
) = producer.send(session.createTextMessage().apply {
    val info = createArenaBrevTilArbeidsgiver(metadata)
    text = arenabrevMarshaller.toString(info)
})
