package eu.profinit.flightlog.repository.impl

import eu.profinit.flightlog.model.CreateFlightModel
import eu.profinit.flightlog.model.FlightLandingModel
import eu.profinit.flightlog.model.FlightModel
import eu.profinit.flightlog.model.ReportModel
import eu.profinit.flightlog.repository.FlightRepository
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.entity.FlightStart
import eu.profinit.flightlog.repository.jpa.AirplaneJpa
import eu.profinit.flightlog.repository.jpa.FlightJpa
import eu.profinit.flightlog.repository.jpa.FlightStartJpa
import eu.profinit.flightlog.repository.jpa.PersonJpa
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class FlightRepositoryImpl(
    private val flightJpa: FlightJpa,
    private val flightStartJpa: FlightStartJpa,
    private val airplaneJpa: AirplaneJpa,
    private val personJpa: PersonJpa,
) : FlightRepository {

    // TODO 2.1: Upravte metodu tak, aby vrátila pouze lety specifického typu
    @Transactional(readOnly = true)
    override fun getAllFlights(): List<FlightModel> =
        flightJpa.findAll().map { it.toModel() }

    // TODO 2.3: Vytvořte metodu, která načte letadla, která jsou ve vzduchu, seřadí je
    // od nejstarších, a v případě shody dá vlečné před kluzák, který táhne.

    override fun landFlight(landingModel: FlightLandingModel) {
        val flight = flightJpa.findById(landingModel.flightId).orElseThrow {
            UnsupportedOperationException("Unable to land not-registered flight: $landingModel.")
        }
        flight.landingTime = landingModel.landingTime
        flightJpa.save(flight)
    }

    override fun takeoffFlight(gliderFlightId: Long?, towplaneFlightId: Long?) {
        val flightStart = FlightStart().apply {
            glider = gliderFlightId?.let { flightJpa.findById(it).orElse(null) }
            towplane = towplaneFlightId?.let { flightJpa.findById(it).orElse(null) }
        }
        flightStartJpa.save(flightStart)
    }

    override fun createFlight(model: CreateFlightModel): Long {
        val copilotEntity = model.copilotId?.let { personJpa.findById(it).orElse(null) }
        val flight = Flight().apply {
            airplane = airplaneJpa.findById(model.airplaneId).orElse(null)
            copilot = copilotEntity
            pilot = personJpa.findById(model.pilotId).orElse(null)
            takeoffTime = model.takeOffTime
            task = model.task
            type = model.type
        }
        return flightJpa.save(flight).id
    }

    @Transactional(readOnly = true)
    override fun getReport(): List<ReportModel> =
        flightStartJpa.findAllForReport().map { it.toModel() }
}
