package eu.profinit.flightlog.kotest

import eu.profinit.flightlog.util.Formats
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class FormatsKotestTests : DescribeSpec({

    describe("Formats.parseDate") {
        it("parses a valid DD-MM-YYYY date") {
            Formats.parseDate("28-04-2026") shouldBe LocalDate.of(2026, 4, 28)
        }
        it("rejects an invalid date") {
            shouldThrow<IllegalArgumentException> { Formats.parseDate("2026-04-28") }
        }
    }

    describe("Formats.parseTime") {
        it("parses a valid HH:mm time") {
            Formats.parseTime("07:30") shouldBe LocalTime.of(7, 30)
        }
        it("rejects an invalid time") {
            shouldThrow<IllegalArgumentException> { Formats.parseTime("25:99") }
        }
    }

    describe("Formats.combine") {
        it("combines date and time at UTC offset") {
            val dt = Formats.combine(LocalDate.of(2026, 4, 28), LocalTime.of(7, 30))
            dt shouldBe OffsetDateTime.of(2026, 4, 28, 7, 30, 0, 0, ZoneOffset.UTC)
        }
    }

    describe("Formats.formatDuration") {
        it("formats with two decimals using a US locale") {
            Formats.formatDuration(1.5) shouldBe "1.50"
            Formats.formatDuration(0.0) shouldBe "0.00"
            Formats.formatDuration(2.345) shouldBe "2.35"
        }
    }

    describe("Formats.formatDate / formatTime") {
        it("formats an OffsetDateTime in CZ patterns") {
            val dt = OffsetDateTime.of(2026, 4, 28, 7, 30, 0, 0, ZoneOffset.UTC)
            Formats.formatDate(dt) shouldBe "28-04-2026"
            Formats.formatTime(dt) shouldBe "07:30"
        }
    }
})
