package eu.profinit.flightlog.model

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

enum class StartTypeDto { WINCH, TOW }
enum class AirplaneCategoryDto { GLIDER, POWERED }

/** FR-03 input: a writer creating a new flight (with optional tow). */
data class CreateFlightRequest(
    @field:NotBlank @field:Pattern(regexp = "\\d{2}-\\d{2}-\\d{4}") val date: String,
    @field:NotBlank @field:Size(max = 50) val pilotName: String,
    @field:NotBlank @field:Size(max = 20) val gliderImmatriculation: String,
    /** Optional aircraft type for glider; used when registering an unknown (guest) aircraft. */
    @field:Size(max = 100) val gliderType: String? = null,
    @field:NotBlank @field:Size(max = 200) val task: String,
    @field:NotBlank @field:Pattern(regexp = "\\d{2}:\\d{2}") val takeoffTime: String,
    @field:NotNull val startType: StartTypeDto,
    @field:Size(max = 50) val towPilotName: String? = null,
    @field:Size(max = 20) val towImmatriculation: String? = null,
    /** Optional tow-plane type when registering an unknown tow aircraft. */
    @field:Size(max = 100) val towType: String? = null,
    /** Optional landing time (admin endpoints only). */
    @field:Pattern(regexp = "\\d{2}:\\d{2}") val landingTime: String? = null,
)

data class LandFlightRequest(
    @field:NotBlank @field:Pattern(regexp = "\\d{2}:\\d{2}") val landingTime: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlightResponse(
    val id: Long,
    val date: String,
    val takeoffTime: String,
    val landingTime: String?,
    val durationHours: Double?,
    val pilotName: String?,
    val pilotClubMember: Boolean,
    val airplaneImmatriculation: String?,
    val airplaneType: String?,
    val category: AirplaneCategoryDto?,
    val task: String?,
    val startType: StartTypeDto?,
    val linkedFlightId: Long?,
    val degraded: Boolean = false,
    val degradedMessage: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateFlightResponse(
    val flights: List<FlightResponse>,
    val degraded: Boolean = false,
    val degradedMessage: String? = null,
)

/** FR-05 filter parameters used by admin listing and CSV export. */
data class FlightFilter(
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val category: AirplaneCategoryDto? = null,
    val immatriculation: String? = null,
    val takeoffFrom: String? = null,
    val takeoffTo: String? = null,
    val durationMin: Double? = null,
    val durationMax: Double? = null,
    val inAirOnly: Boolean = false,
    val landedOnly: Boolean = false,
    @field:Min(0) val page: Int = 0,
    @field:Min(1) @field:Max(50) val size: Int = 25,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** FR-07 import row before persistence. */
data class ImportRow(
    val rowNumber: Int,
    val id: Long?,
    val date: String,
    val type: String,
    val immatriculation: String,
    val pilot: String,
    val task: String,
    val takeoff: String,
    val landing: String,
    val durationHours: Double,
    val linkedRowId: Long?,
)

data class ImportError(val rowNumber: Int, val column: String?, val message: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImportPreviewResponse(
    val toImport: Int,
    val errors: List<ImportError>,
    val confirmationToken: String? = null,
)

data class ImportConfirmRequest(@field:NotBlank val confirmationToken: String)
