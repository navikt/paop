package no.nav.paop.client

import no.nav.paop.IncomingMetadata
import no.nav.paop.PaopConstant
import no.nav.paop.newInstance
import no.nav.paop.wrapFormData
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Dokumentbestillingsinformasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagomraader
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Fagsystemer
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.NorskPostadresse
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.Bruker
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.DokumentInfo
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.Fildetaljer
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.JournalpostDokumentInfoRelasjon
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.LagreDokumentOgOpprettJournalpostRequest
import java.util.GregorianCalendar

fun createJoarkRequest(
    metadata: IncomingMetadata,
    fagmelding: ByteArray
): LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {
    journalpostDokumentInfoRelasjonListe.add(
                JournalpostDokumentInfoRelasjon().apply {
                    dokumentInfo = DokumentInfo().apply {
                        begrensetPartsinnsynFraTredjePart = false

                        fildetaljerListe.add(Fildetaljer().apply {
                            fil = fagmelding
                            filnavn = "${metadata.archiveReference}.pdf"
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
        brukerId = metadata.userPersonNumber
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
    avsenderMottaker = metadata.senderOrgName
    avsenderMottakerId = metadata.senderOrgId
    opprettetAvNavn = PaopConstant.eiaAuto.string
}

fun createProduserIkkeredigerbartDokumentRequest(
    incomingMetadata: IncomingMetadata,
    receiverOrgNumber: String,
    gpName: String,
    postnummerString: String?,
    poststedString: String?,
    xmlContent: String
): ProduserIkkeredigerbartDokumentRequest = ProduserIkkeredigerbartDokumentRequest().apply {
    dokumentbestillingsinformasjon = Dokumentbestillingsinformasjon().apply {
        dokumenttypeId = "brev"
        bestillendeFagsystem = Fagsystemer().apply {
            value = "PAOP"
        }
        bruker = Person().apply {
            navn = "NAV Servicesenter"
            ident = "NAV ORGNR"
        }
        mottaker = Organisasjon().apply {
            navn = gpName
            orgnummer = receiverOrgNumber
        }
        journalsakId = incomingMetadata.archiveReference
        sakstilhoerendeFagsystem = Fagsystemer().apply {
            value = "ARENA"
        }
        dokumenttilhoerendeFagomraade = Fagomraader().apply {
            value = "Sykefravær"
        }
        journalfoerendeEnhet = "N/A"
        adresse = NorskPostadresse().apply {
            adresselinje1 = "stat"
            land = Landkoder().apply {
                value = "NOR"
            }
            postnummer = postnummerString
            poststed = poststedString
        }
        isFerdigstillForsendelse = true
        isInkludererEksterneVedlegg = false
    }
    brevdata = wrapFormData(xmlContent)
}
