package com.appdocker.http.controller

import com.appdocker.http.staticRouterPaths
import com.appdocker.http.staticRouterWebRoot
import io.vertx.ext.web.handler.StaticHandler


class StaticResourceController : AbstractController() {


    override fun start() {

        val paths = config().getJsonArray(staticRouterPaths) ?: return

        val handler = StaticHandler.create()

        val webRoot = config().getString(staticRouterWebRoot)

        if(webRoot != null) {
            logger.info("webroot:$webRoot")
            handler.setWebRoot(webRoot)
        }

        for(path in paths) {
            logger.info("register static resource route :$path")
            router!!.route(path as String).handler(handler)
        }
    }

}