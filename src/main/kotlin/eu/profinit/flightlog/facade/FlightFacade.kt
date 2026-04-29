package eu.profinit.flightlog.facade

import eu.profinit.flightlog.model.FlightLandingModel
import eu.profinit.flightlog.model.FlightModel
import eu.profinit.flightlog.model.FlightTakeOffModel
import eu.profinit.flightlog.model.ReportModel
import eu.profinit.flightlog.operation.GetExportToCsvOperation
import eu.profinit.flightlog.operation.LandOperation
import eu.profinit.flightlog.operation.TakeoffOperation
import eu.profinit.flightlog.repository.FlightRepository
import org.springframework.stereotype.Component

@Component
class FlightFacade(
    @Suppress("unused") private val flightRepository: FlightRepository,
    private val takeoffOperation: TakeoffOperation,
    private val getExportToCsvOperation: GetExportToCsvOperation,
    private val landOperation: LandOperation,
) {
    // TODO 2.5: Doplňte metodu repozitáře, která vrátí letadla ve vzduchu v listě modelů.
    fun getAirplanesInAir(): List<FlightModel> = emptyList()

    fun getExportToCsv(): ByteArray = getExportToCsvOperation.execute()

    fun landFlight(landingModel: FlightLandingModel) = landOperation.execute(landingModel)

    fun getReport(): List<ReportModel> = flightRepository.getReport()

    fun takeoffFlight(takeOffModel: FlightTakeOffModel) = takeoffOperation.execute(takeOffModel)
}
