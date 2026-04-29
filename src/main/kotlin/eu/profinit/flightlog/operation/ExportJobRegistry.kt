package eu.profinit.flightlog.operation

import eu.profinit.flightlog.model.FlightFilter
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

enum class ExportJobStatus { PENDING, READY, FAILED }

data class ExportJob(
    val id: String,
    @Volatile var status: ExportJobStatus,
    @Volatile var data: ByteArray? = null,
    @Volatile var error: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

/** Holds in-memory async CSV exports for >1000-row jobs (FR-06). */
@Service
class ExportJobRegistry(
    private val exportOperation: GetExportToCsvOperation,
) {
    private val jobs = ConcurrentHashMap<String, ExportJob>()
    private val rng = SecureRandom()

    fun submit(filter: FlightFilter): ExportJob {
        val id = randomId()
        val job = ExportJob(id, ExportJobStatus.PENDING)
        jobs[id] = job
        runAsync(job, filter)
        return job
    }

    fun get(id: String): ExportJob? = jobs[id]

    @Async
    open fun runAsync(job: ExportJob, filter: FlightFilter) {
        try {
            job.data = exportOperation.execute(filter)
            job.status = ExportJobStatus.READY
        } catch (ex: Exception) {
            job.error = ex.message; job.status = ExportJobStatus.FAILED
        }
    }

    private fun randomId(): String {
        val buf = ByteArray(12); rng.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }
}
