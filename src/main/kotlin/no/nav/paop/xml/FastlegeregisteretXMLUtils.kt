package no.nav.paop.xml

import no.nhn.schemas.reg.flr.PatientToGPContractAssociation

fun PatientToGPContractAssociation.extractGPName() =
        "${extractGPFirstName()} ${extractGPMiddleName()} ${extractGPLastName()}"

fun PatientToGPContractAssociation.extractGPFirstName(): String? =
        this.doctorCycles.gpOnContractAssociation.first().gp.firstName

fun PatientToGPContractAssociation.extractGPLastName(): String? =
        this.doctorCycles.gpOnContractAssociation.first().gp.lastName

fun PatientToGPContractAssociation.extractGPMiddleName(): String? =
        this.doctorCycles.gpOnContractAssociation.first().gp.middleName

fun PatientToGPContractAssociation.extractGPFnr(): String =
        this.doctorCycles.gpOnContractAssociation.first().gp.nin

fun PatientToGPContractAssociation.extractGPHprNumber(): Int =
        this.doctorCycles.gpOnContractAssociation.first().hprNumber

fun PatientToGPContractAssociation.extractGPOfficePostalCode(): String =
        this.gpContract.gpOffice.physicalAddresses.physicalAddress.first().postalCode.toString()

fun PatientToGPContractAssociation.extractGPOfficePhysicalAddress(): String =
        this.gpContract.gpOffice.physicalAddresses.physicalAddress.first().city
