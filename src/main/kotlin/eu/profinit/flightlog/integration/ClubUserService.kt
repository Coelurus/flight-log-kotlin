package eu.profinit.flightlog.integration

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.repository.entity.ClubUserSnapshot
import eu.profinit.flightlog.repository.jpa.ClubUserSnapshotJpa
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

/**
 * FR-12 + NFR-03 implementation:
 *  - 15 s timeout
 *  - 2 retries (2 s, 5 s)
 *  - short-term Caffeine cache (TTL 60 s)
 *  - long-term JPA-backed snapshot (refreshed on every successful API call)
 *  - falls back to long-term snapshot when API is unreachable; sets the
 *    request-scoped {@link DegradedModeHolder}.
 */
@Service
@Primary
class ClubUserService(
    @Value("\${flightlog.club-users-api}") private val baseUrl: String,
    private val snapshots: ClubUserSnapshotJpa,
    private val degraded: DegradedModeHolder,
    private val mapper: ObjectMapper,
) : ClubUserDatabase {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(15))
            setReadTimeout(Duration.ofSeconds(15))
        })
        .build()

    private val shortTerm: Cache<String, List<ClubUser>> = Caffeine.newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build()

    override fun getClubUsers(): List<PersonModel> = listAll().map { it.toPersonModel() }

    override fun tryGetClubUser(memberId: Long): TryGetClubUserResult {
        val u = listAll().firstOrNull { it.memberId == memberId }
        return TryGetClubUserResult(u != null, u?.toPersonModel())
    }

    fun listPilots(): List<PersonModel> =
        listAll().filter { isPilotRole(it.roles) }.map { it.toPersonModel() }

    fun isPilot(memberId: Long): Boolean =
        listAll().firstOrNull { it.memberId == memberId }?.let { isPilotRole(it.roles) } ?: false

    private fun isPilotRole(roles: List<String>) = roles.any { it.equals("PILOT", ignoreCase = true) }

    private fun listAll(): List<ClubUser> {
        shortTerm.getIfPresent(CACHE_KEY)?.let { return it }
        return try {
            val fresh = fetchFromApi()
            shortTerm.put(CACHE_KEY, fresh)
            persistSnapshot(fresh)
            fresh
        } catch (ex: Exception) {
            logger.warn("ClubDB unavailable, falling back to long-term cache: {}", ex.message)
            degraded.mark()
            loadSnapshot()
        }
    }

    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 2.5),
    )
    fun fetchFromApi(): List<ClubUser> {
        logger.debug("Fetching ClubDB users from {}", baseUrl)
        val raw = client.get().uri("/club/user").retrieve().body(String::class.java)
            ?: return emptyList()
        // The endpoint is a JSON array of users
        return mapper.readValue(raw)
    }

    @Transactional
    fun persistSnapshot(users: List<ClubUser>) {
        val now = OffsetDateTime.now()
        users.forEach {
            val snap = snapshots.findById(it.memberId).orElseGet { ClubUserSnapshot().apply { memberId = it.memberId } }
            snap.firstName = it.firstName
            snap.lastName = it.lastName
            snap.roles = it.roles.joinToString(",")
            snap.fetchedAt = now
            snapshots.save(snap)
        }
    }

    private fun loadSnapshot(): List<ClubUser> = snapshots.findAll().map {
        ClubUser(
            memberId = it.memberId,
            firstName = it.firstName.orEmpty(),
            lastName = it.lastName.orEmpty(),
            roles = it.roles.split(",").filter { r -> r.isNotBlank() },
        )
    }

    companion object {
        private const val CACHE_KEY = "all"
    }
}
