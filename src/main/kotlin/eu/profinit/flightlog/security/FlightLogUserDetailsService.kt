package eu.profinit.flightlog.security

import eu.profinit.flightlog.repository.entity.UserAccount
import eu.profinit.flightlog.repository.entity.UserRole
import eu.profinit.flightlog.repository.jpa.UserAccountJpa
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/** Spring Security adapter on top of [UserAccount]. */
@Service
class FlightLogUserDetailsService(
    private val users: UserAccountJpa,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val account = users.findFirstByEmailIgnoreCase(username)
            ?: throw UsernameNotFoundException("not found")
        val now = OffsetDateTime.now()
        val locked = account.lockedUntil?.isAfter(now) ?: false
        val authorities: List<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_${account.role.name}"))
        return User.withUsername(account.email)
            .password(account.passwordHash)
            .authorities(authorities)
            .accountLocked(locked)
            .disabled(false)
            .build()
    }

    fun loadAccount(email: String): UserAccount? = users.findFirstByEmailIgnoreCase(email)

    fun roleOf(account: UserAccount): UserRole = account.role
}
