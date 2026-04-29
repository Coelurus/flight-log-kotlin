package eu.profinit.flightlog.repository

import eu.profinit.flightlog.model.AirplaneModel
import eu.profinit.flightlog.model.CreateFlightModel
import eu.profinit.flightlog.model.FlightLandingModel
import eu.profinit.flightlog.model.FlightModel
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.model.ReportModel

/**
 * Result holder used in place of the C# `out` parameter pattern (`bool TryGet(..., out long id)`).
 */
data class TryGetResult(val found: Boolean, val id: Long)

interface AirplaneRepository {
    fun addGuestAirplane(airplaneModel: AirplaneModel): Long
    fun tryGetAirplane(airplaneModel: AirplaneModel): TryGetResult
    fun getClubAirplanes(): List<AirplaneModel>
}

interface FlightRepository {
    fun getReport(): List<ReportModel>
    fun landFlight(landingModel: FlightLandingModel)
    fun takeoffFlight(gliderFlightId: Long?, towplaneFlightId: Long?)
    fun createFlight(model: CreateFlightModel): Long
    fun getAllFlights(): List<FlightModel>
}

interface PersonRepository {
    fun addGuestPerson(person: PersonModel): Long
    fun tryGetPerson(personModel: PersonModel): TryGetResult
    fun createClubMember(pilot: PersonModel): Long
}
