package no.nav.paop.client

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.UserTypeRestriction
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2
import java.util.UUID

fun createAltinnMessage(
    iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    incomingArchRef: String,
    orgnummer: String,
    fagmelding: ByteArray,
    altinnUserUsername: String,
    altinnUserPassword: String
) {
    val insertCorrespondenceV2 = InsertCorrespondenceV2().apply {
        isAllowForwarding = true
        // TODO after test stage put back inn reportee = orgnummer
        reportee = "910067494"
        messageSender = "brukersNavn-fnr"
        serviceCode = "5062"
        serviceEdition = "1"
        content = ExternalContentV2().apply {
            languageCode = "1044"
            messageTitle = "Oppfølgingsplan-$archiveReference-brukersNavn(fnr)"
            customMessageData = null
            attachments = AttachmentsV2().apply {
                binaryAttachments = BinaryAttachmentExternalBEV2List().apply {
                    binaryAttachmentV2.add(BinaryAttachmentV2().apply {
                        destinationType = UserTypeRestriction.SHOW_TO_ALL
                        fileName = "oppfoelgingsdialog.pdf" //
                        name = "oppfoelgingsdialog"
                        functionType = AttachmentFunctionType.UNSPECIFIED
                        isEncrypted = false
                        sendersReference = UUID.randomUUID().toString()
                        data = fagmelding
                    })
                }
            }
        }
        archiveReference = null
    }

    iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(altinnUserUsername, altinnUserPassword,
            "NAV_DIGISYFO", incomingArchRef, insertCorrespondenceV2)
}
