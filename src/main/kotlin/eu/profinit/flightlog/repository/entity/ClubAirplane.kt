package eu.profinit.flightlog.repository.entity

import eu.profinit.flightlog.model.AirplaneModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "club_airplanes")
class ClubAirplane {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var immatriculation: String = ""

    @ManyToOne(fetch = FetchType.EAGER, cascade = [jakarta.persistence.CascadeType.ALL])
    @JoinColumn(name = "airplane_type_id")
    var airplaneType: AirplaneType? = null

    var archive: Boolean = false

    fun toModel(): AirplaneModel = AirplaneModel(
        id = id,
        immatriculation = immatriculation,
        type = airplaneType?.type.orEmpty(),
    )
}
