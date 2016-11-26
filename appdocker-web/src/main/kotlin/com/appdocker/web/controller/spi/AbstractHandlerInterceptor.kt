package com.appdocker.web.controller.spi

import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import java.lang.reflect.Method
import java.util.*


open class AbstractHandlerInterceptor : HandlerInterceptor {

    private val logger: Logger

    init {
        logger = LoggerFactory.getLogger(javaClass)
    }


    protected fun createRoutes(router: Router, fullPath: String, httpMethod: com.appdocker.web.annotations.Method?): ArrayList<Route> {

        val routes = arrayListOf<Route>()

        if ( httpMethod != null ) {
            httpMethod.allowedMethods.forEach { httpMethod ->
                logger.info("register path method($httpMethod) :$fullPath")
                routes.add(router.route(httpMethod, fullPath))
            }
        } else {
            logger.info("register path generic method :$fullPath")
            routes.add(router.route(fullPath))
        }

        return routes
    }

    override fun before(controller: JsonObject, router: Router, routePath: String, method: Method) {
    }

    override fun after(controller: JsonObject, router: Router, routePath: String, handler: Method) {
    }
}