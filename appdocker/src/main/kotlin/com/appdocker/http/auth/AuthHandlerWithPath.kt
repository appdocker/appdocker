package com.appdocker.http.auth

import io.vertx.core.json.JsonArray
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.handler.AuthHandler

class AuthHandlerWithPath(val handler: AuthHandler, val provider: AuthProvider)  {
    class Path(var origin:String) {
        private var extract = true
        init {
            if(origin.endsWith('*')) {
                extract = false
                origin = origin.removeSuffix("*")
            }
        }

        fun match(target:String):Boolean {
            if(extract) {
                return target.removeSuffix("/") == origin.removeSuffix("/")
            }

            return target.startsWith(origin)
        }

    }

    private val registerPaths = mutableListOf<Path>()

    /**
     * register new path
     */
    fun registerPaths(paths: JsonArray) {

        paths.forEach {
            path ->

            registerPaths.add(Path(path.toString()))
        }
    }

    fun match(target:String):Boolean {
        this.registerPaths.forEach {
            path ->

            if(path.match(target)) return true
        }

        return false
    }
}