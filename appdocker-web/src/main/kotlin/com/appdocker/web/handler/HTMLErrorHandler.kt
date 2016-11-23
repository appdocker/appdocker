package com.appdocker.web.handler

import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.ErrorHandler


class HTMLErrorHandler(val config: JsonObject?): Handler<RoutingContext> {

    val errorHandler: ErrorHandler

    init {

        val errorTemplate = config?.getString("template")?:
                ErrorHandler.DEFAULT_ERROR_HANDLER_TEMPLATE

        errorHandler = ErrorHandler.create(errorTemplate)
    }

    override fun handle(event: RoutingContext?) {
        errorHandler.handle(event)
    }

}