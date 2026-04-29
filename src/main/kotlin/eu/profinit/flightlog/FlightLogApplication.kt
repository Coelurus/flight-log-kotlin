package eu.profinit.flightlog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlightLogApplication

fun main(args: Array<String>) {
    runApplication<FlightLogApplication>(*args)
}
