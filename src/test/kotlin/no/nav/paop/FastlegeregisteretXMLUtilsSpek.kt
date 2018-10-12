package no.nav.paop

import no.nav.paop.xml.extractGPFirstName
import no.nav.paop.xml.extractGPFnr
import no.nav.paop.xml.extractGPHprNumber
import no.nav.paop.xml.extractGPLastName
import no.nav.paop.xml.extractGPMiddleName
import no.nav.paop.xml.extractGPName
import no.nav.paop.xml.extractGPOfficePhysicalAddress
import no.nav.paop.xml.extractGPOfficePostalCode
import no.nhn.register.commontypes.ArrayOfPhysicalAddress
import no.nhn.schemas.reg.flr.ArrayOfGPOnContractAssociation
import no.nhn.schemas.reg.flr.GPContract
import no.nhn.schemas.reg.flr.GPOffice
import no.nhn.schemas.reg.flr.GPOnContractAssociation
import no.nhn.schemas.reg.flr.PatientToGPContractAssociation
import no.nhn.schemas.reg.flr.Person
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object FastlegeregisteretXMLUtilsSpek : Spek({

    val patientToGPContractAssociationMockResponse = createPatientToGPContractAssociation("Per",
            "Ove", "Hansen", "01010112345", 1235323, 1665, "Oslo")

    describe("tests the functions in FastlegeregisteretXMLUtilsSpek") {
        it("Should extract GPName to String object") {
            val firstname = "Per"
            val middelname = "Ove"
            val lastname = "Hansen"
            val gpname = "$firstname $middelname $lastname"

            val patientToGPContractAssociationExtractGPName = patientToGPContractAssociationMockResponse.extractGPName()

            gpname shouldBeEqualTo patientToGPContractAssociationExtractGPName
        }

        it("Should extract GPFirstName to String object") {
            val middelname = "Ove"

            val patientToGPContractAssociationExtractGPMiddleName = patientToGPContractAssociationMockResponse.extractGPMiddleName()

            middelname.equals(patientToGPContractAssociationExtractGPMiddleName) `should be equal to` true
        }

        it("Should extract GPFirstName to String object") {
            val firstname = "Per"

            val patientToGPContractAssociationExtractGPName = patientToGPContractAssociationMockResponse.extractGPFirstName()

            firstname.equals(patientToGPContractAssociationExtractGPName) `should be equal to` true
        }

        it("Should extract GPFirstName to String object") {
            val lastname = "Hansen"

            val patientToGPContractAssociationExtractGPLastName = patientToGPContractAssociationMockResponse.extractGPLastName()

            lastname.equals(patientToGPContractAssociationExtractGPLastName) `should be equal to` true
        }

        it("Should extract GPFirstName to String object") {
            val fnr = "01010112345"

            val patientToGPContractAssociationExtractGPFnr = patientToGPContractAssociationMockResponse.extractGPFnr()
            fnr.equals(patientToGPContractAssociationExtractGPFnr) `should be equal to` true
        }

        it("Should extract GPHprNumber to String object") {
            val hprNumber = 1235323

            val patientToGPContractAssociationExtractGPHprNumber = patientToGPContractAssociationMockResponse.extractGPHprNumber()
            hprNumber.equals(patientToGPContractAssociationExtractGPHprNumber) `should be equal to` true
        }

        it("Should extract GPOfficePostalCode to String object") {
            val gpOfficePostalCode = "1665"

            val patientToGPContractAssociationextractGPOfficePostalCode = patientToGPContractAssociationMockResponse.extractGPOfficePostalCode()
            gpOfficePostalCode.equals(patientToGPContractAssociationextractGPOfficePostalCode) `should be equal to` true
        }

        it("Should extract extractGPOfficePhysicalAddress to String object") {
            val city = "Oslo"

            val patientToGPContractAssociationextractGPOfficePhysicalAddress = patientToGPContractAssociationMockResponse.extractGPOfficePhysicalAddress()
            city.equals(patientToGPContractAssociationextractGPOfficePhysicalAddress) `should be equal to` true
        }
    }
})

fun createPatientToGPContractAssociation(firstname: String, middelname: String, lastname: String, fnr: String, hprnumber: Int, postalcode: Int, cityString: String): PatientToGPContractAssociation = PatientToGPContractAssociation().apply {
    doctorCycles = ArrayOfGPOnContractAssociation().apply {
        gpOnContractAssociation.add(
                GPOnContractAssociation().apply {
                    gp = Person().apply {
                        firstName = firstname
                        middleName = middelname
                        lastName = lastname
                        nin = fnr
                    }
                    hprNumber = hprnumber
                }
        )
    }
    gpContract = GPContract().apply {
        gpOffice = GPOffice().apply {
            physicalAddresses = ArrayOfPhysicalAddress().apply {
                physicalAddress.add(no.nhn.register.commontypes.PhysicalAddress().apply {
                    postalCode = postalcode
                    city = cityString
                })
            }
        }
    }
}