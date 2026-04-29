package eu.profinit.flightlog.operation

import eu.profinit.flightlog.audit.AuditService
import eu.profinit.flightlog.integration.ClubUserService
import eu.profinit.flightlog.integration.DegradedModeHolder
import eu.profinit.flightlog.model.AirplaneCategoryDto
import eu.profinit.flightlog.model.CreateFlightRequest
import eu.profinit.flightlog.model.CreateFlightResponse
import eu.profinit.flightlog.model.FlightResponse
import eu.profinit.flightlog.model.FlightType
import eu.profinit.flightlog.model.LandFlightRequest
import eu.profinit.flightlog.model.StartTypeDto
import eu.profinit.flightlog.repository.entity.AirplaneCategory
import eu.profinit.flightlog.repository.entity.AuditLevel
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.entity.Person
import eu.profinit.flightlog.repository.entity.PersonType
import eu.profinit.flightlog.repository.entity.StartType
import eu.profinit.flightlog.repository.jpa.AirplaneJpa
import eu.profinit.flightlog.repository.jpa.FlightJpa
import eu.profinit.flightlog.repository.jpa.PersonJpa
import eu.profinit.flightlog.util.Formats
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

class BusinessRuleException(message: String, val field: String? = null) : RuntimeException(message)

@Service
class FlightWriteService(
    private val flights: FlightJpa,
    private val airplanes: AirplaneJpa,
    private val persons: PersonJpa,
    private val clubUsers: ClubUserService,
    private val degraded: DegradedModeHolder,
    private val audit: AuditService,
) {

    @Transactional
    fun create(req: CreateFlightRequest): CreateFlightResponse {
        validateConditional(req)
        val takeoff = parseDateTime(req.date, req.takeoffTime)
        val landing = req.landingTime?.let { parseDateTime(req.date, it) }
        if (landing != null && !landing.isAfter(takeoff)) {
            throw BusinessRuleException("Čas přistání musí být pozdější než čas startu.", "landingTime")
        }
        val (gliderAirplane, gliderCategory) = resolveAirplane(
            req.gliderImmatriculation, req.gliderType, AirplaneCategory.GLIDER, "gliderImmatriculation"
        )
        val gliderPilot = resolvePilot(req.pilotName)

        val gliderFlight = Flight().apply {
            this.airplane = gliderAirplane
            this.pilot = gliderPilot
            this.task = req.task
            this.takeoffTime = takeoff
            this.landingTime = landing
            this.type = if (gliderCategory == AirplaneCategory.GLIDER) FlightType.Glider else FlightType.Towplane
            this.startType = req.startType.toEntity()
            this.createdBy = currentUser()
            this.updatedBy = currentUser()
        }
        flights.save(gliderFlight)

        var towFlight: Flight? = null
        if (req.startType == StartTypeDto.TOW) {
            val (towAirplane, _) = resolveAirplane(
                req.towImmatriculation!!, req.towType, AirplaneCategory.POWERED, "towImmatriculation"
            )
            val towPilot = resolvePilot(req.towPilotName!!)
            towFlight = Flight().apply {
                this.airplane = towAirplane
                this.pilot = towPilot
                this.task = req.task
                this.takeoffTime = takeoff
                this.landingTime = landing
                this.type = FlightType.Towplane
                this.startType = StartType.TOW
                this.createdBy = currentUser()
                this.updatedBy = currentUser()
            }
            flights.save(towFlight)
            gliderFlight.linkedFlight = towFlight
            towFlight.linkedFlight = gliderFlight
            flights.save(gliderFlight); flights.save(towFlight)
        }

        audit.logCurrent("FLIGHT_CREATED", "Flight", gliderFlight.id)
        towFlight?.let { audit.logCurrent("FLIGHT_CREATED", "Flight", it.id) }

        val list = listOfNotNull(gliderFlight, towFlight).map { toResponse(it) }
        return CreateFlightResponse(list, degraded.degraded, degraded.message)
    }

    @Transactional
    fun land(id: Long, req: LandFlightRequest): FlightResponse {
        val flight = flights.findById(id).orElseThrow { BusinessRuleException("Let nenalezen.") }
        val landing = parseDateTime(Formats.DATE.format(flight.takeoffTime), req.landingTime)
        if (!landing.isAfter(flight.takeoffTime)) {
            throw BusinessRuleException("Čas přistání musí být pozdější než čas startu.", "landingTime")
        }
        flight.landingTime = landing
        flight.updatedBy = currentUser()
        flights.save(flight)
        flight.linkedFlight?.let {
            it.landingTime = landing
            it.updatedBy = currentUser()
            flights.save(it)
        }
        audit.logCurrent("FLIGHT_LANDED", "Flight", flight.id)
        return toResponse(flight)
    }

    @Transactional
    fun update(id: Long, req: CreateFlightRequest): FlightResponse {
        val flight = flights.findById(id).orElseThrow { BusinessRuleException("Let nenalezen.") }
        val before = snapshot(flight)
        validateConditional(req)
        val takeoff = parseDateTime(req.date, req.takeoffTime)
        val landing = req.landingTime?.let { parseDateTime(req.date, it) }
        if (landing != null && !landing.isAfter(takeoff)) {
            throw BusinessRuleException("Čas přistání musí být pozdější než čas startu.", "landingTime")
        }
        val (gliderAirplane, _) = resolveAirplane(
            req.gliderImmatriculation, req.gliderType, AirplaneCategory.GLIDER, "gliderImmatriculation"
        )
        flight.airplane = gliderAirplane
        flight.pilot = resolvePilot(req.pilotName)
        flight.task = req.task
        flight.takeoffTime = takeoff
        flight.landingTime = landing
        flight.startType = req.startType.toEntity()
        flight.updatedBy = currentUser()
        flights.save(flight)
        audit.logCurrent("FLIGHT_UPDATED", "Flight", flight.id, details = "before=$before")
        return toResponse(flight)
    }

    @Transactional
    fun delete(id: Long, cascade: Boolean) {
        val flight = flights.findById(id).orElseThrow { BusinessRuleException("Let nenalezen.") }
        val link = flight.linkedFlight
        audit.logCurrent("FLIGHT_DELETING", "Flight", flight.id, details = snapshot(flight), level = AuditLevel.WARN)
        if (link != null) {
            if (cascade) {
                // Break circular FK first to avoid constraint violations.
                link.linkedFlight = null; flight.linkedFlight = null
                flights.save(link); flights.save(flight)
                flights.deleteById(link.id)
            } else {
                link.linkedFlight = null; flight.linkedFlight = null
                flights.save(link)
            }
        }
        flights.deleteById(flight.id)
        audit.logCurrent("FLIGHT_DELETED", "Flight", id, level = AuditLevel.WARN)
    }

    fun toResponse(flight: Flight): FlightResponse {
        val durationHours = flight.landingTime?.let {
            java.time.Duration.between(flight.takeoffTime, it).toMillis() / 3_600_000.0
        }?.let { Math.round(it * 100.0) / 100.0 }
        val airplane = flight.airplane
        val cat = airplane?.category?.let {
            if (it == AirplaneCategory.GLIDER) AirplaneCategoryDto.GLIDER else AirplaneCategoryDto.POWERED
        } ?: when (flight.type) {
            FlightType.Glider -> AirplaneCategoryDto.GLIDER
            FlightType.Towplane -> AirplaneCategoryDto.POWERED
        }
        val clubMember = flight.pilot?.let { it.personType == PersonType.ClubMember } ?: false
        return FlightResponse(
            id = flight.id,
            date = Formats.formatDate(flight.takeoffTime),
            takeoffTime = Formats.formatTime(flight.takeoffTime),
            landingTime = flight.landingTime?.let(Formats::formatTime),
            durationHours = durationHours,
            pilotName = flight.pilot?.let { listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifBlank { null } },
            pilotClubMember = clubMember,
            airplaneImmatriculation = airplane?.toModel()?.immatriculation,
            airplaneType = airplane?.toModel()?.type,
            category = cat,
            task = flight.task,
            startType = flight.startType?.let { StartTypeDto.valueOf(it.name) },
            linkedFlightId = flight.linkedFlight?.id,
            degraded = degraded.degraded,
            degradedMessage = degraded.message,
        )
    }

    private fun validateConditional(req: CreateFlightRequest) {
        if (req.startType == StartTypeDto.TOW) {
            if (req.towPilotName.isNullOrBlank())
                throw BusinessRuleException("Pilot vlečného letadla je povinný.", "towPilotName")
            if (req.towImmatriculation.isNullOrBlank())
                throw BusinessRuleException("Imatrikulace vlečného letadla je povinná.", "towImmatriculation")
        }
    }

    /**
     * Resolves an Airplane by immatriculation. If the registration is unknown, a *guest*
     * Airplane row is auto-created (matching the original .NET behaviour: writers may log
     * flights for visiting aircraft without admin pre-registration).
     */
    private fun resolveAirplane(
        immatriculation: String,
        type: String?,
        defaultCategory: AirplaneCategory,
        @Suppress("UNUSED_PARAMETER") field: String,
    ): Pair<eu.profinit.flightlog.repository.entity.Airplane, AirplaneCategory> {
        val existing = airplanes.findByImmatriculationIgnoreCase(immatriculation)
        if (existing != null) {
            val cat = existing.category ?: defaultCategory
            return existing to cat
        }
        // Auto-register as guest airplane (FR-03: writers can register flights of guest aircraft).
        val guest = eu.profinit.flightlog.repository.entity.Airplane().apply {
            this.guestAirplaneImmatriculation = immatriculation.trim()
            this.guestAirplaneType = type?.trim()?.ifBlank { null } ?: "Neznámý typ"
            this.category = defaultCategory
        }
        val saved = airplanes.save(guest)
        return saved to defaultCategory
    }

    private fun resolvePilot(displayName: String): Person {
        val parts = displayName.trim().split(" ", limit = 2)
        val first = if (parts.size == 2) parts[0] else null
        val last = if (parts.size == 2) parts[1] else parts[0]
        // Try ClubDB first
        val clubMatch = clubUsers.getClubUsers().firstOrNull {
            (it.firstName?.equals(first, true) ?: (first == null)) &&
                it.lastName?.equals(last, true) == true
        }
        if (clubMatch != null) {
            if (!clubUsers.isPilot(clubMatch.memberId)) {
                throw BusinessRuleException("Osoba '$displayName' nemá oprávnění PILOT.", "pilotName")
            }
            val existing = persons.findFirstByMemberId(clubMatch.memberId)
            if (existing != null) return existing
            return persons.save(Person().apply {
                firstName = clubMatch.firstName; lastName = clubMatch.lastName
                memberId = clubMatch.memberId; personType = PersonType.ClubMember
            })
        }
        // Fall back to a guest record (treated as external)
        return persons.save(Person().apply {
            firstName = first; lastName = last; personType = PersonType.Guest
        })
    }

    private fun parseDateTime(date: String, time: String): OffsetDateTime =
        Formats.combine(Formats.parseDate(date), Formats.parseTime(time))

    private fun currentUser(): String? = SecurityContextHolder.getContext().authentication?.name

    private fun snapshot(f: Flight): String =
        "id=${f.id};takeoff=${f.takeoffTime};landing=${f.landingTime};pilot=${f.pilot?.id};airplane=${f.airplane?.id};task=${f.task}"

    private fun StartTypeDto.toEntity(): StartType = StartType.valueOf(name)
}
