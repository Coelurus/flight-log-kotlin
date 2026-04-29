package eu.profinit.flightlog.facade

import eu.profinit.flightlog.model.AirplaneModel
import eu.profinit.flightlog.repository.AirplaneRepository
import org.springframework.stereotype.Component

@Component
class AirplaneFacade(
    private val airplaneRepository: AirplaneRepository,
) {
    fun getClubAirplanes(): List<AirplaneModel> = airplaneRepository.getClubAirplanes()
}
