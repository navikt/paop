package no.nav.paop

import no.nav.paop.client.createJoarkRequest
import no.nav.paop.model.IncomingMetadata
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object JoarkClientTest : Spek({
    describe("tests the functions of JoarkCleint") {
        it("Should set databatch.schemaVersion") {

            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))

            val incomingMetadata = IncomingMetadata(
                    archiveReference = "AR198273",
                    senderOrgId = "1238719290",
                    senderOrgName = "Et selskap AS",
                    senderSystemName = "Spek HR system",
                    senderSystemVersion = "Whoa",
                    userPersonNumber = "987654321"

            )

            val fagmelding = "<TEST></TEST>".toByteArray(Charsets.UTF_8)

            val joarkRequest = createJoarkRequest(incomingMetadata, fagmelding)

            joarkRequest.avsenderMottakerId shouldEqual "1238719290"
        }
    }
})
