package eu.profinit.flightlog.controller

import eu.profinit.flightlog.facade.PersonFacade
import eu.profinit.flightlog.model.PersonModel
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
@CrossOrigin
class UserController(
    private val personFacade: PersonFacade,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun get(): List<PersonModel> {
        logger.debug("Get club members.")
        return personFacade.getClubMembers()
    }
}
