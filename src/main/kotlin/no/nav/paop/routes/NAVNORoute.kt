package no.nav.paop.routes

import net.logstash.logback.argument.StructuredArguments
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.model.dataBatch.DataBatch
import no.nav.model.navOppfPlan.OppfolgingsplanMetadata
import no.nav.paop.client.createArenaBrevdata
import no.nav.paop.client.createJoarkRequest
import no.nav.paop.client.createPhysicalLetter
import no.nav.paop.client.sendArenaOppfolginsplan
import no.nav.paop.log
import no.nav.paop.model.ArenaBistand
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.xml.arenabrevdataMarshaller
import no.nav.paop.xml.dataBatchUnmarshaller
import no.nav.paop.xml.extractNavOppfPlan
import no.nav.paop.xml.toString
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.StedsadresseNorge
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.schemas.reg.flr.IFlrReadOperations
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.StringReader
import javax.jms.MessageProducer
import javax.jms.Session

fun handleNAVFollowupPlan(
    record: ConsumerRecord<String, ExternalAttachment>,
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session
) {
    val dataBatch = dataBatchUnmarshaller.unmarshal(StringReader(record.value().getBatch())) as DataBatch
    val payload = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData

    val attachment = dataBatch.attachments?.attachment?.firstOrNull()?.value

    val oppfPlan = extractNavOppfPlan(payload)
    val usesNavTemplate = !oppfPlan.isBistandHjelpemidler

    val org = organisasjonV4.hentOrganisasjon(HentOrganisasjonRequest().apply {
        orgnummer = oppfPlan.bedriftsNr
    }).organisasjon

    val incomingMetadata = IncomingMetadata(
            archiveReference = record.value().getArchiveReference(),
            senderOrgName = (org.navn as UstrukturertNavn).navnelinje.first(),
            senderOrgId = oppfPlan.bedriftsNr,
            senderSystemName = "NAV_NO",
            senderSystemVersion = "UNKNOWN",
            userPersonNumber = oppfPlan.fodselsNr
    )

    val logKeys = arrayOf(
            StructuredArguments.keyValue("archiveReference", record.value().getArchiveReference()),
            StructuredArguments.keyValue("senderOrganisationNumber", incomingMetadata.senderOrgId),
            StructuredArguments.keyValue("topic", record.topic())
    )
    val logFormat = logKeys.joinToString(",", "(", ")") { "{}" }

    log.info("Received a nav.no oppfølgingsplan $logFormat", *logKeys)

    val bistand = ArenaBistand(
            bistandNavHjelpemidler = oppfPlan.isBistandHjelpemidler,
            bistandNavVeiledning = oppfPlan.isBistandRaadOgVeiledning,
            bistandDialogmote = oppfPlan.isBistandDialogMoeteMedNav,
            bistandVirkemidler = oppfPlan.isBistandArbeidsrettedeTiltakOgVirkemidler
    )

    if (usesNavTemplate) {
        handleNAVFollowupPlanNAVTemplate(journalbehandling, fastlegeregisteret, dokumentProduksjonV3,
                arenaProducer, session, oppfPlan, bistand, attachment, incomingMetadata, org)
    } else {
        val joarkRequest = createJoarkRequest(incomingMetadata, attachment)
        journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)
        sendArenaOppfolginsplan(arenaProducer, session, incomingMetadata, bistand)
    }
}

fun handleNAVFollowupPlanNAVTemplate(
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session,
    oppfolgingsplan: OppfolgingsplanMetadata,
    arenaBistand: ArenaBistand,
    attachment: ByteArray?,
    incomingMetadata: IncomingMetadata,
    org: Organisasjon
) {
    if (oppfolgingsplan.mottaksinformasjon.isOppfoelgingsplanSendesTiNav) {
        val joarkRequest = createJoarkRequest(incomingMetadata, attachment)
        journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

        sendArenaOppfolginsplan(arenaProducer, session, incomingMetadata, arenaBistand)
    }
    if (oppfolgingsplan.mottaksinformasjon.isOppfoelgingsplanSendesTilFastlege) {
        val patientToGPContractAssociation = fastlegeregisteret.getPatientGPDetails(oppfolgingsplan.fodselsNr)

        if (patientToGPContractAssociation.gpContract != null) {
            val orgname = patientToGPContractAssociation.gpContract.gpOffice.name
            val orgNr = patientToGPContractAssociation.gpContract.gpOffice.organizationNumber.toString()
            val orgpostnummer = patientToGPContractAssociation.gpContract.gpOffice.physicalAddresses.physicalAddress.first().postalCode.toString()
            val orgpoststed = patientToGPContractAssociation.gpContract.gpOffice.physicalAddresses.physicalAddress.first().city.toString()

            // TODO TMP
            val brevdata = arenabrevdataMarshaller.toString(createArenaBrevdata())

            createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, incomingMetadata, orgNr, orgname, orgpostnummer, orgpoststed, brevdata)
        }
    } else {
        val address = org.organisasjonDetaljer.postadresse.find { it is StedsadresseNorge } as StedsadresseNorge
        val orgpostnummer = address.poststed.value
        val orgpoststed = "Kirkenær"

        // TODO TMP
        val brevdata = arenabrevdataMarshaller.toString(createArenaBrevdata())

        createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, incomingMetadata,
                incomingMetadata.senderOrgId, incomingMetadata.senderOrgName, orgpostnummer, orgpoststed,
                brevdata)
    }
}
