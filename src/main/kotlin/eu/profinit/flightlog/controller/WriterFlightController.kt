package eu.profinit.flightlog.controller

import eu.profinit.flightlog.model.CreateFlightRequest
import eu.profinit.flightlog.model.CreateFlightResponse
import eu.profinit.flightlog.model.FlightResponse
import eu.profinit.flightlog.model.LandFlightRequest
import eu.profinit.flightlog.operation.FlightWriteService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/writer/flights")
@PreAuthorize("hasAnyRole('WRITER','ADMIN')")
class WriterFlightController(
    private val writeService: FlightWriteService,
) {
    @PostMapping
    fun create(@Valid @RequestBody body: CreateFlightRequest): CreateFlightResponse =
        writeService.create(body)

    @PostMapping("/{id}/land")
    fun land(@PathVariable id: Long, @Valid @RequestBody body: LandFlightRequest): FlightResponse =
        writeService.land(id, body)

    /** Returns the current writer's open (in-air) flight, if any. */
    @GetMapping("/my-open")
    fun myOpen(): FlightResponse? = writeService.findMyOpenFlight()
}
