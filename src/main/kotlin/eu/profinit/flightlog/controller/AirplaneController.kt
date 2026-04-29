package eu.profinit.flightlog.controller

import eu.profinit.flightlog.facade.AirplaneFacade
import eu.profinit.flightlog.model.AirplaneModel
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/airplane")
@CrossOrigin
class AirplaneController(
    private val airplaneFacade: AirplaneFacade,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO 3.1: Vystavte REST HTTP GET metodu vracející seznam klubových letadel.
    // Letadla získáte voláním airplaneFacade.
    // Dotazované URL je /airplane.
    // Odpověď by měla být kolekce AirplaneModel.
    @GetMapping
    fun getClubAirplanes(): List<AirplaneModel> {
        logger.debug("Get club airplanes.")
        return airplaneFacade.getClubAirplanes()
    }
}
