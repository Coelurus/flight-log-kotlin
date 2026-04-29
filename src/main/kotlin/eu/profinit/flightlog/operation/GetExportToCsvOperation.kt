package eu.profinit.flightlog.operation

import eu.profinit.flightlog.model.FlightFilter
import eu.profinit.flightlog.repository.entity.AirplaneCategory
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.util.Formats
import org.springframework.stereotype.Component
import java.time.Duration

/** FR-06 / §3.2.2: serialise a list of flights into the prescribed CSV format. */
@Component
class GetExportToCsvOperation(
    private val flightQueryService: FlightQueryService,
) {
    /** Default execution returns export of all flights (no filter). */
    fun execute(): ByteArray = execute(FlightFilter(page = 0, size = Int.MAX_VALUE))

    fun execute(filter: FlightFilter): ByteArray {
        val flights = flightQueryService.findAll(filter)
        return render(flights)
    }

    fun render(flights: List<Flight>): ByteArray {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM so Excel opens it correctly.
        sb.append("id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno\n")
        flights.forEach { f ->
            val airplane = f.airplane?.toModel()
            val pilotLast = f.pilot?.lastName.orEmpty()
            val durationHours = f.landingTime?.let {
                Duration.between(f.takeoffTime, it).toMillis() / 3_600_000.0
            } ?: 0.0
            val cat = f.airplane?.category ?: AirplaneCategory.POWERED
            val typ = airplane?.type.orEmpty()
            sb.append(f.id).append(';')
                .append(Formats.formatDate(f.takeoffTime)).append(';')
                .append(escape(typ)).append(';')
                .append(escape(airplane?.immatriculation.orEmpty())).append(';')
                .append(escape(pilotLast)).append(';')
                .append(escape(f.task.orEmpty())).append(';')
                .append(Formats.formatTime(f.takeoffTime)).append(';')
                .append(f.landingTime?.let(Formats::formatTime).orEmpty()).append(';')
                .append(Formats.formatDuration(durationHours)).append(';')
                // 'pripojeno' is the linked GLIDER id when this is a tow flight, otherwise empty.
                .append(linkedGliderId(f, cat)?.toString().orEmpty())
                .append('\n')
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun linkedGliderId(f: Flight, cat: AirplaneCategory): Long? {
        if (cat != AirplaneCategory.POWERED) return null
        return f.linkedFlight?.id
    }

    private fun escape(s: String): String =
        if (s.contains(';') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
}

