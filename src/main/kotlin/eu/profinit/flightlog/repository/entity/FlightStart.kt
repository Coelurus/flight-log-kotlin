package eu.profinit.flightlog.repository.entity

import eu.profinit.flightlog.model.ReportModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "flight_starts")
class FlightStart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "towplane_flight_id", nullable = false)
    var towplane: Flight? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "glider_flight_id")
    var glider: Flight? = null

    fun toModel(): ReportModel = ReportModel(
        towplane = towplane?.toModel(),
        glider = glider?.toModel(),
    )
}
