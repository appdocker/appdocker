package com.appdocker.http

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.TemplateEngine
import java.util.concurrent.ConcurrentHashMap


internal object HttpContext {
    internal class HttpServerShared(val router: Router, val templateEngine: TemplateEngine)

    internal val routerMap = ConcurrentHashMap<Vertx, HttpServerShared>()

    fun add(vertx:Vertx,shared:HttpServerShared) {
        routerMap[vertx] = shared
    }

    fun getShared(vertx: Vertx) :HttpServerShared? {
        return routerMap[vertx]
    }
}