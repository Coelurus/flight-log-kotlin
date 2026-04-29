package eu.profinit.flightlog.repository.entity

import eu.profinit.flightlog.model.AddressModel
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "addresses")
class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var street: String? = null
    var city: String? = null
    var postalCode: String? = null
    var country: String? = null

    fun toModel(): AddressModel = AddressModel(
        street = street,
        city = city,
        postalCode = postalCode,
        country = country,
    )
}
