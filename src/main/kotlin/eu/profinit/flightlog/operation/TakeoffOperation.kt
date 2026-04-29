package eu.profinit.flightlog.operation

import eu.profinit.flightlog.model.AirplaneModel
import eu.profinit.flightlog.model.AirplaneWithCrewModel
import eu.profinit.flightlog.model.CreateFlightModel
import eu.profinit.flightlog.model.FlightTakeOffModel
import eu.profinit.flightlog.model.FlightType
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.repository.AirplaneRepository
import eu.profinit.flightlog.repository.FlightRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class TakeoffOperation(
    private val flightRepository: FlightRepository,
    private val airplaneRepository: AirplaneRepository,
    private val createPersonOperation: CreatePersonOperation,
) {
    companion object {
        private const val GUEST_ID = 0L
    }

    fun execute(takeOffModel: FlightTakeOffModel) {
        if (takeOffModel.towplane == null) {
            throw UnsupportedOperationException("Can not takeoff flight with null towplane.")
        }

        val takeoffTime = toLocalTimeFromZulu(takeOffModel.takeoffTime)

        val towplaneFlightId = createFlight(takeOffModel.towplane, takeoffTime, takeOffModel.task, FlightType.Towplane)
        val gliderFlightId = createFlight(takeOffModel.glider, takeoffTime, takeOffModel.task, FlightType.Glider)

        flightRepository.takeoffFlight(gliderFlightId, towplaneFlightId)
    }

    private fun createFlight(
        airplaneWithCrewModel: AirplaneWithCrewModel?,
        takeoffTime: OffsetDateTime,
        task: String?,
        type: FlightType,
    ): Long? {
        if (airplaneWithCrewModel == null) {
            return null
        }

        val pilotId = createPerson(airplaneWithCrewModel.pilot)
            ?: throw NoSuchElementException("Pilot not found")

        val model = CreateFlightModel(
            airplaneId = createAirplane(airplaneWithCrewModel.airplane),
            pilotId = pilotId,
            copilotId = createPerson(airplaneWithCrewModel.copilot),
            type = type,
            takeOffTime = takeoffTime,
            task = task,
        )

        return flightRepository.createFlight(model)
    }

    private fun createPerson(personModel: PersonModel?): Long? =
        createPersonOperation.execute(personModel)

    private fun createAirplane(airplaneModel: AirplaneModel?): Long {
        if (airplaneModel == null) {
            throw NoSuchElementException("Airplane not provided")
        }
        if (airplaneModel.id == GUEST_ID) {
            return airplaneRepository.addGuestAirplane(airplaneModel)
        }
        val tryGet = airplaneRepository.tryGetAirplane(airplaneModel)
        if (tryGet.found) {
            return tryGet.id
        }
        throw NoSuchElementException("Airplane not found")
    }

    private fun toLocalTimeFromZulu(time: OffsetDateTime): OffsetDateTime =
        time.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
}
