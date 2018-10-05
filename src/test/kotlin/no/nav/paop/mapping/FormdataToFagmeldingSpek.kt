package no.nav.paop.mapping

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.model.oppfolgingsplan2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.paop.extractDataBatch
import no.nav.paop.getFileAsString
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.xmlMapper
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object FormdataToFagmeldingSpek : Spek({
    describe("mappes nokkelopplysninger") {
        val dataBatch = extractDataBatch(getFileAsString(
                "src/test/resources/oppfolging_2913_04.xml"))
        val formData = dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData

        val skjemainnhold = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(formData).skjemainnhold

        val incomingMetadata = IncomingMetadata(
                archiveReference = dataBatch.dataUnits.dataUnit.first().archiveReference,
                senderOrgName = skjemainnhold.arbeidsgiver.orgnavn,
                senderOrgId = skjemainnhold.arbeidsgiver.orgnr,
                senderSystemName = skjemainnhold.avsenderSystem.systemNavn,
                senderSystemVersion = skjemainnhold.avsenderSystem.systemVersjon,
                userPersonNumber = skjemainnhold.sykmeldtArbeidstaker.fnr
        )

        it("Won't cause an exception") {
            val fagmelding = mapFormdataToFagmelding(skjemainnhold, incomingMetadata)
            fagmelding.nokkelopplysninger.organiasjonsnr shouldEqual "987654321"
        }
    }
})
