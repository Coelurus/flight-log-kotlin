package eu.profinit.flightlog.repository.impl

import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.repository.PersonRepository
import eu.profinit.flightlog.repository.TryGetResult
import eu.profinit.flightlog.repository.entity.Address
import eu.profinit.flightlog.repository.entity.Person
import eu.profinit.flightlog.repository.entity.PersonType
import eu.profinit.flightlog.repository.jpa.PersonJpa
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class PersonRepositoryImpl(
    private val personJpa: PersonJpa,
) : PersonRepository {

    override fun addGuestPerson(person: PersonModel): Long {
        val address = person.address?.let {
            Address().apply {
                city = it.city
                country = it.country
                postalCode = it.postalCode
                street = it.street
            }
        }
        val entity = Person().apply {
            this.address = address
            firstName = person.firstName
            lastName = person.lastName
            personType = PersonType.Guest
        }
        return personJpa.save(entity).id
    }

    override fun createClubMember(pilot: PersonModel): Long {
        val entity = Person().apply {
            firstName = pilot.firstName
            lastName = pilot.lastName
            personType = PersonType.ClubMember
            memberId = pilot.memberId
        }
        return personJpa.save(entity).id
    }

    @Transactional(readOnly = true)
    override fun tryGetPerson(personModel: PersonModel): TryGetResult {
        val found = personJpa.findFirstByMemberId(personModel.memberId)
        return if (found != null) TryGetResult(true, found.id) else TryGetResult(false, 0)
    }
}
