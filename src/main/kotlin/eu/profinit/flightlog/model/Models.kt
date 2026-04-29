package eu.profinit.flightlog.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

enum class FlightType { Glider, Towplane }

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AddressModel(
    val street: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AirplaneModel(
    val id: Long,
    val immatriculation: String,
    val type: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonModel(
    val memberId: Long,
    val firstName: String? = null,
    val lastName: String? = null,
    val address: AddressModel? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AirplaneWithCrewModel(
    val airplane: AirplaneModel? = null,
    val pilot: PersonModel? = null,
    val copilot: PersonModel? = null,
    val note: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlightModel(
    val id: Long,
    val takeoffTime: OffsetDateTime,
    val landingTime: OffsetDateTime? = null,
    val airplane: AirplaneModel? = null,
    val pilot: PersonModel? = null,
    val copilot: PersonModel? = null,
    val task: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlightTakeOffModel(
    val takeoffTime: OffsetDateTime,
    val task: String? = null,
    val towplane: AirplaneWithCrewModel? = null,
    val glider: AirplaneWithCrewModel? = null,
)

data class FlightLandingModel(
    val flightId: Long,
    var landingTime: OffsetDateTime,
)

data class CreateFlightModel(
    val airplaneId: Long,
    val pilotId: Long,
    val copilotId: Long? = null,
    val takeOffTime: OffsetDateTime,
    val task: String? = null,
    val type: FlightType,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportModel(
    val towplane: FlightModel? = null,
    val glider: FlightModel? = null,
)
