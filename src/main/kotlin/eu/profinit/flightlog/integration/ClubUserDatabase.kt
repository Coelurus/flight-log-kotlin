package eu.profinit.flightlog.integration

import eu.profinit.flightlog.model.PersonModel

class ClubUser(
    var memberId: Long = 0,
    var firstName: String = "",
    var lastName: String = "",
    var roles: List<String> = emptyList(),
) {
    fun toPersonModel(): PersonModel = PersonModel(
        memberId = memberId,
        firstName = firstName,
        lastName = lastName,
    )
}

data class TryGetClubUserResult(val found: Boolean, val person: PersonModel?)

interface ClubUserDatabase {
    fun getClubUsers(): List<PersonModel>
    fun tryGetClubUser(memberId: Long): TryGetClubUserResult
}
