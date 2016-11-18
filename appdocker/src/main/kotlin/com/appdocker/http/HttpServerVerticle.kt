package com.appdocker.http

import com.appdocker.causeWithStackTrace
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonArray
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.ErrorHandler
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

    private var templateEngine:TemplateEngine? = null

    override fun start(startFuture: Future<Void>?) {

        this.server = vertx.createHttpServer()

        createShared()

        willLoadRouters()

        loadRouters()

        didLoadRouters()

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

        var errorTemplate = ErrorHandler.DEFAULT_ERROR_HANDLER_TEMPLATE

        if(config().getString("error.template") != null) {
            errorTemplate = config().getString("error.template")
        }

        router!!.route().failureHandler(ErrorHandler.create(errorTemplate))
    }

    private fun loadRouter(routers: JsonArray, idx:Int) {

        if(routers.size() == idx) return

        val routerName = routers.getString(idx)

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


        HttpContext.add(vertx, HttpContext.HttpServerShared(router!!,templateEngine!!))

        this.server!!.requestHandler {
            require ->
            router!!.accept(require)
        }
    }
}