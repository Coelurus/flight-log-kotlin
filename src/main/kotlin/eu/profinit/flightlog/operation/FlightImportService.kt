package eu.profinit.flightlog.operation

import eu.profinit.flightlog.audit.AuditService
import eu.profinit.flightlog.model.ImportError
import eu.profinit.flightlog.model.ImportPreviewResponse
import eu.profinit.flightlog.model.ImportRow
import eu.profinit.flightlog.model.FlightType
import eu.profinit.flightlog.repository.entity.AirplaneCategory
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.entity.Person
import eu.profinit.flightlog.repository.entity.PersonType
import eu.profinit.flightlog.repository.jpa.AirplaneJpa
import eu.profinit.flightlog.repository.jpa.FlightJpa
import eu.profinit.flightlog.repository.jpa.PersonJpa
import eu.profinit.flightlog.util.Formats
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** FR-07: parse + atomically import a CSV with the export's structure (§3.2.3). */
@Service
class FlightImportService(
    private val flights: FlightJpa,
    private val airplanes: AirplaneJpa,
    private val persons: PersonJpa,
    private val audit: AuditService,
) {
    private val pendingImports = ConcurrentHashMap<String, List<ImportRow>>()
    private val rng = SecureRandom()

    fun preview(input: InputStream): ImportPreviewResponse {
        val (rows, errors) = parse(input)
        if (errors.isEmpty() && rows.isNotEmpty()) {
            val token = randomToken()
            pendingImports[token] = rows
            return ImportPreviewResponse(rows.size, errors, token)
        }
        return ImportPreviewResponse(rows.size, errors, null)
    }

    @Transactional
    fun confirm(token: String): Int {
        val rows = pendingImports.remove(token)
            ?: throw IllegalArgumentException("Neplatný confirmation token.")
        val rowToFlight = mutableMapOf<Long?, Flight>()

        rows.forEach { row ->
            val airplane = airplanes.findByImmatriculationIgnoreCase(row.immatriculation)
                ?: error("Letadlo '${row.immatriculation}' není evidováno (řádek ${row.rowNumber}).")
            val pilot = persons.findFirstByLastNameIgnoreCase(row.pilot) ?: persons.save(Person().apply {
                lastName = row.pilot
                personType = PersonType.Guest
            })
            val takeoff = Formats.combine(Formats.parseDate(row.date), Formats.parseTime(row.takeoff))
            val landing = Formats.combine(Formats.parseDate(row.date), Formats.parseTime(row.landing))
            val cat = airplane.category ?: AirplaneCategory.POWERED
            val flight = Flight().apply {
                this.airplane = airplane
                this.pilot = pilot
                this.task = row.task
                this.takeoffTime = takeoff
                this.landingTime = landing
                this.type = if (cat == AirplaneCategory.GLIDER) FlightType.Glider else FlightType.Towplane
                this.createdBy = SecurityContextHolder.getContext().authentication?.name
                this.updatedBy = this.createdBy
            }
            flights.save(flight)
            rowToFlight[row.id] = flight
        }
        // Resolve `pripojeno` -> linkedFlight
        rows.forEach { row ->
            val linked = row.linkedRowId?.let(rowToFlight::get) ?: return@forEach
            val current = rowToFlight[row.id] ?: return@forEach
            current.linkedFlight = linked; flights.save(current)
            linked.linkedFlight = current; flights.save(linked)
        }
        audit.logCurrent("FLIGHTS_IMPORTED", details = "count=${rows.size}")
        return rows.size
    }

    /** Parses the file completely, collecting structured errors. */
    fun parse(input: InputStream): Pair<List<ImportRow>, List<ImportError>> {
        val rows = mutableListOf<ImportRow>()
        val errors = mutableListOf<ImportError>()
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine()?.removePrefix("\uFEFF")
                ?: return emptyList<ImportRow>() to listOf(ImportError(0, null, "Soubor je prázdný."))
            val headers = headerLine.split(';').map { it.trim().lowercase() }
            val expected = listOf("id", "datum", "typ", "imatrikulace", "pilot", "ukol", "start", "pristani", "doba", "pripojeno")
            if (headers != expected) {
                errors += ImportError(1, null, "Hlavička musí být: ${expected.joinToString(";")}.")
                return emptyList<ImportRow>() to errors
            }
            var lineNo = 1
            reader.lineSequence().forEach { raw ->
                lineNo++
                if (raw.isBlank()) return@forEach
                val cols = raw.split(';')
                if (cols.size != expected.size) {
                    errors += ImportError(lineNo, null, "Očekáváno ${expected.size} sloupců, nalezeno ${cols.size}.")
                    return@forEach
                }
                try {
                    val row = ImportRow(
                        rowNumber = lineNo,
                        id = cols[0].trim().toLongOrNull(),
                        date = cols[1].trim(),
                        type = cols[2].trim(),
                        immatriculation = cols[3].trim(),
                        pilot = cols[4].trim(),
                        task = cols[5].trim(),
                        takeoff = cols[6].trim(),
                        landing = cols[7].trim(),
                        durationHours = cols[8].trim().toDoubleOrNull() ?: 0.0,
                        linkedRowId = cols[9].trim().takeIf { it.isNotBlank() && it != "null" }?.toLongOrNull(),
                    )
                    Formats.parseDate(row.date)
                    val tk = Formats.parseTime(row.takeoff)
                    val ld = Formats.parseTime(row.landing)
                    if (!ld.isAfter(tk)) errors += ImportError(lineNo, "pristani", "Přistání musí být po startu.")
                    if (row.immatriculation.isBlank()) errors += ImportError(lineNo, "imatrikulace", "Povinné pole.")
                    if (row.pilot.isBlank()) errors += ImportError(lineNo, "pilot", "Povinné pole.")
                    rows += row
                } catch (ex: Exception) {
                    errors += ImportError(lineNo, null, "Nelze zpracovat řádek: ${ex.message}")
                }
            }
            // Validate `pripojeno` references resolve to another row's `id` in this file.
            val ids = rows.mapNotNull { it.id }.toSet()
            rows.forEach { r ->
                val link = r.linkedRowId
                if (link != null && link !in ids) {
                    errors += ImportError(r.rowNumber, "pripojeno", "Odkaz na neexistující ID $link v souboru.")
                }
            }
        }
        return rows to errors
    }

    private fun randomToken(): String {
        val buf = ByteArray(24); rng.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }
}
