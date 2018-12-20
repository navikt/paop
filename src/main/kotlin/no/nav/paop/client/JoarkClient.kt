package no.nav.paop.client

import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import no.nav.paop.PaopConstant
import no.nav.paop.log
import no.nav.paop.model.ReceivedOppfolginsplan
import no.nav.paop.xml.datatypeFactory
import no.nav.tjeneste.virksomhet.behandlejournal.v2.binding.BehandleJournalV2
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Arkivfiltyper
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Arkivtemaer
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Dokumenttyper
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.EksternPart
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Kommunikasjonskanaler
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Organisasjon
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Person
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Sak
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Signatur
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.StrukturertInnhold
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.Variantformater
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.journalfoerinngaaendehenvendelse.DokumentinfoRelasjon
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.journalfoerinngaaendehenvendelse.JournalfoertDokumentInfo
import no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.journalfoerinngaaendehenvendelse.Journalpost
import no.nav.tjeneste.virksomhet.behandlejournal.v2.meldinger.JournalfoerInngaaendeHenvendelseRequest
import no.nav.tjeneste.virksomhet.behandlejournal.v2.meldinger.JournalfoerInngaaendeHenvendelseResponse
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar

fun onJournalRequest(
    receivedOppfolginsplan: ReceivedOppfolginsplan,
    fagmelding: ByteArray,
    behandleJournalV2: BehandleJournalV2,
    caseId: String,
    logKeys: String,
    logValues: Array<StructuredArgument>
) {
    val journalpost = createJournalpost(behandleJournalV2, receivedOppfolginsplan, fagmelding, caseId)
    log.info("Message successfully persisted in Joark {} $logKeys", StructuredArguments.keyValue("journalpostId", journalpost.journalpostId), *logValues)
}

fun createJournalpost(
    behandleJournalV2: BehandleJournalV2,
    recivedoppfolginsplan: ReceivedOppfolginsplan,
    pdf: ByteArray,
    caseId: String
): JournalfoerInngaaendeHenvendelseResponse =
        behandleJournalV2.journalfoerInngaaendeHenvendelse(JournalfoerInngaaendeHenvendelseRequest()
                .withApplikasjonsID(PaopConstant.PAOP.string)
                .withJournalpost(Journalpost()
                        .withDokumentDato(now())
                        .withJournalfoerendeEnhetREF(PaopConstant.GOSYS.string)
                        .withKanal(Kommunikasjonskanaler().withValue(PaopConstant.ALTINN.string))
                        .withSignatur(Signatur().withSignert(true))
                        .withArkivtema(Arkivtemaer().withValue(PaopConstant.SYK.string))
                        .withForBruker(Person().withIdent(no.nav.tjeneste.virksomhet.behandlejournal.v2.informasjon.behandlejournal.NorskIdent().withIdent(recivedoppfolginsplan.userPersonNumber)))
                        .withOpprettetAvNavn(PaopConstant.PAOPSak.string)
                        .withInnhold(PaopConstant.Oppfoløginsplan.string)
                        .withEksternPart(EksternPart()
                                .withNavn(recivedoppfolginsplan.senderOrgId)
                                .withEksternAktoer(Organisasjon().withOrgnummer(recivedoppfolginsplan.senderOrgId))
                        )
                        .withGjelderSak(Sak().withSaksId(caseId).withFagsystemkode(PaopConstant.GOSYS.string))
                        .withMottattDato(now())
                        .withDokumentinfoRelasjon(DokumentinfoRelasjon()
                                .withTillknyttetJournalpostSomKode(PaopConstant.JournalpostSomKodeHouveddokument.string)
                                .withJournalfoertDokument(JournalfoertDokumentInfo()
                                        .withBegrensetPartsInnsyn(false)
                                        .withDokumentType(Dokumenttyper().withValue(PaopConstant.ES.string))
                                        .withSensitivitet(true)
                                        .withTittel(PaopConstant.Oppfoløginsplan.string)
                                        .withKategorikode(PaopConstant.ES.string)
                                        .withBeskriverInnhold(
                                                StrukturertInnhold()
                                                        .withFilnavn("oppfolginsplan.pdf")
                                                        .withFiltype(Arkivfiltyper().withValue(PaopConstant.PDF.string))
                                                        .withVariantformat(Variantformater().withValue(PaopConstant.ARKIV.string))
                                                        .withInnhold(pdf),
                                                StrukturertInnhold()
                                                        .withFilnavn("oppfolginsplan.xml")
                                                        .withFiltype(Arkivfiltyper().withValue(PaopConstant.XML.string))
                                                        .withVariantformat(Variantformater().withValue(PaopConstant.ORIGINAL.string))
                                                        .withInnhold(objectMapper.writeValueAsBytes(recivedoppfolginsplan.oppfolginsplan))
                                        )
                                )
                        )
                )
        )
fun now(): XMLGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()))