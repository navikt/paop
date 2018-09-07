package no.nav.paop.client

import no.nav.paop.Oppfolginsplan
import no.nav.paop.PaopConstant
import no.nav.paop.newInstance
import no.nav.paop.mapping.extractOrgNr
import no.nav.paop.mapping.extractOrgnavn
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFnr
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.Bruker
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.DokumentInfo
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.Fildetaljer
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.JournalpostDokumentInfoRelasjon
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.LagreDokumentOgOpprettJournalpostRequest
import java.util.GregorianCalendar

fun createJoarkRequest(formData: String, oppfolginsplanType: Oppfolginsplan, edilogg: String, fagmelding: ByteArray):
        LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {

    journalpostDokumentInfoRelasjonListe.add(
                JournalpostDokumentInfoRelasjon().apply {
                    dokumentInfo = DokumentInfo().apply {
                        begrensetPartsinnsynFraTredjePart = false

                        fildetaljerListe.add(Fildetaljer().apply {
                            fil = fagmelding
                            filnavn = edilogg
                            filtypeKode = PaopConstant.pdf.string
                            variantFormatKode = PaopConstant.arkiv.string
                            versjon = 1
                        })
                        kategoriKode = PaopConstant.kategoriKodeES.string
                        tittel = PaopConstant.OppfolingsplanFraArbeidsgiver.string
                        brevkode = PaopConstant.brevkode900003.string
                        sensitivt = false
                        organInternt = false
                        versjon = 1
                    }
                    tilknyttetJournalpostSomKode = PaopConstant.houveddokument.string
                    tilknyttetAvNavn = PaopConstant.eiaAuto.string
                    versjon = 1
                }
            )

    gjelderListe.add(Bruker().apply {
        brukerId = extractSykmeldtArbeidstakerFnr(formData, oppfolginsplanType)
        brukertypeKode = PaopConstant.person.string
    })

    merknad = PaopConstant.OppfolingsplanFraArbeidsgiver.string
    mottakskanalKode = PaopConstant.eia.string
    mottattDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
    innhold = PaopConstant.OppfolingsplanFraArbeidsgiver.string
    journalForendeEnhetId = null
    journalposttypeKode = PaopConstant.journalposttypeKodeI.string
    journalstatusKode = PaopConstant.journalstatusKodeMO.string
    dokumentDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
    fagomradeKode = PaopConstant.opp.string
    fordeling = PaopConstant.eiaOk.string
    avsenderMottaker = extractOrgnavn(formData, oppfolginsplanType)
    avsenderMottakerId = extractOrgNr(formData, oppfolginsplanType)
    opprettetAvNavn = PaopConstant.eiaAuto.string
}
