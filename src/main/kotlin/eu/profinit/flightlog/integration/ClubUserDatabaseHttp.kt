package eu.profinit.flightlog.integration

import eu.profinit.flightlog.model.PersonModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(prefix = "flightlog.integration", name = ["use-stub"], havingValue = "false")
class ClubUserDatabaseHttp(
    @Value("\${flightlog.club-users-api}") private val baseUrl: String,
) : ClubUserDatabase {

    private val client: RestClient = RestClient.builder().baseUrl(baseUrl).build()

    override fun getClubUsers(): List<PersonModel> {
        val users = receiveClubUsers()
        return transformToPersonModel(users)
    }

    override fun tryGetClubUser(memberId: Long): TryGetClubUserResult {
        val person = getClubUsers().firstOrNull { it.memberId == memberId }
        return TryGetClubUserResult(person != null, person)
    }

    // TODO 8.2: Naimplementujte volání endpointu ClubDB pomocí RestClient
    private fun receiveClubUsers(): List<ClubUser> = emptyList()

    private fun transformToPersonModel(users: List<ClubUser>): List<PersonModel> =
        users.map { it.toPersonModel() }
}
