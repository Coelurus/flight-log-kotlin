package eu.profinit.flightlog.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.OffsetDateTime

enum class UserRole { WRITER, ADMIN }

@Entity
@Table(
    name = "users",
    indexes = [Index(name = "idx_users_email", columnList = "email", unique = true)],
)
class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "email", nullable = false, length = 254)
    var email: String = ""

    @Column(name = "display_name", length = 100)
    var displayName: String? = null

    @Column(name = "password_hash", nullable = false, length = 200)
    var passwordHash: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: UserRole = UserRole.WRITER

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0

    @Column(name = "locked_until")
    var lockedUntil: OffsetDateTime? = null

    @Column(name = "created_at")
    var createdAt: OffsetDateTime = OffsetDateTime.now()
}

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = [Index(name = "idx_prt_hash", columnList = "token_hash", unique = true)],
)
class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(name = "token_hash", nullable = false, length = 100)
    var tokenHash: String = ""

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime = OffsetDateTime.now()

    @Column(name = "consumed_at")
    var consumedAt: OffsetDateTime? = null
}

enum class AuditLevel { INFO, WARN, ERROR }

@Entity
@Table(
    name = "audit_log",
    indexes = [
        Index(name = "idx_audit_ts", columnList = "ts"),
        Index(name = "idx_audit_user", columnList = "user_id"),
    ],
)
class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "user_id")
    var userId: Long? = null

    @Column(name = "user_email", length = 254)
    var userEmail: String? = null

    @Column(name = "ts", nullable = false)
    var timestamp: OffsetDateTime = OffsetDateTime.now()

    @Column(name = "action", nullable = false, length = 60)
    var action: String = ""

    @Column(name = "entity_type", length = 60)
    var entityType: String? = null

    @Column(name = "entity_id")
    var entityId: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 10)
    var level: AuditLevel = AuditLevel.INFO

    @Lob
    @Column(name = "details")
    var details: String? = null
}

/** Long-term ClubDB cache row (FR-12 fallback). */
@Entity
@Table(name = "club_user_snapshot")
class ClubUserSnapshot {
    @Id
    @Column(name = "member_id")
    var memberId: Long = 0

    @Column(name = "first_name", length = 100)
    var firstName: String? = null

    @Column(name = "last_name", length = 100)
    var lastName: String? = null

    @Column(name = "roles", length = 200)
    var roles: String = ""

    @Column(name = "fetched_at", nullable = false)
    var fetchedAt: OffsetDateTime = OffsetDateTime.now()
}
