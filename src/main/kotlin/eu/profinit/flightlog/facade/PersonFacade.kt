package eu.profinit.flightlog.facade

import eu.profinit.flightlog.integration.ClubUserDatabase
import eu.profinit.flightlog.model.PersonModel
import org.springframework.stereotype.Component

@Component
class PersonFacade(
    private val clubUserDatabase: ClubUserDatabase,
) {
    fun getClubMembers(): List<PersonModel> = clubUserDatabase.getClubUsers()
}
