package com.appdocker.web

import com.appdocker.AppDockerContext
import com.appdocker.causeWithStackTrace
import io.netty.handler.codec.http2.Http2Error
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.TemplateEngine

/**
 * The http server verticle
 */
class HttpServerVerticle : AbstractVerticle() {
    private var logger = LoggerFactory.getLogger(HttpServerVerticle::class.java)

    private var server: HttpServer? = null

    private var router: Router? = null

    private var templateEngine: TemplateEngine? = null

    override fun start(startFuture: Future<Void>?) {

        this.server = vertx.createHttpServer()

        createShared()

        willLoadRouters()

        loadRouters()

        server!!.listen(config().getInteger("port",8080)) {
            startFuture!!.complete()
        }
    }

    private fun willLoadRouters() {
        // add logger handler
        if(config().getBoolean("request.log",false)) {
            router!!.route().handler(io.vertx.ext.web.handler.LoggerHandler.create())
        }

        if(config().getBoolean("cookie",false)) {
            router!!.route().handler(io.vertx.ext.web.handler.CookieHandler.create())
        }

        router!!.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    }

    private fun didLoadRouters() {

        // error handlers

        router!!.route().handler {
            context ->

            // handler 404
            logger.debug(context.normalisedPath())

            context.fail(404)
        }

        val errorHandlers = config().getJsonArray("error.handlers")?:return

        errorHandlers.forEach {
            handlerConfig ->

            loadErrorHandler(handlerConfig as JsonObject)
        }

//        var errorTemplate = io.vertx.ext.web.handler.ErrorHandler.DEFAULT_ERROR_HANDLER_TEMPLATE
//
//        if(config().getString("error.template") != null) {
//            errorTemplate = config().getString("error.template")
//        }
//
//        router!!.route().failureHandler(io.vertx.ext.web.handler.ErrorHandler.create(errorTemplate))
    }

    private fun loadErrorHandler(config : JsonObject) {

        val mainClassName = config.getString("main")?:
                throw IllegalArgumentException("error.handler config must define 'main' class ")

        val routePath = config.getString("path")?:
                throw IllegalArgumentException("error.handler config must define route 'path' ")

        val mainClass = Class.forName(mainClassName)

        val constructor = mainClass.getConstructor(JsonObject::class.java)

        logger.debug("register failure handler for route path :$routePath")

        @Suppress("UNCHECKED_CAST")
        val handler = constructor.newInstance(config.getJsonObject("config")) as Handler<RoutingContext>?

        router!!.route(routePath).failureHandler(handler)

        logger.debug("register failure handler for route path :$routePath -- success")
    }

    private fun loadRouter(routers: JsonArray, idx:Int) {

        if(routers.size() == idx) {
            didLoadRouters()

            return
        }

        val routerName = routers.getString(idx)

        logger.info("register router:$routerName ...")

        vertx.deployVerticle("service:$routerName") {

            result ->

            if (result.succeeded()) {
                logger.info("register router:$routerName -- succeeded")
            } else {

                logger.error("register router:$routerName -- failed:${result.causeWithStackTrace()}")

                System.exit(1)
            }

            loadRouter(routers,idx + 1)
        }
    }

    private fun loadRouters() {
        loadRouter(config().getJsonArray(configControllers),0)
    }

    private fun loadTemplateEngine(): TemplateEngine {
        // load template engine

        val templateClassName = config().getString("template")?:"io.vertx.ext.web.templ.MVELTemplateEngine"

        val templateEngineClass = Class.forName(templateClassName)

        return  templateEngineClass.getDeclaredMethod("create").invoke(null) as TemplateEngine
    }


    private fun createShared() {
        router = Router.router(vertx)

        templateEngine = loadTemplateEngine()

        AppDockerContext.sharedMap.put(appdockerHttpServerTemplate,templateEngine!!)

        AppDockerContext.sharedMap.put(appdockerHttpServerRouter,router!!)

        this.server!!.requestHandler {
            require ->
            router!!.accept(require)
        }
    }
}