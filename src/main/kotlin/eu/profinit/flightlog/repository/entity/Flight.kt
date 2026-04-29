package eu.profinit.flightlog.repository.entity

import eu.profinit.flightlog.model.FlightModel
import eu.profinit.flightlog.model.FlightType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.OffsetDateTime

enum class StartType { WINCH, TOW }

@Entity
@Table(name = "flights")
class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var takeoffTime: OffsetDateTime = OffsetDateTime.now()
    var landingTime: OffsetDateTime? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "airplane_id")
    var airplane: Airplane? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pilot_id")
    var pilot: Person? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "copilot_id")
    var copilot: Person? = null

    var task: String? = null
    var note: String? = null

    @Enumerated(EnumType.STRING)
    var type: FlightType = FlightType.Glider

    @Enumerated(EnumType.STRING)
    @Column(name = "start_type")
    var startType: StartType? = null

    /** Self-FK linking the glider flight to its tow flight (and vice versa). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_flight_id")
    var linkedFlight: Flight? = null

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "updated_by")
    var updatedBy: String? = null

    @Column(name = "created_at")
    var createdAt: OffsetDateTime? = null

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime? = null

    @PrePersist
    fun onCreate() {
        val now = OffsetDateTime.now()
        if (createdAt == null) createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = OffsetDateTime.now()
    }

    fun toModel(): FlightModel = FlightModel(
        id = id,
        takeoffTime = takeoffTime,
        landingTime = landingTime,
        airplane = airplane?.toModel(),
        pilot = pilot?.toModel(),
        copilot = copilot?.toModel(),
        task = task,
    )
}

