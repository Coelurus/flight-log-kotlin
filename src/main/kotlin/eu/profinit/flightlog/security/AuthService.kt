package eu.profinit.flightlog.security

import eu.profinit.flightlog.audit.AuditService
import eu.profinit.flightlog.repository.entity.AuditLevel
import eu.profinit.flightlog.repository.entity.PasswordResetToken
import eu.profinit.flightlog.repository.entity.UserAccount
import eu.profinit.flightlog.repository.entity.UserRole
import eu.profinit.flightlog.repository.jpa.PasswordResetTokenJpa
import eu.profinit.flightlog.repository.jpa.UserAccountJpa
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64

class BadCredentialsLoginException : RuntimeException("Neplatný e-mail nebo heslo.")
class CaptchaRequiredException : RuntimeException("Captcha required.")
class AccountLockedLoginException : RuntimeException("Account locked.")

data class LoginResponse(val email: String, val displayName: String?, val role: UserRole)

@Service
class AuthService(
    private val users: UserAccountJpa,
    private val tokens: PasswordResetTokenJpa,
    private val encoder: PasswordEncoder,
    private val attempts: LoginAttemptService,
    private val audit: AuditService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val rng = SecureRandom()

    @Transactional
    fun login(
        request: HttpServletRequest,
        email: String,
        password: String,
        captchaToken: String?,
    ): LoginResponse {
        val account = users.findFirstByEmailIgnoreCase(email)
        if (account == null) {
            // Generic message; do not reveal whether email exists.
            throw BadCredentialsLoginException()
        }
        if (attempts.isLocked(account)) {
            audit.log(account.id, account.email, "LOGIN_LOCKED", level = AuditLevel.WARN)
            throw AccountLockedLoginException()
        }
        if (attempts.requiresCaptcha(account) && captchaToken.isNullOrBlank()) {
            throw CaptchaRequiredException()
        }
        if (!encoder.matches(password, account.passwordHash)) {
            attempts.onFailure(account.email)
            audit.log(account.id, account.email, "LOGIN_FAILED", level = AuditLevel.WARN)
            throw BadCredentialsLoginException()
        }
        attempts.onSuccess(account.email)

        val auth = UsernamePasswordAuthenticationToken(
            account.email,
            null,
            listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_${account.role.name}")),
        )
        val ctx = SecurityContextHolder.createEmptyContext()
        ctx.authentication = auth
        SecurityContextHolder.setContext(ctx)
        val session = request.getSession(true)
        session.setAttribute("SPRING_SECURITY_CONTEXT", ctx)
        session.maxInactiveInterval = 30 * 60 // 30 minutes idle (FR-01)

        audit.log(account.id, account.email, "LOGIN_OK")
        return LoginResponse(account.email, account.displayName, account.role)
    }

    @Transactional
    fun changePassword(email: String, oldPassword: String, newPassword: String) {
        val account = users.findFirstByEmailIgnoreCase(email)
            ?: throw BadCredentialsLoginException()
        if (!encoder.matches(oldPassword, account.passwordHash)) throw BadCredentialsLoginException()
        validatePasswordStrength(newPassword)
        account.passwordHash = encoder.encode(newPassword)
        users.save(account)
        audit.log(account.id, account.email, "PASSWORD_CHANGED")
    }

    @Transactional
    fun requestPasswordReset(email: String): String? {
        val account = users.findFirstByEmailIgnoreCase(email) ?: return null
        val raw = randomToken()
        val token = PasswordResetToken().apply {
            userId = account.id
            tokenHash = sha256(raw)
            expiresAt = OffsetDateTime.now().plusHours(24)
        }
        tokens.save(token)
        audit.log(account.id, account.email, "PASSWORD_RESET_REQUESTED")
        // In a real system: send email. Here we return the raw token to the caller (logged at INFO in dev).
        logger.info("Password reset token for {} (valid 24h): {}", account.email, raw)
        return raw
    }

    @Transactional
    fun confirmPasswordReset(rawToken: String, newPassword: String) {
        val hash = sha256(rawToken)
        val token = tokens.findFirstByTokenHash(hash)
            ?: throw IllegalArgumentException("Invalid token")
        if (token.consumedAt != null) throw IllegalArgumentException("Token already used")
        if (token.expiresAt.isBefore(OffsetDateTime.now())) throw IllegalArgumentException("Token expired")
        validatePasswordStrength(newPassword)
        val account = users.findById(token.userId).orElseThrow { IllegalStateException("Missing user") }
        account.passwordHash = encoder.encode(newPassword)
        account.failedLoginAttempts = 0
        account.lockedUntil = null
        users.save(account)
        token.consumedAt = OffsetDateTime.now()
        tokens.save(token)
        audit.log(account.id, account.email, "PASSWORD_RESET_CONFIRMED")
    }

    private fun validatePasswordStrength(pw: String) {
        require(pw.length in 8..128) { "Heslo musí mít 8-128 znaků." }
    }

    private fun randomToken(): String {
        val buf = ByteArray(32)
        rng.nextBytes(buf)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}

/** Simple bootstrap seed of the admin/writer accounts so the app is usable. */
@org.springframework.stereotype.Component
class DefaultUserSeeder(
    private val users: UserAccountJpa,
    private val encoder: PasswordEncoder,
) : org.springframework.boot.ApplicationRunner {
    override fun run(args: org.springframework.boot.ApplicationArguments?) {
        if (users.findFirstByEmailIgnoreCase("admin@flightlog.local") == null) {
            users.save(UserAccount().apply {
                email = "admin@flightlog.local"; displayName = "Default Admin"
                passwordHash = encoder.encode("admin1234"); role = UserRole.ADMIN
            })
        }
        if (users.findFirstByEmailIgnoreCase("writer@flightlog.local") == null) {
            users.save(UserAccount().apply {
                email = "writer@flightlog.local"; displayName = "Default Writer"
                passwordHash = encoder.encode("writer1234"); role = UserRole.WRITER
            })
        }
    }
}
