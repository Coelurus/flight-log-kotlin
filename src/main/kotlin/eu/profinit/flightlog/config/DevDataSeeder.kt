package eu.profinit.flightlog.config

import eu.profinit.flightlog.TestDatabaseGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("dev")
class DevDataSeeder {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun seedRunner(
        generator: TestDatabaseGenerator,
        @Value("\${flightlog.seed-test-data:false}") seed: Boolean,
    ): ApplicationRunner = ApplicationRunner {
        if (seed) {
            logger.info("Seeding development database with test data.")
            generator.renewDatabase()
        }
    }
}
