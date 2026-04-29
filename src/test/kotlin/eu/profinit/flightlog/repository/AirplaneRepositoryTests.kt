package eu.profinit.flightlog.repository

import eu.profinit.flightlog.TestDatabaseGenerator
import eu.profinit.flightlog.model.AirplaneModel
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AirplaneRepositoryTests @Autowired constructor(
    private val airplaneRepository: AirplaneRepository,
    private val testDatabaseGenerator: TestDatabaseGenerator,
) {

    @Test
    fun `add guest airplane returns positive id`() {
        testDatabaseGenerator.renewDatabase()

        val airplaneModel = AirplaneModel(id = 0, immatriculation = "OKA-424", type = "Zlín")
        val result = airplaneRepository.addGuestAirplane(airplaneModel)

        assertTrue(result > 0, "There should be Id (> 0) of new guest airplane.")
    }

    @Test
    fun `get club airplanes is not empty`() {
        testDatabaseGenerator.renewDatabase()
        val result = airplaneRepository.getClubAirplanes()
        assertFalse(result.isEmpty())
    }
}
