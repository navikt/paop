package no.nav.paop

enum class Oppfolginsplan(val serviceCode: String, val ServiceEdition: String) {
    OP2012("2913", "2"),
    OP2014("2913", "3"),
    OP2016("2913", "4"),
    TEST("9001", "1"), // TODO slett denne etter testing
    NAVOPPFPLAN("NavOppfPlan", "rapportering-sykmeldte")
}

fun findOppfolingsplanType(serviceCode: String, serviceEditionCode: String): Oppfolginsplan {
    return Oppfolginsplan.values().first { it.serviceCode == serviceCode && it.ServiceEdition == serviceEditionCode }
}
