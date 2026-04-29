package eu.profinit.flightlog.operation

import eu.profinit.flightlog.audit.AuditService
import eu.profinit.flightlog.repository.jpa.AirplaneJpa
import eu.profinit.flightlog.repository.jpa.FlightJpa
import eu.profinit.flightlog.repository.jpa.PersonJpa
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class FlightImportServiceTests {

    private val service = FlightImportService(
        flights = mock<FlightJpa>(),
        airplanes = mock<AirplaneJpa>(),
        persons = mock<PersonJpa>(),
        audit = mock<AuditService>(),
    )

    @Test
    fun `parses well-formed file`() {
        val csv = """
            id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
            1;09-05-2025;WT9;PUS19;Houdek;Prostor;16:57;17:12;0.25;
            2;09-05-2025;Blanik;OK-1234;Novak;VLEK;16:57;17:10;0.22;1
        """.trimIndent().byteInputStream(Charsets.UTF_8)

        val (rows, errors) = service.parse(csv)

        assertEquals(2, rows.size)
        assertTrue(errors.isEmpty(), "errors: $errors")
        assertEquals(1L, rows[1].linkedRowId)
    }

    @Test
    fun `rejects landing before takeoff`() {
        val csv = """
            id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
            1;09-05-2025;WT9;PUS19;Houdek;Prostor;17:12;16:57;0.25;
        """.trimIndent().byteInputStream(Charsets.UTF_8)

        val (_, errors) = service.parse(csv)

        assertTrue(errors.any { it.column == "pristani" }, "expected pristani error, got $errors")
    }

    @Test
    fun `rejects unknown pripojeno reference`() {
        val csv = """
            id;datum;typ;imatrikulace;pilot;ukol;start;pristani;doba;pripojeno
            1;09-05-2025;WT9;PUS19;Houdek;Prostor;16:57;17:12;0.25;99
        """.trimIndent().byteInputStream(Charsets.UTF_8)

        val (_, errors) = service.parse(csv)

        assertTrue(errors.any { it.column == "pripojeno" }, "expected pripojeno error, got $errors")
    }
}
