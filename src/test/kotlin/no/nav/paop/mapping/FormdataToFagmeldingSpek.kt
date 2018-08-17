package no.nav.paop.mapping

import no.nav.paop.Oppfolginsplan
import no.nav.paop.extractDataBatch
import no.nav.paop.getFileAsString
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object FormdataToFagmeldingSpek : Spek({
    describe("mappes nokkelopplysninger") {
        val dataBatch = extractDataBatch(getFileAsString(
                "src/test/resources/oppfolging_2913_04.xml"))
        val formdata = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
        it("Won't cause an exception") {
            val fagmelding = mapFormdataToFagmelding(formdata, Oppfolginsplan.OP2016)
        }
    }
})