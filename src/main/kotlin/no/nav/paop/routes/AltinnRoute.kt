package no.nav.paop.routes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.emottak.schemas.HentPartnerIDViaOrgnummerRequest
import no.nav.emottak.schemas.PartnerResource
import no.nav.model.dataBatch.DataBatch
import no.nav.model.oppfolgingsplan2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.paop.client.PdfClient
import no.nav.paop.client.PdfType
import no.nav.paop.client.createAltinnMessage
import no.nav.paop.client.createArenaBrevdata
import no.nav.paop.client.createDialogmelding
import no.nav.paop.client.createJoarkRequest
import no.nav.paop.client.createPhysicalLetter
import no.nav.paop.client.sendArenaOppfolginsplan
import no.nav.paop.client.sendDialogmeldingOppfolginsplan
import no.nav.paop.log
import no.nav.paop.mapping.mapFormdataToFagmelding
import no.nav.paop.model.ArenaBistand
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.model.IncomingUserInfo
import no.nav.paop.xml.arenabrevdataMarshaller
import no.nav.paop.xml.dataBatchUnmarshaller
import no.nav.paop.xml.extractGPFirstName
import no.nav.paop.xml.extractGPFnr
import no.nav.paop.xml.extractGPHprNumber
import no.nav.paop.xml.extractGPLastName
import no.nav.paop.xml.extractGPMiddleName
import no.nav.paop.xml.extractGPName
import no.nav.paop.xml.extractGPOfficePhysicalAddress
import no.nav.paop.xml.extractGPOfficePostalCode
import no.nav.paop.xml.toString
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.adresseregisteret.ICommunicationPartyService
import no.nhn.schemas.reg.flr.IFlrReadOperations
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.StringReader
import javax.jms.MessageProducer
import javax.jms.Session

val xmlMapper: ObjectMapper = XmlMapper(JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}).registerModule(JaxbAnnotationModule())
        .registerKotlinModule()
        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

fun handleAltinnFollowupPlan(
    record: ConsumerRecord<String, ExternalAttachment>,
    pdfClient: PdfClient,
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
    dokumentProduksjonV3: DokumentproduksjonV3,
    adresseRegisterV1: ICommunicationPartyService,
    partnerEmottak: PartnerResource,
    iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    arenaProducer: MessageProducer,
    receiptProducer: MessageProducer,
    session: Session,
    altinnUserUsername: String,
    altinnUserPassword: String
) {
    val dataBatch = dataBatchUnmarshaller.unmarshal(StringReader(record.value().getBatch())) as DataBatch
    val payload = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
    val oppfolgingsplan = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(payload)
    val skjemainnhold = oppfolgingsplan.skjemainnhold

    val incomingMetadata = IncomingMetadata(
            archiveReference = record.value().getArchiveReference(),
            senderOrgName = skjemainnhold.arbeidsgiver.orgnavn,
            senderOrgId = skjemainnhold.arbeidsgiver.orgnr,
            senderSystemName = skjemainnhold.avsenderSystem.systemNavn,
            senderSystemVersion = skjemainnhold.avsenderSystem.systemVersjon,
            userPersonNumber = skjemainnhold.sykmeldtArbeidstaker.fnr
    )

    val logKeys = arrayOf(
            keyValue("archiveReference", record.value().getArchiveReference()),
            keyValue("senderOrganisationNumber", incomingMetadata.senderOrgId),
            keyValue("topic", record.topic())
    )
    val logFormat = logKeys.joinToString(",", "(", ")") { "{}" }

    log.info("Received a Altinn oppfølgingsplan $logFormat", *logKeys)

    val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFormdataToFagmelding(skjemainnhold, incomingMetadata))

    val validOrganizationNumber = organisasjonV4.validerOrganisasjon(ValiderOrganisasjonRequest().apply {
            orgnummer = incomingMetadata.senderOrgId
        }).isGyldigOrgnummer
    if (!validOrganizationNumber) {
        throw RuntimeException("Failed because the incoming organization ${incomingMetadata.senderOrgId} was invalid")
    }

    val incomingPersonInfo = IncomingUserInfo(
            userFamilyName = skjemainnhold.sykmeldtArbeidstaker?.fornavn,
            userGivenName = skjemainnhold.sykmeldtArbeidstaker?.etternavn,
            userPersonNumber = skjemainnhold.sykmeldtArbeidstaker.fnr
    )

    val arenaBistand = ArenaBistand(
            bistandNavHjelpemidler = skjemainnhold.tiltak.tiltaksinformasjon.any { it.isBistandHjelpemidler },
            bistandNavVeiledning = skjemainnhold.tiltak.tiltaksinformasjon.any { it.isBistandRaadOgVeiledning },
            bistandDialogmote = skjemainnhold.tiltak.tiltaksinformasjon.any { it.isBistandDialogMoeteMedNav },
            bistandVirkemidler = skjemainnhold.tiltak.tiltaksinformasjon.any { it.isBistandArbeidsrettedeTiltakOgVirkemidler }
    )

    if (skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTiNav) {
        val joarkRequest = createJoarkRequest(incomingMetadata, fagmelding)
        journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)
        sendArenaOppfolginsplan(arenaProducer, session, incomingMetadata, arenaBistand)
    }
    if (skjemainnhold.mottaksInformasjon.isOppfolgingsplanSendesTilFastlege) {
        handleDoctorFollowupPlanAltinn(fastlegeregisteret, dokumentProduksjonV3, adresseRegisterV1,
                partnerEmottak, iCorrespondenceAgencyExternalBasic, arenaProducer, receiptProducer, session, incomingMetadata, incomingPersonInfo, fagmelding, altinnUserUsername, altinnUserPassword)
    }
}

fun handleDoctorFollowupPlanAltinn(
    fastlegeregisteret: IFlrReadOperations,
    dokumentProduksjonV3: DokumentproduksjonV3,
    adresseRegisterV1: ICommunicationPartyService,
    partnerEmottak: PartnerResource,
    iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    arenaProducer: MessageProducer,
    receiptProducer: MessageProducer,
    session: Session,
    incomingMetadata: IncomingMetadata,
    incomingPersonInfo: IncomingUserInfo,
    fagmelding: ByteArray,
    altinnUserUsername: String,
    altinnUserPassword: String
) {
    val patientToGPContractAssociation = fastlegeregisteret.getPatientGPDetails(incomingMetadata.userPersonNumber)

    if (patientToGPContractAssociation != null) {
        val gpName = patientToGPContractAssociation.extractGPName()
        val gpFirstName = patientToGPContractAssociation.extractGPFirstName()!!
        val gpMiddleName = patientToGPContractAssociation.extractGPMiddleName()
        val gpLastName = patientToGPContractAssociation.extractGPLastName()!!
        val gpFnr = patientToGPContractAssociation.extractGPFnr()
        val gpHprNumber = patientToGPContractAssociation.extractGPHprNumber()
        val gpOfficePostnr = patientToGPContractAssociation.extractGPOfficePostalCode()
        val gpOfficePoststed = patientToGPContractAssociation.extractGPOfficePhysicalAddress()

        val gpHerIdFlr = patientToGPContractAssociation.gpHerId

        val getCommunicationPartyDetailsResponse = adresseRegisterV1.getOrganizationPersonDetails(gpHerIdFlr)

        // Should only return one org
        val herIDAdresseregister = getCommunicationPartyDetailsResponse.organizations.organization.first().herId
        val gpOfficeOrganizationNumber = getCommunicationPartyDetailsResponse.organizations.organization.first().organizationNumber.toString()
        val gpOfficeOrganizationName = getCommunicationPartyDetailsResponse.organizations.organization.first().name

        val partner = partnerEmottak.hentPartnerIDViaOrgnummer(HentPartnerIDViaOrgnummerRequest().apply {
            orgnr = gpOfficeOrganizationNumber
        }).partnerInformasjon

        val canReceiveDialogMessage = partner.firstOrNull {
            it.heRid.toInt() == herIDAdresseregister
        }
        if (canReceiveDialogMessage != null) {
            val fellesformat = createDialogmelding(incomingMetadata, incomingPersonInfo, gpOfficeOrganizationName,
                    gpOfficeOrganizationNumber, herIDAdresseregister, fagmelding,
                    canReceiveDialogMessage, gpFirstName, gpMiddleName, gpLastName, gpHerIdFlr, gpFnr, gpHprNumber)

            sendDialogmeldingOppfolginsplan(receiptProducer, session, fellesformat)
            log.info("Dialogmessage sendt to GP")
        } else {
            // TODO TMP
            val brevdata = arenabrevdataMarshaller.toString(createArenaBrevdata())

            createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, incomingMetadata, gpOfficeOrganizationNumber,
                    gpName, gpOfficePostnr, gpOfficePoststed, brevdata)
        }
        log.info("PhysicalLetter sendt to GP")
    } else {
        createAltinnMessage(iCorrespondenceAgencyExternalBasic, incomingMetadata.archiveReference,
                incomingMetadata.senderOrgId, fagmelding, altinnUserUsername, altinnUserPassword)
        log.info("Oppfølginsplan sendt to altinn")
    }
}
