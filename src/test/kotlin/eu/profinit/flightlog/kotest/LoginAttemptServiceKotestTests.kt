package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.repository.entity.UserAccount
import eu.profinit.flightlog.repository.jpa.UserAccountJpa
import eu.profinit.flightlog.security.LoginAttemptService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.OffsetDateTime

class LoginAttemptServiceKotestTests : DescribeSpec({

    fun newAccount(email: String = "u@example.com"): UserAccount = UserAccount().apply {
        this.email = email
        this.failedLoginAttempts = 0
        this.lockedUntil = null
    }

    describe("LoginAttemptService.onFailure") {
        it("does nothing when account is unknown") {
            val users = mockk<UserAccountJpa>(relaxed = true)
            every { users.findFirstByEmailIgnoreCase(any()) } returns null
            val svc = LoginAttemptService(users)

            svc.onFailure("missing@example.com")

            verify(exactly = 0) { users.save(any()) }
        }

        it("increments counter without locking under threshold") {
            val users = mockk<UserAccountJpa>(relaxed = true)
            val acc = newAccount().apply { failedLoginAttempts = 1 }
            every { users.findFirstByEmailIgnoreCase("u@example.com") } returns acc
            every { users.save(any<UserAccount>()) } answers { firstArg() }
            val svc = LoginAttemptService(users)

            svc.onFailure("u@example.com")

            acc.failedLoginAttempts shouldBe 2
            (acc.lockedUntil == null) shouldBe true
            verify(exactly = 1) { users.save(acc) }
        }

        it("locks the account when threshold is reached") {
            val users = mockk<UserAccountJpa>(relaxed = true)
            val acc = newAccount().apply { failedLoginAttempts = LoginAttemptService.MAX_ATTEMPTS - 1 }
            every { users.findFirstByEmailIgnoreCase(any()) } returns acc
            every { users.save(any<UserAccount>()) } answers { firstArg() }
            val svc = LoginAttemptService(users)

            svc.onFailure("u@example.com")

            acc.failedLoginAttempts shouldBe LoginAttemptService.MAX_ATTEMPTS
            (acc.lockedUntil != null && acc.lockedUntil!!.isAfter(OffsetDateTime.now())) shouldBe true
        }
    }

    describe("LoginAttemptService.onSuccess") {
        it("clears the failure counter and lock") {
            val users = mockk<UserAccountJpa>(relaxed = true)
            val acc = newAccount().apply {
                failedLoginAttempts = 5
                lockedUntil = OffsetDateTime.now().plusMinutes(30)
            }
            every { users.findFirstByEmailIgnoreCase(any()) } returns acc
            every { users.save(any<UserAccount>()) } answers { firstArg() }
            val svc = LoginAttemptService(users)

            svc.onSuccess("u@example.com")

            acc.failedLoginAttempts shouldBe 0
            (acc.lockedUntil == null) shouldBe true
            verify(exactly = 1) { users.save(acc) }
        }
    }

    describe("LoginAttemptService.isLocked / requiresCaptcha") {
        val svc = LoginAttemptService(mockk(relaxed = true))

        it("isLocked is true while lockedUntil is in the future") {
            val acc = newAccount().apply { lockedUntil = OffsetDateTime.now().plusMinutes(5) }
            svc.isLocked(acc) shouldBe true
        }
        it("isLocked is false once the lock has expired") {
            val acc = newAccount().apply { lockedUntil = OffsetDateTime.now().minusMinutes(5) }
            svc.isLocked(acc) shouldBe false
        }
        it("isLocked is false when no lock is set") {
            svc.isLocked(newAccount()) shouldBe false
        }
        it("requiresCaptcha kicks in at MAX_ATTEMPTS") {
            val acc = newAccount().apply { failedLoginAttempts = LoginAttemptService.MAX_ATTEMPTS }
            svc.requiresCaptcha(acc) shouldBe true
            svc.requiresCaptcha(newAccount()) shouldBe false
        }
    }
})
