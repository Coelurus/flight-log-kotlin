package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.integration.DegradedModeHolder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DegradedModeHolderKotestTests : DescribeSpec({
    describe("DegradedModeHolder") {
        it("starts not degraded") {
            val h = DegradedModeHolder()
            h.degraded shouldBe false
            (h.message == null) shouldBe true
        }
        it("captures degraded state with default message") {
            val h = DegradedModeHolder()
            h.mark()
            h.degraded shouldBe true
            h.message shouldBe "Data nemusí být aktuální"
        }
        it("accepts a custom message") {
            val h = DegradedModeHolder()
            h.mark("offline")
            h.message shouldBe "offline"
        }
    }
})
