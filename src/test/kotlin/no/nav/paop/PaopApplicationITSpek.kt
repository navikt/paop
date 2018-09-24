package no.nav.paop

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PaopApplicationITSpek : Spek({
    val e = EmbeddedEnvironment()
    beforeGroup { e.start() }
    afterEachTest {
        e.resetMocks()
    }
    afterGroup {
        e.shutdown()
    }
    describe("Full flow exception") {
        it("") {
            val message = ""
            e.produceMessage(message)
        }
    }
})