package eu.profinit.flightlog.operation

import eu.profinit.flightlog.model.FlightType
import eu.profinit.flightlog.repository.entity.AirplaneCategory
import eu.profinit.flightlog.repository.entity.Airplane
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.entity.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class GetExportToCsvOperationTests {

    private val operation = GetExportToCsvOperation(
        flightQueryService = org.mockito.kotlin.mock(),
    )

    @Test
    fun `render produces semicolon-separated CSV with header and BOM`() {
        val airplane = Airplane().apply {
            id = 1; guestAirplaneImmatriculation = "PUS19"; guestAirplaneType = "WT9"
            category = AirplaneCategory.POWERED
        }
        val pilot = Person().apply { id = 1; firstName = "Jan"; lastName = "Houdek" }
        val takeoff = OffsetDateTime.of(2025, 5, 9, 16, 57, 0, 0, ZoneOffset.UTC)
        val landing = takeoff.plusMinutes(15)
        val flight = Flight().apply {
            id = 25
            this.airplane = airplane; this.pilot = pilot; task = "Prostor"
            takeoffTime = takeoff; landingTime = landing; type = FlightType.Towplane
        }

        val out = String(operation.render(listOf(flight)), Charsets.UTF_8)

        assertTrue(out.startsWith("\uFEFF"), "must start with UTF-8 BOM")
        val lines = out.removePrefix("\uFEFF").trim().lines()
        assertEquals("id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno", lines[0])
        assertEquals("25;09-05-2025;WT9;PUS19;Houdek;Prostor;16:57;17:12;0.25;", lines[1])
    }
}

