package eu.profinit.flightlog.repository.impl

import eu.profinit.flightlog.model.AirplaneModel
import eu.profinit.flightlog.repository.AirplaneRepository
import eu.profinit.flightlog.repository.TryGetResult
import eu.profinit.flightlog.repository.entity.Airplane
import eu.profinit.flightlog.repository.jpa.AirplaneJpa
import eu.profinit.flightlog.repository.jpa.ClubAirplaneJpa
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class AirplaneRepositoryImpl(
    private val airplaneJpa: AirplaneJpa,
    private val clubAirplaneJpa: ClubAirplaneJpa,
) : AirplaneRepository {

    override fun addGuestAirplane(airplaneModel: AirplaneModel): Long {
        val airplane = Airplane().apply {
            guestAirplaneImmatriculation = airplaneModel.immatriculation
            guestAirplaneType = airplaneModel.type
        }
        return airplaneJpa.save(airplane).id
    }

    @Transactional(readOnly = true)
    override fun getClubAirplanes(): List<AirplaneModel> =
        clubAirplaneJpa.findAllWithType().map { it.toModel() }

    @Transactional(readOnly = true)
    override fun tryGetAirplane(airplaneModel: AirplaneModel): TryGetResult {
        val found = airplaneJpa.findById(airplaneModel.id).orElse(null)
        return if (found != null) TryGetResult(true, found.id) else TryGetResult(false, 0)
    }
}
