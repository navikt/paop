package no.nav.paop

import com.fasterxml.jackson.module.kotlin.readValue
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.emottak.schemas.PartnerInformasjon
import no.nav.model.oppfolgingsplan2016.Oppfoelgingsplan4UtfyllendeInfoM
import no.nav.paop.model.IncomingMetadata
import no.nav.paop.model.IncomingUserInfo
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.ValiderOrganisasjonOrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.ValiderOrganisasjonUgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v4.feil.OrganisasjonIkkeFunnet
import no.nav.tjeneste.virksomhet.organisasjon.v4.feil.UgyldigInput
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonRequest
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.ValiderOrganisasjonResponse
import org.amshove.kluent.shouldEqual
import org.mockito.Mockito
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

            val op2016 = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            op2016.skjemainnhold.arbeidsgiver.annenKontaktpersonEtternavn shouldEqual "Etternavn"
        }
    }

    describe("tests the functions extractOppfolginsplan2014") {
        it("Should set dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData to OP2014") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_03.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77423963"

            val op2014 = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            op2014.skjemainnhold.arbeidsgiver.annenKontaktpersonEtternavn shouldEqual "Navnesen"
        }
    }

    describe("tests the functions extractOppfolginsplan2012") {
        it("Should set dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData to OP2012") {
            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_02.xml"))
            dataBatch.dataUnits.dataUnit.first().formTask.form.first().reference shouldEqual "77426094"

            val op2012 = xmlMapper.readValue<Oppfoelgingsplan4UtfyllendeInfoM>(
                    dataBatch.dataUnits.dataUnit.first().formTask.form.first().formData)

            op2012.skjemainnhold.arbeidsgiver.annenKontaktpersonEtternavn shouldEqual "Navnesen"
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

    describe("tests the functions sendDialogmeldingOppfolginsplan") {
        it("Should send a sendDialogmeldingOppfolginsplan") {

            val dataBatch = extractDataBatch(getFileAsString(
                    "src/test/resources/oppfolging_2913_02.xml"))

            val serviceCode = "2913"
            val serviceEditionCode = "2"
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

            val incomingPersonInfo = IncomingUserInfo(
                    userFamilyName = skjemainnhold.sykmeldtArbeidstaker?.fornavn,
                    userGivenName = skjemainnhold.sykmeldtArbeidstaker?.etternavn,
                    userPersonNumber = skjemainnhold.sykmeldtArbeidstaker.fnr
            )

            val gpOfficeOrganizationName = "Kule helsetjenester AS"
            val gpOffcieOrganizationNumber = "223456789"
            val gpFirstName = "Per"
            val gpMiddleName = "Ole"
            val gpLastName = "Hansen"
            val gpFnr = "21345455"
            val gpHprNumber = 453456
            val gpHerIdFlr = 453456

            val herIDAdresseregister = 1234566
            val fagmelding: ByteArray = listOf(0xDE, 0xAD, 0xBE, 0xEF).map { it.toByte() }.toByteArray()
            val canReceiveDialogMessage = PartnerInformasjon().apply {
                partnerID = "3245363"
            }

            val fellesformatDialogmelding = createDialogmelding(
                    incomingMetadata,
                    incomingPersonInfo,
                    gpOfficeOrganizationName,
                    gpOffcieOrganizationNumber,
                    herIDAdresseregister,
                    fagmelding,
                    canReceiveDialogMessage,
                    gpFirstName,
                    gpMiddleName,
                    gpLastName,
                    gpHerIdFlr,
                    gpFnr,
                    gpHprNumber)

            val msgHead = fellesformatDialogmelding.any.first() as XMLMsgHead

            msgHead.msgInfo.type.dn shouldEqual "Notat"
            msgHead.msgInfo.type.v shouldEqual "DIALOG_NOTAT"
        }
    }
    describe("tests the call to validerOrganisasjon should throw ValiderOrganisasjonOrganisasjonIkkeFunnet") {
        it("Should try to validate the orgnummer and throw ValiderOrganisasjonOrganisasjonIkkeFunnet") {

            val organisasjonV4Mock: OrganisasjonV4 = Mockito.mock(OrganisasjonV4::class.java)

            val validerOrganisasjonRequestOrgIkkeFunnet = ValiderOrganisasjonRequest().apply {
                orgnummer = "973123453"
            }

            Mockito.`when`(organisasjonV4Mock.validerOrganisasjon(validerOrganisasjonRequestOrgIkkeFunnet))
                    .thenThrow(ValiderOrganisasjonOrganisasjonIkkeFunnet("Organisasjon ikke funnet", OrganisasjonIkkeFunnet()))

            val validOrganizationNumber = try {
                organisasjonV4Mock.validerOrganisasjon(validerOrganisasjonRequestOrgIkkeFunnet).isGyldigOrgnummer
            } catch (e: Exception) {
                log.error("Failed to validate organization number due to an exception", e)
                false
            }

            validOrganizationNumber shouldEqual false
        }
    }

    describe("tests the call to validerOrganisasjon and the exception ValiderOrganisasjonUgyldigInput") {
        it("Should throw ValiderOrganisasjonUgyldigInput") {

            val organisasjonV4Mock: OrganisasjonV4 = Mockito.mock(OrganisasjonV4::class.java)

            val validerOrganisasjonRequestUgyldigInput = ValiderOrganisasjonRequest().apply {
                orgnummer = "werwrwrwr"
            }

            Mockito.`when`(organisasjonV4Mock.validerOrganisasjon(validerOrganisasjonRequestUgyldigInput))
                    .thenThrow(ValiderOrganisasjonUgyldigInput("Organisasjon ugyldig input", UgyldigInput()))

            val validOrganizationNumber = try {
                organisasjonV4Mock.validerOrganisasjon(validerOrganisasjonRequestUgyldigInput).isGyldigOrgnummer
            } catch (e: Exception) {
                log.error("Failed to validate organization number due to an exception", e)
                false
            }

            validOrganizationNumber shouldEqual false
        }
    }

    describe("tests the call to validerOrganisasjon") {
        it("Should validate OK") {

            val organisasjonV4Mock: OrganisasjonV4 = Mockito.mock(OrganisasjonV4::class.java)

            val validerOrganisasjonRequest = ValiderOrganisasjonRequest().apply {
                orgnummer = "973123456"
            }

            val validerOrganisasjonResponse = ValiderOrganisasjonResponse().apply {
                isGyldigOrgnummer = true
            }

            Mockito.`when`(organisasjonV4Mock.validerOrganisasjon(validerOrganisasjonRequest)).thenReturn(validerOrganisasjonResponse)

            val validOrganizationNumber = try {
                organisasjonV4Mock.validerOrganisasjon(validerOrganisasjonRequest).isGyldigOrgnummer
            } catch (e: Exception) {
                log.error("Failed to validate organization number due to an exception", e)
                false
            }

            validOrganizationNumber shouldEqual true
        }
    }
})
