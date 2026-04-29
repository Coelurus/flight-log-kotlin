package eu.profinit.flightlog.controller

import eu.profinit.flightlog.model.AirplaneCategoryDto
import eu.profinit.flightlog.model.CreateFlightRequest
import eu.profinit.flightlog.model.CreateFlightResponse
import eu.profinit.flightlog.model.FlightFilter
import eu.profinit.flightlog.model.FlightResponse
import eu.profinit.flightlog.model.ImportConfirmRequest
import eu.profinit.flightlog.model.ImportPreviewResponse
import eu.profinit.flightlog.model.PagedResponse
import eu.profinit.flightlog.operation.ExportJobRegistry
import eu.profinit.flightlog.operation.ExportJobStatus
import eu.profinit.flightlog.operation.FlightImportService
import eu.profinit.flightlog.operation.FlightQueryService
import eu.profinit.flightlog.operation.FlightWriteService
import eu.profinit.flightlog.operation.GetExportToCsvOperation
import eu.profinit.flightlog.util.Formats
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/admin/flights")
@PreAuthorize("hasRole('ADMIN')")
class AdminFlightController(
    private val query: FlightQueryService,
    private val write: FlightWriteService,
    private val export: GetExportToCsvOperation,
    private val exportJobs: ExportJobRegistry,
    private val import: FlightImportService,
) {
    companion object {
        const val ASYNC_EXPORT_THRESHOLD = 1000
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) dateFrom: String?,
        @RequestParam(required = false) dateTo: String?,
        @RequestParam(required = false) category: AirplaneCategoryDto?,
        @RequestParam(required = false) immatriculation: String?,
        @RequestParam(required = false) takeoffFrom: String?,
        @RequestParam(required = false) takeoffTo: String?,
        @RequestParam(required = false) durationMin: Double?,
        @RequestParam(required = false) durationMax: Double?,
        @RequestParam(defaultValue = "false") inAirOnly: Boolean,
        @RequestParam(defaultValue = "false") landedOnly: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
    ): PagedResponse<FlightResponse> = query.list(
        FlightFilter(dateFrom, dateTo, category, immatriculation, takeoffFrom, takeoffTo,
            durationMin, durationMax, inAirOnly, landedOnly, page, size)
    )

    @PostMapping
    fun manualCreate(@Valid @RequestBody body: CreateFlightRequest): CreateFlightResponse =
        write.create(body)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody body: CreateFlightRequest): FlightResponse =
        write.update(id, body)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, @RequestParam(defaultValue = "false") cascade: Boolean): ResponseEntity<Void> {
        write.delete(id, cascade)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/export")
    fun export(
        @RequestParam(required = false) dateFrom: String?,
        @RequestParam(required = false) dateTo: String?,
        @RequestParam(required = false) category: AirplaneCategoryDto?,
        @RequestParam(required = false) immatriculation: String?,
        @RequestParam(required = false) takeoffFrom: String?,
        @RequestParam(required = false) takeoffTo: String?,
        @RequestParam(required = false) durationMin: Double?,
        @RequestParam(required = false) durationMax: Double?,
        @RequestParam(defaultValue = "false") inAirOnly: Boolean,
        @RequestParam(defaultValue = "false") landedOnly: Boolean,
    ): ResponseEntity<*> {
        val filter = FlightFilter(dateFrom, dateTo, category, immatriculation, takeoffFrom, takeoffTo,
            durationMin, durationMax, inAirOnly, landedOnly, page = 0, size = Int.MAX_VALUE)
        val total = query.findAll(filter).size
        if (total > ASYNC_EXPORT_THRESHOLD) {
            val job = exportJobs.submit(filter)
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapOf("jobId" to job.id))
        }
        val csv = export.execute(filter)
        val filename = "flightlog_export_${Formats.formatDate(OffsetDateTime.now())}.csv"
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(ByteArrayResource(csv))
    }

    @GetMapping("/exports/{jobId}")
    fun exportStatus(@PathVariable jobId: String): ResponseEntity<*> {
        val job = exportJobs.get(jobId) ?: return ResponseEntity.notFound().build<Any>()
        return when (job.status) {
            ExportJobStatus.PENDING -> ResponseEntity.ok(mapOf("status" to "pending"))
            ExportJobStatus.FAILED -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("status" to "failed", "error" to job.error))
            ExportJobStatus.READY -> {
                val filename = "flightlog_export_${Formats.formatDate(OffsetDateTime.now())}.csv"
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
                    .body(ByteArrayResource(job.data ?: ByteArray(0)))
            }
        }
    }

    @PostMapping("/import")
    fun importPreview(@RequestParam("file") file: MultipartFile): ResponseEntity<ImportPreviewResponse> {
        val preview = import.preview(file.inputStream)
        val status = if (preview.errors.isEmpty()) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(preview)
    }

    @PostMapping("/import/confirm")
    fun importConfirm(@Valid @RequestBody body: ImportConfirmRequest): ResponseEntity<Map<String, Any>> {
        val count = import.confirm(body.confirmationToken)
        return ResponseEntity.ok(mapOf("imported" to count))
    }
}
