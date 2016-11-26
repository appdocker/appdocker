package com.appdocker.web.controller

import io.vertx.ext.web.handler.StaticHandler


class StaticResourceController : AbstractController() {


    override fun start() {

        val paths = config().getJsonArray(com.appdocker.web.staticRouterPaths) ?: return

        val handler = StaticHandler.create()

        val webRoot = config().getString(com.appdocker.web.staticRouterWebRoot)

        if(webRoot != null) {
            logger.info("webroot:$webRoot")
            handler.setWebRoot(webRoot)
        }

        for(path in paths) {
            logger.info("register static resource route :$path")

            val route = router!!.route(path as String)

            route.handler(handler)
        }
    }

}