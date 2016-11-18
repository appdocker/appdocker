package io.iris.web.auth

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthHandler


class RedirectAuthHandler(val tag:String,authProvider:AuthProvider, config:JsonObject):AuthHandler {

    private val impl:AuthHandler

    init {
        impl = io.vertx.ext.web.handler.RedirectAuthHandler.create(
                authProvider,
                config.getString("loginRedirectURL","/login"))
    }

    override fun addAuthority(authority: String?): AuthHandler {
        impl.addAuthority(authority)

        return this
    }

    override fun addAuthorities(authorities: MutableSet<String>?): AuthHandler {
        impl.addAuthorities(authorities)

        return this
    }

    override fun handle(event: RoutingContext?) {
       impl.handle(event)
    }

    override fun toString(): String {
        return "RedirectAuthHandler:$tag"
    }
}