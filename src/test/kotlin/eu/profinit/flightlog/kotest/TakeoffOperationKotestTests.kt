package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.model.AirplaneModel
import eu.profinit.flightlog.model.AirplaneWithCrewModel
import eu.profinit.flightlog.model.FlightTakeOffModel
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.operation.CreatePersonOperation
import eu.profinit.flightlog.operation.TakeoffOperation
import eu.profinit.flightlog.repository.AirplaneRepository
import eu.profinit.flightlog.repository.FlightRepository
import eu.profinit.flightlog.repository.TryGetResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TakeoffOperationKotestTests : DescribeSpec({

    fun fixtures(): Triple<FlightRepository, AirplaneRepository, CreatePersonOperation> =
        Triple(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    describe("TakeoffOperation.execute") {
        it("throws when towplane is missing") {
            val (flightRepo, airplaneRepo, personOp) = fixtures()
            val op = TakeoffOperation(flightRepo, airplaneRepo, personOp)

            val takeoff = FlightTakeOffModel(
                takeoffTime = OffsetDateTime.of(2026, 4, 28, 10, 0, 0, 0, ZoneOffset.UTC),
                task = "x",
                towplane = null,
                glider = null,
            )
            shouldThrow<UnsupportedOperationException> { op.execute(takeoff) }
        }

        it("creates towplane and glider flights and links them") {
            val (flightRepo, airplaneRepo, personOp) = fixtures()
            val op = TakeoffOperation(flightRepo, airplaneRepo, personOp)

            val tow = AirplaneWithCrewModel(
                airplane = AirplaneModel(id = 11, immatriculation = "OK-TOW", type = "Z-126"),
                pilot = PersonModel(memberId = 1, firstName = "T", lastName = "Pilot"),
            )
            val glider = AirplaneWithCrewModel(
                airplane = AirplaneModel(id = 22, immatriculation = "OK-GLD", type = "L-23"),
                pilot = PersonModel(memberId = 2, firstName = "G", lastName = "Pilot"),
            )
            val takeoff = FlightTakeOffModel(
                takeoffTime = OffsetDateTime.of(2026, 4, 28, 10, 0, 0, 0, ZoneOffset.UTC),
                task = "training",
                towplane = tow,
                glider = glider,
            )

            every { personOp.execute(tow.pilot) } returns 100L
            every { personOp.execute(glider.pilot) } returns 200L
            every { personOp.execute(null) } returns null
            every { airplaneRepo.tryGetAirplane(tow.airplane!!) } returns TryGetResult(true, 11L)
            every { airplaneRepo.tryGetAirplane(glider.airplane!!) } returns TryGetResult(true, 22L)
            every { flightRepo.createFlight(any()) } returnsMany listOf(1L, 2L)

            op.execute(takeoff)

            verify(exactly = 2) { flightRepo.createFlight(any()) }
            verify(exactly = 1) { flightRepo.takeoffFlight(any(), any()) }
        }

        it("throws when an unknown (non-guest) airplane is supplied") {
            val (flightRepo, airplaneRepo, personOp) = fixtures()
            val op = TakeoffOperation(flightRepo, airplaneRepo, personOp)

            val tow = AirplaneWithCrewModel(
                airplane = AirplaneModel(id = 999, immatriculation = "OK-???", type = "?"),
                pilot = PersonModel(memberId = 1, firstName = "T", lastName = "Pilot"),
            )
            val takeoff = FlightTakeOffModel(
                takeoffTime = OffsetDateTime.of(2026, 4, 28, 10, 0, 0, 0, ZoneOffset.UTC),
                task = "x",
                towplane = tow,
                glider = null,
            )
            every { personOp.execute(tow.pilot) } returns 100L
            every { personOp.execute(null) } returns null
            every { airplaneRepo.tryGetAirplane(tow.airplane!!) } returns TryGetResult(false, 0L)

            shouldThrow<NoSuchElementException> { op.execute(takeoff) }
        }

        it("registers a guest airplane via repository") {
            val (flightRepo, airplaneRepo, personOp) = fixtures()
            val op = TakeoffOperation(flightRepo, airplaneRepo, personOp)

            val guestAirplane = AirplaneModel(id = 0, immatriculation = "OK-GST", type = "GuestType")
            val tow = AirplaneWithCrewModel(
                airplane = guestAirplane,
                pilot = PersonModel(memberId = 1, firstName = "T", lastName = "Pilot"),
            )
            val takeoff = FlightTakeOffModel(
                takeoffTime = OffsetDateTime.of(2026, 4, 28, 10, 0, 0, 0, ZoneOffset.UTC),
                task = "x",
                towplane = tow,
                glider = null,
            )
            every { personOp.execute(tow.pilot) } returns 100L
            every { personOp.execute(null) } returns null
            every { airplaneRepo.addGuestAirplane(guestAirplane) } returns 77L
            every { flightRepo.createFlight(any()) } returns 5L

            op.execute(takeoff)

            verify(exactly = 1) { airplaneRepo.addGuestAirplane(guestAirplane) }
            verify(exactly = 1) { flightRepo.takeoffFlight(null, 5L) }
        }
    }
})
