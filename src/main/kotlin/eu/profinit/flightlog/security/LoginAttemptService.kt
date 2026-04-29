package eu.profinit.flightlog.security

import eu.profinit.flightlog.repository.entity.UserAccount
import eu.profinit.flightlog.repository.jpa.UserAccountJpa
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** Tracks failed login attempts per account (NFR-06). */
@Service
class LoginAttemptService(
    private val users: UserAccountJpa,
) {
    companion object {
        const val MAX_ATTEMPTS = 3
        val LOCK_DURATION_MINUTES: Long = 15
    }

    @Transactional
    fun onFailure(email: String) {
        val account = users.findFirstByEmailIgnoreCase(email) ?: return
        account.failedLoginAttempts += 1
        if (account.failedLoginAttempts >= MAX_ATTEMPTS) {
            account.lockedUntil = OffsetDateTime.now().plusMinutes(LOCK_DURATION_MINUTES)
        }
        users.save(account)
    }

    @Transactional
    fun onSuccess(email: String) {
        val account = users.findFirstByEmailIgnoreCase(email) ?: return
        account.failedLoginAttempts = 0
        account.lockedUntil = null
        users.save(account)
    }

    fun isLocked(account: UserAccount): Boolean =
        account.lockedUntil?.isAfter(OffsetDateTime.now()) ?: false

    fun requiresCaptcha(account: UserAccount): Boolean =
        account.failedLoginAttempts >= MAX_ATTEMPTS
}
