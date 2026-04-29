package eu.profinit.flightlog.integration

import eu.profinit.flightlog.model.PersonModel
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "flightlog.integration", name = ["use-stub"], havingValue = "true", matchIfMissing = true)
class ClubUserDatabaseStub : ClubUserDatabase {

    override fun getClubUsers(): List<PersonModel> =
        receiveClubUsers().map {
            PersonModel(
                memberId = it.memberId,
                firstName = it.firstName,
                lastName = it.lastName,
            )
        }

    override fun tryGetClubUser(memberId: Long): TryGetClubUserResult {
        val person = getClubUsers().firstOrNull { it.memberId == memberId }
        return TryGetClubUserResult(person != null, person)
    }

    private fun receiveClubUsers(): List<ClubUser> = listOf(
        ClubUser(1L, "Kamila", "Spoustová", listOf("PILOT")),
        ClubUser(2L, "Naděžda", "Pavelková", listOf("PILOT")),
        ClubUser(3L, "Silvie", "Hronová", listOf("PILOT")),
        ClubUser(9L, "Miloš", "Korbel", listOf("PILOT", "BACKOFFICE")),
        ClubUser(10L, "Petr", "Hrubec", listOf("PILOT", "BACKOFFICE")),
        ClubUser(13L, "Michal", "Vyvlečka", listOf("BACKOFFICE")),
    )
}
