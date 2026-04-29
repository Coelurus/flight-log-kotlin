package eu.profinit.flightlog.operation

import eu.profinit.flightlog.integration.ClubUserDatabase
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.repository.PersonRepository
import org.springframework.stereotype.Component

@Component
class CreatePersonOperation(
    private val personRepository: PersonRepository,
    private val clubUserDatabase: ClubUserDatabase,
) {
    companion object {
        private const val GUEST_ID = 0L
    }

    fun execute(personModel: PersonModel?): Long? {
        if (personModel == null) {
            return null
        }

        if (personModel.memberId == GUEST_ID) {
            return personRepository.addGuestPerson(personModel)
        }

        val tryPerson = personRepository.tryGetPerson(personModel)
        if (tryPerson.found) {
            return tryPerson.id
        }

        val tryClub = clubUserDatabase.tryGetClubUser(personModel.memberId)
        if (tryClub.found && tryClub.person != null) {
            return personRepository.createClubMember(tryClub.person)
        }

        throw NoSuchElementException("Person is not guest and Person not found in internal Database.")
    }
}
