package eu.profinit.flightlog.operation

import eu.profinit.flightlog.integration.ClubUserDatabase
import eu.profinit.flightlog.integration.TryGetClubUserResult
import eu.profinit.flightlog.model.AddressModel
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.repository.PersonRepository
import eu.profinit.flightlog.repository.TryGetResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class CreatePersonOperationTests {

    private lateinit var personRepository: PersonRepository
    private lateinit var clubUserDatabase: ClubUserDatabase
    private lateinit var operation: CreatePersonOperation

    @BeforeEach
    fun setUp() {
        personRepository = mock()
        clubUserDatabase = mock()
        operation = CreatePersonOperation(personRepository, clubUserDatabase)
    }

    @Test
    fun `execute should return null for null input`() {
        val result = operation.execute(null)
        assertNull(result)
        verifyNoMoreInteractions(personRepository, clubUserDatabase)
    }

    @Test
    fun `execute should create guest`() {
        val person = PersonModel(
            memberId = 0,
            address = AddressModel(city = "NY", postalCode = "456", street = "2nd Ev", country = "USA"),
            firstName = "John",
            lastName = "Smith",
        )
        whenever(personRepository.addGuestPerson(person)).thenReturn(10L)

        val result = operation.execute(person)

        assertNotNull(result)
        assertTrue(result!! > 0)
    }

    @Test
    fun `execute should return existing club member`() {
        val person = PersonModel(memberId = 3, firstName = "Jan", lastName = "Novák")
        whenever(personRepository.tryGetPerson(person)).thenReturn(TryGetResult(true, 333))

        val result = operation.execute(person)

        assertEquals(333L, result)
    }

    @Test
    fun `execute should create new club member`() {
        val person = PersonModel(memberId = 444, firstName = "Karl", lastName = "Lucemnburský")
        val clubUser = PersonModel(memberId = 444, firstName = "Karel", lastName = "Lucemburský")

        whenever(personRepository.tryGetPerson(person)).thenReturn(TryGetResult(false, 0))
        whenever(clubUserDatabase.tryGetClubUser(444)).thenReturn(TryGetClubUserResult(true, clubUser))
        whenever(personRepository.createClubMember(any())).thenReturn(4L)

        val result = operation.execute(person)

        assertEquals(4L, result)
    }
}
