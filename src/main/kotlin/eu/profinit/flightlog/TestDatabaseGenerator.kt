package eu.profinit.flightlog

import eu.profinit.flightlog.model.FlightType
import eu.profinit.flightlog.repository.entity.Address
import eu.profinit.flightlog.repository.entity.Airplane
import eu.profinit.flightlog.repository.entity.AirplaneType
import eu.profinit.flightlog.repository.entity.ClubAirplane
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.entity.FlightStart
import eu.profinit.flightlog.repository.entity.Person
import eu.profinit.flightlog.repository.jpa.AirplaneJpa
import eu.profinit.flightlog.repository.jpa.ClubAirplaneJpa
import eu.profinit.flightlog.repository.jpa.FlightJpa
import eu.profinit.flightlog.repository.jpa.FlightStartJpa
import eu.profinit.flightlog.repository.jpa.PersonJpa
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class TestDatabaseGenerator(
    private val airplaneJpa: AirplaneJpa,
    private val clubAirplaneJpa: ClubAirplaneJpa,
    private val flightJpa: FlightJpa,
    private val flightStartJpa: FlightStartJpa,
    private val personJpa: PersonJpa,
) {

    @Transactional
    fun renewDatabase() {
        deleteAll()
        createTestDatabaseWithFixedTime(OffsetDateTime.now())
    }

    @Transactional
    fun deleteAll() {
        flightStartJpa.deleteAll()
        flightJpa.deleteAll()
        airplaneJpa.deleteAll()
        clubAirplaneJpa.deleteAll()
        personJpa.deleteAll()
    }

    @Transactional
    fun initializeClubAirplanes() {
        val type1 = AirplaneType().apply { type = "L-13A Blaník" }
        val type2 = AirplaneType().apply { type = "Zlín Z-42M" }
        clubAirplaneJpa.saveAll(
            listOf(
                ClubAirplane().apply { immatriculation = "OK-B123"; airplaneType = type1 },
                ClubAirplane().apply { immatriculation = "OK-V23424"; airplaneType = type2 },
            )
        )
    }

    @Transactional
    fun createTestDatabaseWithFixedTime(now: OffsetDateTime) {
        initializeClubAirplanes()

        val people = personJpa.saveAll(
            listOf(
                Person().apply { firstName = "Kamila"; lastName = "Spoustová" },
                Person().apply { firstName = "Naděžda"; lastName = "Pavelková" },
                Person().apply { firstName = "Silvie"; lastName = "Hronová" },
                Person().apply { firstName = "Miloš"; lastName = "Korbel" },
                Person().apply { firstName = "Petr"; lastName = "Hrubec" },
                Person().apply { firstName = "Michal"; lastName = "Vyvlečka" },
                Person().apply {
                    firstName = "Lenka"
                    lastName = "Kiasová"
                    address = Address()
                },
            )
        )

        val silvie = people[2]
        val petr = people[4]
        val lenka = people[6]

        val airplanes = airplaneJpa.saveAll(
            listOf(
                Airplane().apply { guestAirplaneImmatriculation = "OK-V23428"; guestAirplaneType = "Zlín Z-42M" },
                Airplane().apply { guestAirplaneImmatriculation = "OK-B128"; guestAirplaneType = "L-13A Blaník" },
            )
        )
        val zlin = airplanes[0]
        val blanik = airplanes[1]

        val flights = flightJpa.saveAll(
            listOf(
                Flight().apply {
                    takeoffTime = now.minusMinutes(10)
                    airplane = zlin; pilot = lenka; task = "VLEK"; type = FlightType.Glider
                },
                Flight().apply {
                    takeoffTime = now.minusMinutes(10)
                    airplane = blanik; pilot = silvie; task = "Tahac"; type = FlightType.Towplane
                },
                Flight().apply {
                    takeoffTime = now.minusMinutes(100)
                    airplane = zlin; pilot = petr; task = "VLEK"; type = FlightType.Glider
                },
                Flight().apply {
                    takeoffTime = now.minusMinutes(100)
                    airplane = blanik; pilot = silvie; task = "Tahac"; type = FlightType.Towplane
                },
                Flight().apply {
                    takeoffTime = OffsetDateTime.of(2020, 1, 7, 16, 47, 10, 0, ZoneOffset.UTC)
                    landingTime = OffsetDateTime.of(2020, 1, 7, 17, 17, 10, 0, ZoneOffset.UTC)
                    airplane = blanik; pilot = lenka; task = "Tahac"; type = FlightType.Towplane
                },
            )
        )

        flightStartJpa.saveAll(
            listOf(
                FlightStart().apply { glider = flights[0]; towplane = flights[1] },
                FlightStart().apply { glider = flights[2]; towplane = flights[3] },
                FlightStart().apply { towplane = flights[4] },
            )
        )
    }
}
