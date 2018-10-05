package no.nav.paop.model

data class IncomingMetadata(
    val archiveReference: String,
    val senderOrgId: String,
    val senderOrgName: String,
    val senderSystemName: String,
    val senderSystemVersion: String,
    val userPersonNumber: String
)

data class IncomingUserInfo(
    val userFamilyName: String?,
    val userGivenName: String?,
    val userPersonNumber: String
)

data class ArenaBistand(
    val bistandNavHjelpemidler: Boolean,
    val bistandNavVeiledning: Boolean,
    val bistandDialogmote: Boolean,
    val bistandVirkemidler: Boolean
)
