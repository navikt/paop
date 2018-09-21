package no.nav.paop

import org.spekframework.spek2.Spek


object PaopApplicationITSpek : Spek({
    val e = EmbeddedEnvironment()

    beforeGroup { e.start() }
    afterEachTest { e.resetMocks() }
    afterGroup { e.shutdown() }

})
