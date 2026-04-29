package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.model.FlightLandingModel
import eu.profinit.flightlog.operation.LandOperation
import eu.profinit.flightlog.repository.FlightRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.OffsetDateTime
import java.time.ZoneOffset

class LandOperationKotestTests : DescribeSpec({

    describe("LandOperation.execute") {
        it("converts UTC landing time to system zone and delegates to repository") {
            val flightRepository = mockk<FlightRepository>(relaxed = true)
            val operation = LandOperation(flightRepository)

            val zuluTime = OffsetDateTime.of(2026, 4, 28, 12, 0, 0, 0, ZoneOffset.UTC)
            val model = FlightLandingModel(flightId = 42L, landingTime = zuluTime)

            val captured = slot<FlightLandingModel>()
            operation.execute(model)

            verify(exactly = 1) { flightRepository.landFlight(capture(captured)) }
            captured.captured.flightId shouldBe 42L
            // Same instant, possibly different offset depending on JVM TZ:
            captured.captured.landingTime.toInstant() shouldBe zuluTime.toInstant()
        }
    }
})
