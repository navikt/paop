package no.nav.paop

import no.nav.paop.client.createJoarkRequest
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.Arrays

object JoarkClientTest : Spek({
    describe("tests the functions of JoarkCleint") {
        it("Should set databatch.schemaVersion") {

            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))

            val formdata = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
            val oppfolgingslplanType = Oppfolginsplan.OP2016
            val edilogg = "1231413424.1"
            val fagmelding = ByteArray(100)
            Arrays.fill(fagmelding, 1.toByte())

            val joarkRequest = createJoarkRequest(formdata, oppfolgingslplanType, edilogg, fagmelding)

            joarkRequest.avsenderMottakerId shouldEqual "987654321"
        }
    }
})