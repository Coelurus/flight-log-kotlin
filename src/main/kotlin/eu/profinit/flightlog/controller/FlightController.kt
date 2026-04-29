package eu.profinit.flightlog.controller

import eu.profinit.flightlog.facade.FlightFacade
import eu.profinit.flightlog.model.FlightLandingModel
import eu.profinit.flightlog.model.FlightModel
import eu.profinit.flightlog.model.FlightTakeOffModel
import eu.profinit.flightlog.model.ReportModel
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/flight")
@CrossOrigin
class FlightController(
    private val flightFacade: FlightFacade,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/InAir")
    fun getPlanesInAir(): List<FlightModel> {
        logger.debug("Get airplanes in Air.")
        return flightFacade.getAirplanesInAir()
    }

    @PostMapping("/Land")
    fun land(@RequestBody landingModel: FlightLandingModel): ResponseEntity<Void> {
        logger.debug("Land flight.")
        flightFacade.landFlight(landingModel)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/Takeoff")
    fun takeoff(@RequestBody takeOffModel: FlightTakeOffModel): ResponseEntity<Void> =
        try {
            flightFacade.takeoffFlight(takeOffModel)
            logger.debug("Takeoff flight.")
            ResponseEntity.ok().build()
        } catch (ex: UnsupportedOperationException) {
            logger.error("Takeoff flight unable to proceed: {}", ex.message, ex)
            ResponseEntity.badRequest().build()
        }

    @GetMapping("/Report")
    fun report(): List<ReportModel> {
        logger.debug("Build report.")
        return flightFacade.getReport()
    }

    @GetMapping("/Export")
    fun export(): ResponseEntity<ByteArrayResource> {
        val csv = flightFacade.getExportToCsv()
        logger.debug("Export flights into CSV.")
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.csv\"")
            .body(ByteArrayResource(csv))
    }
}
