package com.appdocker.http.auth

import com.appdocker.http.auth.AuthHandlerWithPath
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.handler.AuthHandler
import java.util.concurrent.ConcurrentLinkedQueue


object AuthHandlers {
    private val handlers = ConcurrentLinkedQueue<AuthHandlerWithPath>()

    fun add(tag:String, provider: AuthProvider, handlerConfig: JsonObject, paths: JsonArray?) {

        val handlerClassName = handlerConfig.getString("name")?:throw IllegalArgumentException("'name' configuration property can't be null")

        val handlerClass = javaClass.classLoader.loadClass(handlerClassName)

        val constructor = handlerClass.getConstructor(String::class.java, AuthProvider::class.java, JsonObject::class.java)

        val config = handlerConfig.getJsonObject("config")?: JsonObject()

        val handler = constructor.newInstance(tag,provider,config) as AuthHandler

        val authHandlerWithPath = AuthHandlerWithPath(handler,provider)

        if(paths != null) {
            authHandlerWithPath.registerPaths(paths)
        } else {
            authHandlerWithPath.registerPaths(JsonArray("[\"/*\"]"))
        }


        handlers.add(authHandlerWithPath)
    }

    fun get(path:String): AuthHandler? {

        handlers.forEach {
            handler ->

            if(handler.match(path)) return handler.handler
        }

        return null
    }

    fun getProvider(path:String): AuthProvider? {

        handlers.forEach {
            handler ->

            if(handler.match(path)) return handler.provider
        }

        return null
    }
}
