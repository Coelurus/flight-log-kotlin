package eu.profinit.flightlog.integration

import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component
import org.springframework.web.context.WebApplicationContext

/** Request-scoped flag set when ClubDB results came from cache fallback (NFR-03). */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
open class DegradedModeHolder {
    var degraded: Boolean = false
        protected set
    var message: String? = null
        protected set

    open fun mark(msg: String = "Data nemusí být aktuální") {
        degraded = true
        message = msg
    }
}
