package com.appdocker.samples

import com.appdocker.web.annotations.Handler
import com.appdocker.web.annotations.Template
import com.appdocker.web.controller.ControllerVerticle
import io.vertx.ext.web.RoutingContext

class SampleAPI: ControllerVerticle() {

    @Handler
    @Template("/login")
    fun login(context : RoutingContext, echoServices: Array<EchoService>) {
        context.put("hello","v1")
        context.next()
    }

    @Handler
    @Template("/login")
    fun loginV2(context : RoutingContext, echoService:EchoService) {
        context.put("hello","v2")
        context.next()
    }

    @Handler
    fun logoff(context :RoutingContext) {
        context.response().putHeader("content-type","text/plain")
        context.response().end("{}")
    }
}