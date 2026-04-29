package eu.profinit.flightlog.audit

import eu.profinit.flightlog.repository.entity.AuditLevel
import eu.profinit.flightlog.repository.entity.AuditLog
import eu.profinit.flightlog.repository.jpa.AuditLogJpa
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class AuditService(
    private val repo: AuditLogJpa,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        userId: Long?,
        userEmail: String?,
        action: String,
        entityType: String? = null,
        entityId: Long? = null,
        details: String? = null,
        level: AuditLevel = AuditLevel.INFO,
    ) {
        val row = AuditLog().apply {
            this.userId = userId
            this.userEmail = userEmail
            this.action = action
            this.entityType = entityType
            this.entityId = entityId
            this.details = details
            this.level = level
            this.timestamp = OffsetDateTime.now()
        }
        repo.save(row)
        when (level) {
            AuditLevel.INFO -> logger.info("AUDIT {} user={} entity={}#{}", action, userEmail, entityType, entityId)
            AuditLevel.WARN -> logger.warn("AUDIT {} user={} entity={}#{}", action, userEmail, entityType, entityId)
            AuditLevel.ERROR -> logger.error("AUDIT {} user={} entity={}#{}", action, userEmail, entityType, entityId)
        }
    }

    fun logCurrent(
        action: String,
        entityType: String? = null,
        entityId: Long? = null,
        details: String? = null,
        level: AuditLevel = AuditLevel.INFO,
    ) {
        val email = SecurityContextHolder.getContext().authentication?.name
        log(null, email, action, entityType, entityId, details, level)
    }

    /** NFR-09: keep audit_log rows for at least 12 months. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    fun purgeOlderThanRetention() {
        val cutoff = OffsetDateTime.now().minusMonths(13)
        val deleted = repo.deleteOlderThan(cutoff)
        if (deleted > 0) logger.info("Purged {} audit_log rows older than {}", deleted, cutoff)
    }
}
