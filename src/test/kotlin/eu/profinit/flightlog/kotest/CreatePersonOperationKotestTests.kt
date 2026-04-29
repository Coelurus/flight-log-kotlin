package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.integration.ClubUserDatabase
import eu.profinit.flightlog.integration.TryGetClubUserResult
import eu.profinit.flightlog.model.AddressModel
import eu.profinit.flightlog.model.PersonModel
import eu.profinit.flightlog.operation.CreatePersonOperation
import eu.profinit.flightlog.repository.PersonRepository
import eu.profinit.flightlog.repository.TryGetResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CreatePersonOperationKotestTests : DescribeSpec({

    describe("CreatePersonOperation.execute") {
        val personRepository = mockk<PersonRepository>(relaxed = true)
        val clubUserDatabase = mockk<ClubUserDatabase>(relaxed = true)
        val operation = CreatePersonOperation(personRepository, clubUserDatabase)

        it("returns null when person model is null") {
            operation.execute(null).shouldBeNull()
        }

        it("creates a guest when memberId == 0") {
            val person = PersonModel(
                memberId = 0,
                firstName = "John",
                lastName = "Smith",
                address = AddressModel(city = "NY", postalCode = "456", street = "2nd Ev", country = "USA"),
            )
            every { personRepository.addGuestPerson(person) } returns 10L

            operation.execute(person) shouldBe 10L
            verify(exactly = 1) { personRepository.addGuestPerson(person) }
        }

        it("returns id of an existing club member") {
            val person = PersonModel(memberId = 3, firstName = "Jan", lastName = "Novák")
            every { personRepository.tryGetPerson(person) } returns TryGetResult(true, 333)

            operation.execute(person) shouldBe 333L
        }

        it("creates a new club member fetched from ClubDB") {
            val person = PersonModel(memberId = 444, firstName = "Karl", lastName = "Lucemnburský")
            val clubUser = PersonModel(memberId = 444, firstName = "Karel", lastName = "Lucemburský")

            every { personRepository.tryGetPerson(person) } returns TryGetResult(false, 0)
            every { clubUserDatabase.tryGetClubUser(444) } returns TryGetClubUserResult(true, clubUser)
            every { personRepository.createClubMember(clubUser) } returns 4L

            operation.execute(person) shouldBe 4L
        }

        it("throws when neither local DB nor ClubDB has the person") {
            val person = PersonModel(memberId = 999, firstName = "Ghost", lastName = "User")
            every { personRepository.tryGetPerson(person) } returns TryGetResult(false, 0)
            every { clubUserDatabase.tryGetClubUser(999) } returns TryGetClubUserResult(false, null)

            shouldThrow<NoSuchElementException> { operation.execute(person) }
        }
    }
})
