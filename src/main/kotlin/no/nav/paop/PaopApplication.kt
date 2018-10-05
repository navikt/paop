package no.nav.paop

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.UserTypeRestriction
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2
import no.kith.xmlstds.XMLCV
import no.kith.xmlstds.base64container.XMLBase64Container
import no.kith.xmlstds.dialog._2006_10_11.XMLNotat
import no.kith.xmlstds.dialog._2006_10_11.XMLPerson
import no.kith.xmlstds.dialog._2006_10_11.XMLRollerRelatertNotat
import no.kith.xmlstds.msghead._2006_05_24.XMLCS
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.kith.xmlstds.msghead._2006_05_24.XMLHealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgInfo
import no.kith.xmlstds.msghead._2006_05_24.XMLOrganisation
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.kith.xmlstds.msghead._2006_05_24.XMLRefDoc
import no.kith.xmlstds.msghead._2006_05_24.XMLSender
import no.kith.xmlstds.msghead._2006_05_24.XMLTS
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.emottak.schemas.HentPartnerIDViaOrgnummerRequest
import no.nav.emottak.schemas.PartnerInformasjon
import no.nav.emottak.schemas.PartnerResource
import no.nav.model.arena.brev.AktivitetsType
import no.nav.model.arena.brev.BesoksadresseType
import no.nav.model.arena.brev.FagType
import no.nav.model.arena.brev.FellesType
import no.nav.model.arena.brev.KontaktInformasjonType
import no.nav.model.arena.brev.MoteInfoType
import no.nav.model.arena.brev.MottakerAdresseType
import no.nav.model.arena.brev.MottakerType
import no.nav.model.arena.brev.MottakerTypeKode
import no.nav.model.arena.brev.PostadresseType
import no.nav.model.arena.brev.ReturadresseType
import no.nav.model.arena.brev.SakspartType
import no.nav.model.arena.brev.SakspartTypeKode
import no.nav.model.arena.brev.SignerendeSaksbehandlerType
import no.nav.model.arena.brevdata.Brevdata
import no.nav.model.dataBatch.DataBatch
import no.nav.model.navOppfPlan.OppfolgingsplanMetadata
import no.nav.paop.client.PdfClient
import no.nav.paop.client.PdfType
import no.nav.paop.client.createJoarkRequest
import no.nav.paop.client.createProduserIkkeredigerbartDokumentRequest
import no.nav.paop.client.extractAvsenderSystemSystemVersjon
import no.nav.paop.client.extractAvsenderSystemSystemnavn
import no.nav.paop.client.letterSentNotificationToArena
import no.nav.paop.client.sendArenaOppfolginsplan
import no.nav.paop.client.sendDialogmeldingOppfolginsplan
import no.nav.paop.mapping.extractOrgNr
import no.nav.paop.mapping.extractOrgnavn
import no.nav.paop.mapping.extractSykmeldtArbeidstakerEtternavn
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFnr
import no.nav.paop.mapping.extractSykmeldtArbeidstakerFornavn
import no.nav.paop.mapping.extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler
import no.nav.paop.mapping.extractTiltakBistandDialogMoeteMedNav
import no.nav.paop.mapping.extractTiltakBistandHjelpemidler
import no.nav.paop.mapping.extractTiltakBistandRaadOgVeiledning
import no.nav.paop.mapping.mapFormdataToFagmelding
import no.nav.paop.model.ArenaBistand
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.model.IncomingUserInfo
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.FinnOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.v1.binding.Journalbehandling
import no.nhn.adresseregisteret.ICommunicationPartyService
import no.nhn.schemas.reg.flr.IFlrReadOperations
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import no.trygdeetaten.xml.eiff._1.XMLEIFellesformat
import no.trygdeetaten.xml.eiff._1.XMLMottakenhetBlokk
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDateTime
import java.util.GregorianCalendar
import java.util.UUID
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

val log: Logger = LoggerFactory.getLogger("nav.paop-application")
val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

fun handleOppfoelgingsplan(
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
    val serviceCode = record.value().getServiceCode()
    val serviceEditionCode = record.value().getServiceEditionCode()
    val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
    var oppfolgingsplanType = findOppfolingsplanType(serviceCode, serviceEditionCode)
    oppfolgingsplanType = Oppfolginsplan.OP2016 // TODO: Delete after initial testing
    log.info("record.value().getArchiveReference(): {}", record.value().getArchiveReference())

    val incomingMetadata = IncomingMetadata(
            archiveReference = record.value().getArchiveReference(),
            senderOrgName = extractOrgnavn(formData, oppfolgingsplanType),
            senderOrgId = extractOrgNr(formData, oppfolgingsplanType),
            senderSystemName = extractAvsenderSystemSystemnavn(formData, oppfolgingsplanType),
            senderSystemVersion = extractAvsenderSystemSystemVersjon(formData, oppfolgingsplanType),
            userPersonNumber = extractSykmeldtArbeidstakerFnr(formData, oppfolgingsplanType)
    )

    val incomingPersonInfo = IncomingUserInfo(
            userPersonNumber = extractSykmeldtArbeidstakerFnr(formData, oppfolgingsplanType),
            userFamilyName = extractSykmeldtArbeidstakerFornavn(formData, oppfolgingsplanType),
            userGivenName = extractSykmeldtArbeidstakerEtternavn(formData, oppfolgingsplanType)
    )

    val arenaBistand = ArenaBistand(
            bistandNavHjelpemidler = extractTiltakBistandHjelpemidler(formData, oppfolgingsplanType),
            bistandNavVeiledning = extractTiltakBistandRaadOgVeiledning(formData, oppfolgingsplanType),
            bistandDialogmote = extractTiltakBistandDialogMoeteMedNav(formData, oppfolgingsplanType),
            bistandVirkemidler = extractTiltakBistandArbeidsrettedeTiltakOgVirkemidler(formData, oppfolgingsplanType)
    )

    val attachment = dataBatch.attachments?.attachment?.firstOrNull()?.value

    val validOrganizationNumber = try {
        organisasjonV4.validerOrganisasjon(ValiderOrganisasjonRequest().apply {
            orgnummer = extractOrgNr(formData, oppfolgingsplanType)
        }).isGyldigOrgnummer
    } catch (e: Exception) {
        log.error("Failed to validate organization number due to an exception", e)
        false
    }

    if (!validOrganizationNumber) {
        // TODO: Do something else then silently fail
        return
    }

    if (oppfolgingsplanType in arrayOf(Oppfolginsplan.OP2012, Oppfolginsplan.OP2014, Oppfolginsplan.OP2016)) {
        val fagmelding = pdfClient.generatePDF(PdfType.FAGMELDING, mapFormdataToFagmelding(formData, oppfolgingsplanType))
        if (isFollowupPlanForNAV(formData, oppfolgingsplanType)) {
            val joarkRequest = createJoarkRequest(incomingMetadata, fagmelding)
            journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)
            sendArenaOppfolginsplan(arenaProducer, session, incomingMetadata, arenaBistand)
        }
        if (isFollowupPlanForFastlege(formData, oppfolgingsplanType)) {
            handleDoctorFollowupPlanAltinn(fastlegeregisteret, organisasjonV4, dokumentProduksjonV3, adresseRegisterV1,
                    partnerEmottak, iCorrespondenceAgencyExternalBasic, arenaProducer, receiptProducer, session, incomingMetadata, incomingPersonInfo, fagmelding, altinnUserUsername, altinnUserPassword)
        }
    } else if (oppfolgingsplanType == Oppfolginsplan.NAVOPPFPLAN) {

        val extractOppfolginsplan = extractNavOppfPlan(formData)
        val usesNavTemplate = !extractOppfolginsplan.isBistandHjelpemidler

        // TODO: Don't silently fail
        if (usesNavTemplate) {
            handleNAVFollowupPlanNAVTemplate(journalbehandling, fastlegeregisteret, organisasjonV4, dokumentProduksjonV3,
                    arenaProducer, session, extractOppfolginsplan, arenaBistand, attachment, incomingMetadata)
        } else {
            val fagmelding = dataBatch.attachments.attachment.first().value
            val joarkRequest = createJoarkRequest(incomingMetadata, fagmelding)
            journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)
            sendArenaOppfolginsplan(arenaProducer, session, incomingMetadata, arenaBistand)
        }
    } else {
        throw Exception("Unvalid oppfolingsplan type")
    }
}

fun handleNAVFollowupPlanNAVTemplate(
    journalbehandling: Journalbehandling,
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session,
    oppfolgingsplan: OppfolgingsplanMetadata,
    arenaBistand: ArenaBistand,
    attachment: ByteArray?,
    incomingMetadata: IncomingMetadata
) {
    if (oppfolgingsplan.mottaksinformasjon.isOppfoelgingsplanSendesTiNav) {
        val joarkRequest = createJoarkRequest(incomingMetadata, attachment)
        journalbehandling.lagreDokumentOgOpprettJournalpost(joarkRequest)

        sendArenaOppfolginsplan(arenaProducer, session, incomingMetadata, arenaBistand)
    }
    if (oppfolgingsplan.mottaksinformasjon.isOppfoelgingsplanSendesTilFastlege) {
        var fastlegefunnet = false
        val patientFnr = oppfolgingsplan.fodselsNr
        var patientToGPContractAssociation = PatientToGPContractAssociation()
        try {
            patientToGPContractAssociation = fastlegeregisteret.getPatientGPDetails(patientFnr)
            fastlegefunnet = true
        } catch (e: Exception) {
            log.error("Call to flr returned Exception", e)
        }

        if (fastlegefunnet && patientToGPContractAssociation.gpContract != null) {
            val orgname = patientToGPContractAssociation.gpContract.value.gpOffice.value.name.value
            val orgNr = patientToGPContractAssociation.gpContract.value.gpOffice.value.organizationNumber.toString()
            val orgpostnummer = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().postalCode.toString()
            val orgpoststed = patientToGPContractAssociation.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().city.value.toString()

            // TODO TMP
            val brevdata = arenabrevdataMarshaller.toString(createArenaBrevdata())

            createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, incomingMetadata, orgNr, orgname, orgpostnummer, orgpoststed, brevdata)
        }
    } else {

        val hentOrganisasjonRequest = HentOrganisasjonRequest().apply {
            orgnummer = incomingMetadata.senderOrgId
        }
        val hentOrganisasjonResponse = organisasjonV4.hentOrganisasjon(hentOrganisasjonRequest)

        val finnOrganisasjonRequest = FinnOrganisasjonRequest().apply {
            navn = hentOrganisasjonResponse.organisasjon.navn.toString()
        }
        val finnOrganisasjonResponse = organisasjonV4.finnOrganisasjon(finnOrganisasjonRequest)

        val orgpostnummer = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()!!.postnummer.value
        val orgpoststed = finnOrganisasjonResponse.organisasjonSammendragListe.firstOrNull()!!.poststed

        // TODO TMP
        val brevdata = arenabrevdataMarshaller.toString(createArenaBrevdata())

        createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, incomingMetadata,
                incomingMetadata.senderOrgId, incomingMetadata.senderOrgName, orgpostnummer, orgpoststed,
                brevdata)
    }
}

fun createPhysicalLetter(
    dokumentProduksjonV3: DokumentproduksjonV3,
    arenaProducer: MessageProducer,
    session: Session,
    incomingMetadata: IncomingMetadata,
    receiverOrgNumber: String,
    gpName: String,
    postnummer: String,
    poststed: String,
    xmlContent: String
) {
    val brevrequest = createProduserIkkeredigerbartDokumentRequest(incomingMetadata, receiverOrgNumber, gpName, postnummer, poststed, xmlContent)
    try {
        dokumentProduksjonV3.produserIkkeredigerbartDokument(brevrequest)
        // TODO do we need to tell ARENA that the letter is sendt?
        letterSentNotificationToArena(arenaProducer, session, incomingMetadata)
    } catch (e: Exception) {
        log.error("Call to dokprod returned Exception", e)
    }
}

fun handleNonFastlegeFollowupPlan(
    fagmelding: ByteArray,
    iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    metadata: IncomingMetadata,
    altinnUserUsername: String,
    altinnUserPassword: String
) {
    log.info("metadata.archiveReference: {}", metadata.archiveReference)
    createAltinnMessage(iCorrespondenceAgencyExternalBasic, metadata.archiveReference, metadata.senderOrgId, fagmelding, altinnUserUsername, altinnUserPassword)
}

fun handleDoctorFollowupPlanAltinn(
    fastlegeregisteret: IFlrReadOperations,
    organisasjonV4: OrganisasjonV4,
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
    val patientToGPContractAssociation = try {
        fastlegeregisteret.getPatientGPDetails(incomingMetadata.userPersonNumber)
    } catch (e: Exception) {
        log.error("Call to flr returned Exception", e)
        // TODO: We shouldn't just fail here
        null
    }
    // TODO remove after testing
    handleNonFastlegeFollowupPlan(fagmelding, iCorrespondenceAgencyExternalBasic, incomingMetadata, altinnUserUsername, altinnUserPassword)

    if (patientToGPContractAssociation != null) {
        val gpName = patientToGPContractAssociation.extractGPName()
        val gpFirstName = patientToGPContractAssociation.extractGPFirstName()!!
        val gpMiddleName = patientToGPContractAssociation.extractGPMiddleName()
        val gpLastName = patientToGPContractAssociation.extractGPLastName()!!
        val gpFnr = patientToGPContractAssociation.extractGPFnr()
        val gpHprNumber = patientToGPContractAssociation.extractGPHprNumber()
        val gpOfficePostnr = patientToGPContractAssociation.extractGPOfficePostalCode()
        val gpOfficePoststed = patientToGPContractAssociation.extractGPOfficePhysicalAddress()

        val gpHerIdFlr = patientToGPContractAssociation.gpHerId.value

        val getCommunicationPartyDetailsResponse = adresseRegisterV1.getOrganizationPersonDetails(gpHerIdFlr)

        // Should only return one org
        val herIDAdresseregister = getCommunicationPartyDetailsResponse.organizations.value.organization.first().herId
        val gpOfficeOrganizationNumber = getCommunicationPartyDetailsResponse.organizations.value.organization.first().organizationNumber.toString()
        val gpOfficeOrganizationName = getCommunicationPartyDetailsResponse.organizations.value.organization.first().name.value

        val hentPartnerIDViaOrgnummerRequest = HentPartnerIDViaOrgnummerRequest().apply {
            orgnr = gpOfficeOrganizationNumber
        }

        val hentPartnerIDViaOrgnummerResponse = partnerEmottak.hentPartnerIDViaOrgnummer(hentPartnerIDViaOrgnummerRequest)

        val canReceiveDialogMessage = hentPartnerIDViaOrgnummerResponse.partnerInformasjon.firstOrNull {
            it.heRid.toInt() == herIDAdresseregister
        }
        if (canReceiveDialogMessage != null) {
            val fellesformat = createDialogmelding(incomingMetadata, incomingPersonInfo,
                    gpOfficeOrganizationName, gpOfficeOrganizationNumber, herIDAdresseregister, fagmelding,
                    canReceiveDialogMessage, gpFirstName, gpMiddleName, gpLastName, gpHerIdFlr, gpFnr, gpHprNumber)

            sendDialogmeldingOppfolginsplan(receiptProducer, session, fellesformat)
        } else {
            // TODO TMP
            val brevdata = arenabrevdataMarshaller.toString(createArenaBrevdata())

            createPhysicalLetter(dokumentProduksjonV3, arenaProducer, session, incomingMetadata, gpOfficeOrganizationNumber,
                    gpName, gpOfficePostnr, gpOfficePoststed, brevdata)
        }
    } else {
            handleNonFastlegeFollowupPlan(fagmelding, iCorrespondenceAgencyExternalBasic, incomingMetadata, altinnUserUsername, altinnUserPassword)
    }
}

fun connectionFactory(environment: Environment) = MQConnectionFactory().apply {
    hostName = environment.mqHostname
    port = environment.mqPort
    queueManager = environment.mqQueueManagerName
    transportType = WMQConstants.WMQ_CM_CLIENT
    // TODO mq crypo
    // sslCipherSuite = "TLS_RSA_WITH_AES_256_CBC_SHA"
    channel = environment.mqChannelName
    ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun Marshaller.toString(input: Any): String = StringWriter().use {
    marshal(input, it)
    it.toString()
}

fun isFollowupPlanForFastlege(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean = when (oppfolgingPlanType) {
    Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
    Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
    Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTilFastlege?.value
    else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
} ?: false

fun isFollowupPlanForNAV(formData: String, oppfolgingPlanType: Oppfolginsplan): Boolean = when (oppfolgingPlanType) {
    Oppfolginsplan.OP2012 -> extractOppfolginsplan2012(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
    Oppfolginsplan.OP2014 -> extractOppfolginsplan2014(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
    Oppfolginsplan.OP2016 -> extractOppfolginsplan2016(formData).skjemainnhold?.mottaksInformasjon?.value?.oppfolgingsplanSendesTiNav?.value
    else -> throw RuntimeException("Invalid oppfolginsplanType: $oppfolgingPlanType")
} ?: false

fun PatientToGPContractAssociation.extractGPName() =
        "${extractGPFirstName()} ${extractGPMiddleName()} ${extractGPLastName()}"

fun PatientToGPContractAssociation.extractGPFirstName(): String? =
        this.doctorCycles.value.gpOnContractAssociation.first().gp.value.firstName.value

fun PatientToGPContractAssociation.extractGPLastName(): String? =
        this.doctorCycles.value.gpOnContractAssociation.first().gp.value.lastName.value

fun PatientToGPContractAssociation.extractGPMiddleName(): String =
        this.doctorCycles.value.gpOnContractAssociation.first().gp.value.middleName.value

fun PatientToGPContractAssociation.extractGPFnr(): String =
        this.doctorCycles.value.gpOnContractAssociation.first().gp.value.nin.value

fun PatientToGPContractAssociation.extractGPHprNumber(): Int =
        this.doctorCycles.value.gpOnContractAssociation.first().hprNumber

fun PatientToGPContractAssociation.extractGPOfficePostalCode(): String =
        this.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().postalCode.toString()

fun PatientToGPContractAssociation.extractGPOfficePhysicalAddress(): String =
        this.gpContract.value.gpOffice.value.physicalAddresses.value.physicalAddress.first().city.value

val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().let {
    it.isNamespaceAware = true
    it.newDocumentBuilder()
}
fun wrapFormData(formData: String): Element = documentBuilder.parse(InputSource(StringReader(formData))).documentElement

fun createDialogmelding(
    incomingMetadata: IncomingMetadata,
    incomingPersonInfo: IncomingUserInfo,
    gpOrganizationName: String,
    gpOrganizationNumber: String,
    herIDAdresseregister: Int,
    fagmelding: ByteArray,
    canReceiveDialogMessage: PartnerInformasjon,
    gpfirstname: String,
    gpMiddelName: String,
    gpLastname: String,
    gpHerIdFlr: Int,
    gpFnr: String,
    gpHprNumber: Int
): XMLEIFellesformat = XMLEIFellesformat().apply {
    any.add(XMLMsgHead().apply {
        msgInfo = XMLMsgInfo().apply {
            type = XMLCS().apply {
                v = "DIALOG_NOTAT"
                dn = "Notat"
            }
            miGversion = "v1.2 2006-05-24"
            genDate = LocalDateTime.now()
            msgId = UUID.randomUUID().toString()
            ack = XMLCS().apply {
                dn = "Ja"
                v = "J"
            }
            sender = XMLSender().apply {
                organisation = XMLOrganisation().apply {
                    organisationName = "NAV"
                    ident.add(XMLIdent().apply {
                        id = "889640782"
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "Organisasjonsnummeret i Enhetsregisteret"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "ENH"
                        }
                    })
                    ident.add(XMLIdent().apply {
                        id = "79768"
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "HER"
                        }
                    })
                }
            }
            receiver = XMLReceiver().apply {
                organisation = XMLOrganisation().apply {
                    organisationName = gpOrganizationName
                    ident.add(XMLIdent().apply {
                        id = herIDAdresseregister.toString()
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "HER-id"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "HER"
                        }
                    })
                    ident.add(XMLIdent().apply {
                        id = gpOrganizationNumber
                        typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            dn = "Organisasjonsnummeret i Enhetsregisteret"
                            s = "2.16.578.1.12.4.1.1.9051"
                            v = "ENH"
                        }
                    })
                    healthcareProfessional = XMLHealthcareProfessional().apply {
                        roleToPatient = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                            v = "6"
                            s = "2.16.578.1.12.4.1.1.9034"
                            dn = "Fastlege"
                        }
                        familyName = gpLastname
                        givenName = gpfirstname
                        if (gpMiddelName.isNotBlank() && gpMiddelName.isNotEmpty()) {
                            middleName = gpMiddelName
                        }
                        ident.add(XMLIdent().apply {
                            id = gpHerIdFlr.toString()
                            typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                                dn = "HER-id"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "HER"
                            }
                        })
                        ident.add(XMLIdent().apply {
                            id = gpFnr
                            typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                                dn = "Fødselsnummer Norsk fødselsnummer"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "FNR"
                            }
                        })
                        ident.add(XMLIdent().apply {
                            id = gpHprNumber.toString()
                            typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                                dn = "HPR-nummer"
                                s = "2.16.578.1.12.4.1.1.8116"
                                v = "HPR"
                            }
                        })
                    }
                }
            }
            patient = XMLPatient().apply {
                familyName = incomingPersonInfo.userFamilyName
                givenName = incomingPersonInfo.userGivenName
                ident.add(XMLIdent().apply {
                    id = incomingPersonInfo.userPersonNumber
                    typeId = no.kith.xmlstds.msghead._2006_05_24.XMLCV().apply {
                        dn = "Fødselsnummer"
                        s = "2.16.578.1.12.4.1.1.8116"
                        v = "FNR"
                    }
                })
            }
        }
        document.add(XMLDocument().apply {
            documentConnection = XMLCS().apply {
                dn = "Hoveddokument"
                v = "H"
            }
            refDoc = XMLRefDoc().apply {
                issueDate = XMLTS().apply {
                    v = "${LocalDateTime.now().year}-${LocalDateTime.now().monthValue}-${LocalDateTime.now().dayOfMonth}"
                }
                msgType = XMLCS().apply {
                    dn = "XML-instans"
                    v = "XML"
                }
                mimeType = "text/xml"
                content = XMLRefDoc.Content().apply {
                    any.add(no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding().apply {
                        notat.add(XMLNotat().apply {
                            temaKodet = XMLCV().apply {
                                dn = "Oppfølgingsplan"
                                s = "2.16.578.1.12.4.1.1.8127"
                                v = "1"
                            }
                            tekstNotatInnhold = XMLNotat().apply {
                                tekstNotatInnhold = "Åpne PDF-vedlegg"
                                dokIdNotat = incomingMetadata.archiveReference
                            }
                            rollerRelatertNotat.add(XMLRollerRelatertNotat().apply {
                                rolleNotat = XMLCV().apply {
                                    v = "1"
                                    s = "2.16.578.1.12.4.1.1.9057"
                                }
                                person = XMLPerson()
                            })
                        })
                    })
                }
            }
        })

        document.add(XMLDocument().apply {
            documentConnection = XMLCS().apply {
                dn = "Vedlegg"
                v = "V"
            }
            refDoc = XMLRefDoc().apply {
                id = incomingMetadata.archiveReference
                issueDate = XMLTS().apply {
                    v = "${LocalDateTime.now().year}-${LocalDateTime.now().monthValue}-${LocalDateTime.now().dayOfMonth}"
                }
                msgType = XMLCS().apply {
                    dn = "Vedlegg"
                    v = "A"
                }
                mimeType = "application/pdf"
                content = XMLRefDoc.Content().apply {
                    any.add(XMLBase64Container().apply {
                        value = fagmelding
                    })
                }
            }
        })
    })
    any.add(XMLMottakenhetBlokk().apply {
        ebAction = "Plan"
        ebRole = "Saksbehandler"
        ebService = "Oppfolgingsplan"
        partnerReferanse = canReceiveDialogMessage.partnerID
    })
}

fun createArenaBrevdata(): Brevdata = Brevdata().apply {
    // TODO this is only TMP
    content.add(JAXBElement(QName("felles"), FellesType::class.java, FellesType().apply {
                spraakkode = "NB"
                fagsaksnummer = "2014122950"
                signerendeSaksbehandler = SignerendeSaksbehandlerType().apply {
                    signerendeSaksbehandlerNavn = "Sagne Sakbehandler"
                }
                sakspart = SakspartType().apply {
                    sakspartId = "01010112345".toLong()
                    sakspartTypeKode = SakspartTypeKode.PERSON
                    sakspartNavn = "Liv Mona Olsen"
                }
                mottaker = MottakerType().apply {
                    mottakerId = "01010112345".toLong()
                    mottakerTypeKode = MottakerTypeKode.PERSON
                    mottakerNavn = "Liv Mona Olsen"
                    mottakerAdresse = MottakerAdresseType().apply {
                        adresselinje1 = "Rolfsbuktalleen 7"
                        adresselinje2 = "Oslo"
                        adresselinje3 = "Moss"
                        postNr = 1364
                        poststed = "FORNEBU"
                        land = "Norge"
                    }
                }
                navnAvsenderEnhet = "Dagpenger Inn"
                kontaktInformasjon = KontaktInformasjonType().apply {
                    kontaktTelefonnummer = "55 55 33 33"
                    returadresse = ReturadresseType().apply {
                        navEnhetsNavn = "Dagpenger Inn"
                        adresselinje = "Postboks 6944 St.Olavs plass"
                        postNr = "0130".toShort()
                        poststed = "OSLO"
                    }
                    postadresse = PostadresseType().apply {
                        navEnhetsNavn = "Dagpenger Inn"
                        adresselinje = "Postboks 6944 St.Olavs plass"
                        postNr = "0130".toShort()
                        poststed = "OSLO"
                    }
                    besoksadresse = BesoksadresseType().apply {
                        adresselinje = "Adresselinje"
                        postNr = "0130"
                        poststed = "OSLO"
                    }
                }
            }))

    content.add(JAXBElement(QName("fag"), FagType::class.java, FagType().apply {
        aktivitetsNavn = "aktivitet"
        aktivitetsType = AktivitetsType.VGINT
        isSvarslipp = true
        moteInfo = MoteInfoType().apply {
            moteKontakt = "Kurt Kursholder"
            dato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
            klokkeslett = "10:59"
            sted = "Oslo"
            fristDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
            brevTekst = "Brevtekst"
        }
        isVisReaksjon = true
    }))
}

fun createAltinnMessage(
    iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    archiveReference: String,
    orgnummer: String,
    fagmelding: ByteArray,
    altinnUserUsername: String,
    altinnUserPassword: String
) {
    iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
            altinnUserUsername,
            altinnUserPassword,
            "NAV_DIGISYFO",
            archiveReference,
            createInsertCorrespondenceV2(orgnummer, archiveReference, fagmelding)
            )
}

fun createInsertCorrespondenceV2(
    orgnummer: String,
    archiveReferenceIncoming: String,
    fagmelding: ByteArray
): InsertCorrespondenceV2 = InsertCorrespondenceV2().apply {
    isAllowForwarding = true
    reportee = orgnummer
    messageSender = "brukersNavn-fnr"
    serviceCode = "5062"
    serviceEdition = "1"
    content = createExternalContentV2(archiveReference, fagmelding)
    archiveReference = archiveReferenceIncoming
}

fun createExternalContentV2(
    archiveReference: String,
    fagmelding: ByteArray
): ExternalContentV2 = ExternalContentV2().apply {
    languageCode = "1044"
    messageTitle = "Oppfølgingsplan-$archiveReference-brukersNavn(fnr)"
    customMessageData = null
    attachments = createAttachmentsV2(archiveReference, fagmelding)
}

fun createAttachmentsV2(archiveReference: String, fagmelding: ByteArray): AttachmentsV2 = AttachmentsV2().apply {
    binaryAttachments = BinaryAttachmentExternalBEV2List().apply {
        binaryAttachmentV2.add(BinaryAttachmentV2().apply {
                destinationType = UserTypeRestriction.SHOW_TO_ALL
                fileName = "oppfoelgingsdialog.pdf" //
                name = "oppfoelgingsdialog"
                functionType = AttachmentFunctionType.UNSPECIFIED
                isEncrypted = false
                sendersReference = archiveReference
                data = fagmelding
        })
    }
}
