@file:JvmName("HandlerInterceptor")


package com.appdocker.web.controller.spi

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router


interface HandlerInterceptor {

    fun name(): String {
        return this.javaClass.name
    }

    fun before(controller: JsonObject, router: Router, routePath: String, method: java.lang.reflect.Method)

    fun after(controller: JsonObject, router: Router, routePath: String, handler: java.lang.reflect.Method)
}