package eu.profinit.flightlog.repository

import eu.profinit.flightlog.TestDatabaseGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class FlightRepositoryTests @Autowired constructor(
    private val flightRepository: FlightRepository,
    private val testDatabaseGenerator: TestDatabaseGenerator,
) {

    @Test
    @Disabled("TODO 2.2: Upravte volanou metodu, aby vrátila pouze lety, které jsou kluzáky.")
    fun `get flights of type glider returns 2 gliders`() {
        testDatabaseGenerator.renewDatabase()
        val result = flightRepository.getAllFlights()
        // val result = flightRepository.getFlightsOfType(FlightType.Glider)
        assertEquals(2, result.size, "In test database is 2 gliders.")
    }

    @Test
    @Disabled("TODO 2.4: Doplňte metodu repozitáře a odstraňte přeskočení testu.")
    fun `get airplanes in air returns flight models`() {
        testDatabaseGenerator.renewDatabase()
        val result = flightRepository.getAllFlights()
        // val result = flightRepository.getAirplanesInAir()
        assertEquals(true, result.isNotEmpty())
    }

    @Test
    fun `get report returns 3 starts`() {
        testDatabaseGenerator.renewDatabase()
        val result = flightRepository.getReport()
        val flights = result.flatMap { listOf(it.glider, it.towplane) }
        assertEquals(3, result.size, "In test database is 3 flight starts")
        assertNull(flights[4], "Last flight start should have null glider.")
    }
}
