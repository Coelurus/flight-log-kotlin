package eu.profinit.flightlog.operation

import eu.profinit.flightlog.model.AirplaneCategoryDto
import eu.profinit.flightlog.model.FlightFilter
import eu.profinit.flightlog.model.FlightResponse
import eu.profinit.flightlog.model.PagedResponse
import eu.profinit.flightlog.repository.entity.AirplaneCategory
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.jpa.FlightJpa
import eu.profinit.flightlog.util.Formats
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class FlightQueryService(
    private val flights: FlightJpa,
    private val writeService: FlightWriteService,
) {

    @Transactional(readOnly = true)
    fun list(filter: FlightFilter): PagedResponse<FlightResponse> {
        val pageable = PageRequest.of(filter.page, filter.size, Sort.by(Sort.Direction.DESC, "takeoffTime"))
        val page = flights.findAll(buildSpec(filter), pageable)
        val items = page.content.map(writeService::toResponse)
        return PagedResponse(items, page.number, page.size, page.totalElements, page.totalPages)
    }

    @Transactional(readOnly = true)
    fun findAll(filter: FlightFilter): List<Flight> =
        flights.findAll(buildSpec(filter), Sort.by(Sort.Direction.DESC, "takeoffTime"))

    private fun buildSpec(filter: FlightFilter): Specification<Flight> = Specification { root, _, cb ->
        val predicates = mutableListOf<Predicate>()

        filter.dateFrom?.let {
            val from = Formats.parseDate(it).atStartOfDay().atOffset(ZoneOffset.UTC)
            predicates += cb.greaterThanOrEqualTo(root.get("takeoffTime"), from)
        }
        filter.dateTo?.let {
            val to = Formats.parseDate(it).atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)
            predicates += cb.lessThanOrEqualTo(root.get("takeoffTime"), to)
        }
        filter.category?.let { cat ->
            val mapped = if (cat == AirplaneCategoryDto.GLIDER) AirplaneCategory.GLIDER else AirplaneCategory.POWERED
            val airplane = root.join<Flight, Any>("airplane", jakarta.persistence.criteria.JoinType.LEFT)
            predicates += cb.equal(airplane.get<AirplaneCategory>("category"), mapped)
        }
        filter.immatriculation?.let { imm ->
            val airplane = root.join<Flight, Any>("airplane", jakarta.persistence.criteria.JoinType.LEFT)
            val club = airplane.join<Any, Any>("clubAirplane", jakarta.persistence.criteria.JoinType.LEFT)
            val clubImm = cb.lower(club.get("immatriculation"))
            val guestImm = cb.lower(airplane.get("guestAirplaneImmatriculation"))
            val target = imm.lowercase()
            predicates += cb.or(cb.equal(clubImm, target), cb.equal(guestImm, target))
        }
        filter.takeoffFrom?.let { predicates += cb.greaterThanOrEqualTo(root.get("takeoffTime"), parseTimeBoundary(it, false)) }
        filter.takeoffTo?.let { predicates += cb.lessThanOrEqualTo(root.get("takeoffTime"), parseTimeBoundary(it, true)) }
        if (filter.inAirOnly) predicates += cb.isNull(root.get<OffsetDateTime>("landingTime"))

        cb.and(*predicates.toTypedArray())
    }

    private fun parseTimeBoundary(time: String, end: Boolean): OffsetDateTime {
        // Accept either HH:mm (time only - applied to all dates? we treat as today) or full DD-MM-YYYY HH:mm
        return try {
            val parts = time.split(" ")
            val date = Formats.parseDate(parts[0])
            val t = if (parts.size > 1) Formats.parseTime(parts[1]) else if (end) LocalTime.MAX else LocalTime.MIN
            OffsetDateTime.of(date, t, ZoneOffset.UTC)
        } catch (_: Exception) {
            // Fallback - treat as time-of-day boundary today.
            OffsetDateTime.of(java.time.LocalDate.now(), Formats.parseTime(time), ZoneOffset.UTC)
        }
    }
}
