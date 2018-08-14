package no.nav.paop.client

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.paop.objectMapper
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDateTime
val log: Logger = LoggerFactory.getLogger(SarClient::class.java)
class SarClient(private val url: String, private val username: String, private val password: String) {
    private val client: OkHttpClient = OkHttpClient()
    fun getSamhandler(ident: String): List<Samhandler> {
        val request = Request.Builder()
                .get()
                .header("Authorization", Credentials.basic(username, password))
                .url(HttpUrl.parse(url)!!
                        .newBuilder()
                        .addPathSegments("/rest/sar/samh")
                        .addQueryParameter("ident", ident)
                        .build()
                )
                .build()
        val response = client.newCall(request)
                .execute()
        if (response.isSuccessful) {
            return objectMapper.readValue(response.body()?.byteStream(), Array<Samhandler>::class.java).toList()
        } else {
            log.error("Received an error while contacting SAR {}", keyValue("errorBody", response.body()?.string()))
            throw IOException("Unable to contact SAR, got status code ${response.code()}")
        }
    }
}
data class Samhandler(
    val samh_id: String,
    val navn: String,
    val samh_type_kode: String,
    val behandling_utfall_kode: String,
    val unntatt_veiledning: String,
    val godkjent_manuell_krav: String,
    val ikke_godkjent_for_refusjon: String,
    val godkjent_egenandel_refusjon: String,
    val godkjent_for_fil: String,
    val breg_hovedenhet: SamhandlerBregHovedenhet?,
    val endringslogg_tidspunkt_siste: LocalDateTime?,
    val samh_ident: List<SamhandlerIdent>,
    val samh_praksis: List<SamhandlerPraksis>,
    val samh_avtale: List<SamhandlerAvtale>,
    val samh_direkte_oppgjor_avtale: List<SamhandlerDirekteOppgjoerAvtale>,
    val samh_email: List<SamhEmail>?
)
data class SamhandlerBregHovedenhet(
    val organisasjonsnummer: String,
    val organisasjonsform: String,
    val institusjonellsektorkodekode: String,
    val naeringskode1kode: String,
    val naeringskode2kode: String?
)
data class SamhandlerIdent(
    val samh_id: String,
    val samh_ident_id: String,
    val ident: String,
    val ident_type_kode: String,
    val aktiv_ident: String
)
data class SamhandlerPraksis(
    val refusjon_type_kode: String,
    val laerer: String,
    val lege_i_spesialisering: String,
    val tidspunkt_resync_periode: LocalDateTime,
    val tidspunkt_registrert: LocalDateTime,
    val samh_praksis_status_kode: String,
    val telefonnr: String?,
    val arbeids_kommune_nr: String,
    val arbeids_postnr: String,
    val arbeids_adresse_linje_1: String?,
    val arbeids_adresse_linje_2: String?,
    val arbeids_adresse_linje_3: String?,
    val arbeids_adresse_linje_4: String?,
    val arbeids_adresse_linje_5: String?,
    val her_id: String?,
    val post_adresse_linje_1: String?,
    val post_adresse_linje_2: String?,
    val post_adresse_linje_3: String?,
    val post_adresse_linje_4: String?,
    val post_adresse_linje_5: String?,
    val post_kommune_nr: String?,
    val post_postnr: String?,
    val resh_id: String?,
    val tss_ident: String,
    val navn: String?,
    val ident: String,
    val samh_praksis_type_kode: String?,
    val samh_id: String,
    val samh_praksis_id: String,
    val samh_praksis_konto: List<SamhandlerPraksisKonto>,
    val samh_praksis_periode: List<SamhandlerPeriode>,
    val samh_praksis_email: List<SamhandlerPraksisEmail>?
)
data class SamhandlerPraksisKonto(
    val tidspunkt_registrert: LocalDateTime,
    val registrert_av_id: String,
    val konto: String,
    val samh_praksis_id: String,
    val samh_praksis_konto_id: String
)
data class SamhandlerPeriode(
    val endret_ved_import: String,
    val sist_endret: LocalDateTime,
    val slettet: String,
    val gyldig_fra: LocalDateTime,
    val gyldig_til: LocalDateTime?,
    val samh_praksis_id: String,
    val samh_praksis_periode_id: String
)
data class SamhandlerAvtale(
    val gyldig_fra: LocalDateTime,
    val gyldig_til: LocalDateTime?,
    val prosentandel: String,
    val avtale_type_kode: String,
    val samh_id: String,
    val samh_avtale_id: String
)
data class SamhandlerDirekteOppgjoerAvtale(
    val gyldig_fra: LocalDateTime,
    val koll_avtale_mottatt_dato: LocalDateTime?,
    val monster_avtale_mottatt_dato: LocalDateTime?,
    val samh_id: String,
    val samh_direkte_oppgjor_avtale_id: String
)
data class SamhandlerPraksisEmail(
    val samh_praksis_email_id: String,
    val samh_praksis_id: String,
    val email: String,
    val primaer_email: String?
)
data class SamhEmail(
    val samh_email_id: String,
    val samh_id: String,
    val email: String,
    val primaer_email: String,
    val email_type_kode: String
)