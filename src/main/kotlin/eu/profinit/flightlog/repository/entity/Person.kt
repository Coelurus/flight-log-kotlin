package eu.profinit.flightlog.repository.entity

import eu.profinit.flightlog.model.PersonModel
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

enum class PersonType { ClubMember, Guest }

@Entity
@Table(name = "persons")
class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Enumerated(EnumType.STRING)
    var personType: PersonType = PersonType.ClubMember

    var firstName: String? = null
    var lastName: String? = null

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id")
    var address: Address? = null

    var memberId: Long = 0

    fun toModel(): PersonModel = PersonModel(
        memberId = memberId,
        firstName = firstName,
        lastName = lastName,
        address = address?.toModel(),
    )
}
