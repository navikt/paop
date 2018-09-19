package no.nav.paop

import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime
import java.time.Month

object PaopApplicationTest : Spek({

    describe("tests the functions extractDataBatch") {

        it("Should set databatch.schemaVersion") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.schemaVersion shouldEqual 1.0.toBigDecimal()
        }
        it("Should set databatch.attachments") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.attachments shouldEqual null
        }
        it("Should set databatch.batchReference") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.batchReference shouldEqual 85385
        }
        it("Should set databatch.dataUnits.dataUnit[0].reportee") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().reportee shouldEqual "923475230"
        }
        it("Should set databatch.dataUnits.dataUnit[0].archiveReference") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().archiveReference shouldEqual "AR387469726"
        }
        it("Should set databatch.dataUnits.dataUnit[0].archiveTimeStamp") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            val expectedDate = LocalDateTime.of(2017, Month.MARCH, 14, 12, 52)
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.year shouldEqual expectedDate.year
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.month shouldEqual expectedDate.month.value
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.day shouldEqual expectedDate.dayOfMonth
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.hour shouldEqual expectedDate.hour
            dataBatch.dataUnits.dataUnit.first().archiveTimeStamp.minute shouldEqual expectedDate.minute
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.serviceCode") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.serviceCode shouldEqual "2913"
        }
        it("Should set databatch.dataUnits.dataUnit[0]formTask.serviceEditionCode") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.serviceEditionCode shouldEqual "4"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].dataFormatId") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().dataFormatId shouldEqual "3521"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].dataFormatVersion") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().dataFormatVersion shouldEqual "37865"
        }
        it("Should set databatch.dataUnits.dataUnit[0].formTask.form[0].reference") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77423963"
        }
    }

    describe("tests the functions extractOppfolginsplan2016") {
        it("Should set dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData to OP2016") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_04.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77423963"

            val op2016 = extractOppfolginsplan2016(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            op2016.skjemainnhold.arbeidsgiver.value.annenKontaktpersonEtternavn.value shouldEqual "Etternavn"
        }
    }

    describe("tests the functions extractOppfolginsplan2014") {
        it("Should set dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData to OP2014") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_03.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77423963"

            val op2014 = extractOppfolginsplan2014(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            op2014.skjemainnhold.arbeidsgiver.value.annenKontaktpersonEtternavn.value shouldEqual "Navnesen"
        }
    }

    describe("tests the functions extractOppfolginsplan2012") {
        it("Should set dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData to OP2012") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_02.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77426094"

            val op2012 = extractOppfolginsplan2012(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            op2012.skjemainnhold.arbeidsgiver.value.annenKontaktpersonEtternavn.value shouldEqual "Navnesen"
        }
    }

    describe("tests the functions extractNavOppfPlan") {
        it("Should set dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData to NavOppfPlan") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_navoppfplan_rapportering_sykemeldte.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "rapportering-sykmeldte"

            val navOppfPlan = extractNavOppfPlan(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            navOppfPlan.fodselsNr shouldEqual "01010112345"
        }
    }

    describe("tests the functions findOppfolingsplanType") {
        it("Should find the type of oppfolginsplan") {

            val oppfolginsplanType = findOppfolingsplanType("2913", "2")

            oppfolginsplanType shouldEqual Oppfolginsplan.OP2012
        }
    }
})
