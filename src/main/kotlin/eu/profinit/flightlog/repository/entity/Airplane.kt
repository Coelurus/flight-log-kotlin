package eu.profinit.flightlog.repository.entity

import eu.profinit.flightlog.model.AirplaneModel
import jakarta.persistence.CascadeType
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
import jakarta.persistence.Table

enum class AirplaneCategory { GLIDER, POWERED }

@Entity
@Table(name = "airplanes")
class Airplane {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.MERGE])
    @JoinColumn(name = "club_airplane_id")
    var clubAirplane: ClubAirplane? = null

    var guestAirplaneImmatriculation: String? = null
    var guestAirplaneType: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    var category: AirplaneCategory? = null

    fun toModel(): AirplaneModel = AirplaneModel(
        id = id,
        immatriculation = clubAirplane?.immatriculation ?: guestAirplaneImmatriculation.orEmpty(),
        type = clubAirplane?.airplaneType?.type ?: guestAirplaneType.orEmpty(),
    )
}

