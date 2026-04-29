package eu.profinit.flightlog.repository.jpa

import eu.profinit.flightlog.repository.entity.Airplane
import eu.profinit.flightlog.repository.entity.AuditLog
import eu.profinit.flightlog.repository.entity.ClubAirplane
import eu.profinit.flightlog.repository.entity.ClubUserSnapshot
import eu.profinit.flightlog.repository.entity.Flight
import eu.profinit.flightlog.repository.entity.FlightStart
import eu.profinit.flightlog.repository.entity.PasswordResetToken
import eu.profinit.flightlog.repository.entity.Person
import eu.profinit.flightlog.repository.entity.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface AirplaneJpa : JpaRepository<Airplane, Long> {
    @Query(
        """
        select a from Airplane a
        left join fetch a.clubAirplane ca
        left join fetch ca.airplaneType
        where lower(coalesce(ca.immatriculation, a.guestAirplaneImmatriculation)) = lower(:imm)
        """
    )
    fun findByImmatriculationIgnoreCase(@Param("imm") imm: String): Airplane?
}

interface ClubAirplaneJpa : JpaRepository<ClubAirplane, Long> {
    @Query("select ca from ClubAirplane ca join fetch ca.airplaneType")
    fun findAllWithType(): List<ClubAirplane>
}

interface PersonJpa : JpaRepository<Person, Long> {
    fun findFirstByMemberId(memberId: Long): Person?
    fun findFirstByLastNameIgnoreCase(lastName: String): Person?
}

interface FlightJpa : JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {
    @Query(
        """
        select f from Flight f
            left join fetch f.airplane a
            left join fetch a.clubAirplane ca
            left join fetch ca.airplaneType
            left join fetch f.pilot
        where f.landingTime is null and f.createdBy = :user
        order by f.takeoffTime desc
        """
    )
    fun findOpenByCreator(@Param("user") user: String): List<Flight>
}

interface FlightStartJpa : JpaRepository<FlightStart, Long> {
    @Query(
        """
        select fs from FlightStart fs
            left join fetch fs.glider g
            left join fetch g.airplane
            left join fetch g.pilot
            left join fetch g.copilot
            left join fetch fs.towplane t
            left join fetch t.airplane
            left join fetch t.pilot
            left join fetch t.copilot
        order by t.takeoffTime desc
        """
    )
    fun findAllForReport(): List<FlightStart>
}

interface UserAccountJpa : JpaRepository<UserAccount, Long> {
    fun findFirstByEmailIgnoreCase(email: String): UserAccount?
}

interface PasswordResetTokenJpa : JpaRepository<PasswordResetToken, Long> {
    fun findFirstByTokenHash(tokenHash: String): PasswordResetToken?
}

interface AuditLogJpa : JpaRepository<AuditLog, Long> {
    @Modifying
    @Query("delete from AuditLog a where a.timestamp < :before")
    fun deleteOlderThan(@Param("before") before: OffsetDateTime): Int
}

interface ClubUserSnapshotJpa : JpaRepository<ClubUserSnapshot, Long>

