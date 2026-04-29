package eu.profinit.flightlog.operation

import eu.profinit.flightlog.model.FlightLandingModel
import eu.profinit.flightlog.repository.FlightRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class LandOperation(
    private val flightRepository: FlightRepository,
) {
    fun execute(landingModel: FlightLandingModel) {
        landingModel.landingTime = toLocalTimeFromZulu(landingModel.landingTime)
        flightRepository.landFlight(landingModel)
    }

    private fun toLocalTimeFromZulu(time: OffsetDateTime): OffsetDateTime =
        time.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
}
