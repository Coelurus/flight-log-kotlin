package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.integration.ClubUserDatabaseStub
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ClubUserDatabaseStubKotestTests : DescribeSpec({

    val stub = ClubUserDatabaseStub()

    describe("ClubUserDatabaseStub.getClubUsers") {
        it("returns the canned roster of pilots and back-office members") {
            val users = stub.getClubUsers()
            users.size shouldBe 6
            users.map { it.memberId } shouldContain 1L
            users.map { it.memberId } shouldContain 13L
        }
    }

    describe("ClubUserDatabaseStub.tryGetClubUser") {
        it("finds a known member id") {
            val r = stub.tryGetClubUser(2L)
            r.found shouldBe true
            r.person.shouldNotBeNull()
            r.person!!.firstName shouldBe "Naděžda"
        }
        it("does not find an unknown member id") {
            val r = stub.tryGetClubUser(9999L)
            r.found shouldBe false
            r.person.shouldBeNull()
        }
    }
})
