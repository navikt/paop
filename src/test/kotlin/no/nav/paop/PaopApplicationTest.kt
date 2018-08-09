package no.nav.paop

import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime
import java.time.Month

object PaopApplicationTest : Spek({

    describe("tests the functions in PaopApplication") {

        it("Should set databatch.schemaVersion") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.schemaVersion shouldEqual 1.0.toBigDecimal()
        }
        it("Should set databatch.attachments") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.attachments shouldEqual null
        }
        it("Should set databatch.batchReference") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.batchReference shouldEqual 85385
        }
        it("Should set databatch.dataUnits.dataUnit[0].reportee") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().reportee shouldEqual "923475230"
        }
        it("Should set databatch.dataUnits.dataUnit[0].archiveReference") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().archiveReference shouldEqual "AR387469726"
        }
        it("Should set databatch.dataUnits.dataUnit[0].archiveTimeStamp") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            val expectedDate = LocalDateTime.of(2017, Month.MARCH, 14, 12, 52)
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.year shouldEqual expectedDate.year
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.month shouldEqual expectedDate.month.value
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.day shouldEqual expectedDate.dayOfMonth
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.hour shouldEqual expectedDate.hour
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.minute shouldEqual expectedDate.minute
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.serviceCode") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.serviceCode shouldEqual "2913"
        }
        it("Should set databatch.dataUnits.dataUnit[0]formTask.serviceEditionCode") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.serviceEditionCode shouldEqual "4"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].dataFormatId") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().dataFormatId shouldEqual "3521"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].dataFormatVersion") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().dataFormatVersion shouldEqual "37865"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].reference") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77423963"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].parentReference") {
            val dataBatch = extractDataBatchFromString(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().parentReference shouldEqual "0"
        }
    }
})