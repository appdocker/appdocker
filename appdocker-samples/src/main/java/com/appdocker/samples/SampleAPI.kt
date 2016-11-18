package com.appdocker.samples

import com.appdocker.http.controller.AbstractController
import com.appdocker.http.annotations.Handler
import com.appdocker.http.annotations.Template
import io.vertx.ext.web.RoutingContext

class SampleAPI: AbstractController() {


    @Handler("/user/login")
    @Template("/login")
    fun login(context : RoutingContext) {
        context.put("hello",this)
        context.next()
    }

    @Handler
    fun logoff(context :RoutingContext) {
        context.response().putHeader("content-type","text/plain")
        context.response().end("{}")
    }
}