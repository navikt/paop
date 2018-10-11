package no.nav.paop

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.model.dataBatch.DataBatch
import no.nav.model.oppfolgingsplan2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.paop.routes.xmlMapper
import org.amshove.kluent.shouldNotEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object JaxbAnnotationSpek : Spek({

    describe("Can unmarshal every type of altinn oppfølgingsplan with Jackson") {
        for (file in arrayOf("/oppfolging_2913_02.xml", "/oppfolging_2913_03.xml", "/oppfolging_2913_04.xml")) {
            val serviceCode = file.substring(12..15)
            val serviceEditionCode = file.substring(17..18)
            it("Can unmarshal the oppfølgingplan with service code: $serviceCode and service edition code: $serviceEditionCode") {
                val dataBatch = dataBatchUnmarshaller.unmarshal(JaxbAnnotationSpek::class.java.getResourceAsStream("/oppfolging_2913_03.xml")) as DataBatch

                val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData
                println(formData)
                val oppfolginsplan = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(formData)
                oppfolginsplan shouldNotEqual null
            }
        }
    }
})
