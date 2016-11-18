package com.appdocker.http.controller

import com.appdocker.http.HttpContext
import com.appdocker.http.annotations.Auth
import com.appdocker.http.annotations.Handler
import com.appdocker.http.annotations.Method
import com.appdocker.http.annotations.Template
import com.appdocker.http.auth.AuthHandlers
import com.esotericsoftware.reflectasm.MethodAccess
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.TemplateEngine
import java.util.*


open class AbstractController : AbstractVerticle(){

    protected var logger: Logger

    protected var router: Router? = null

    protected var rootPath:String? = null

    protected var templateEngine: TemplateEngine? = null


    init {
        logger = LoggerFactory.getLogger(javaClass)
    }


    override fun start(startFuture: Future<Void>) {

        val shared = HttpContext.getShared(vertx)

        router = shared!!.router

        templateEngine = shared.templateEngine

        rootPath = config().getString("path")

        start()

        setupRouter()

        startFuture.complete()
    }


    override fun stop(stopFuture: Future<Void>?) {

        shutdownRouter()

        super.stop(stopFuture)
    }

    /**
     * setup router handlers using reflect tech
     */
    private fun setupRouter() {

        for(method in javaClass.declaredMethods) {
            val handlerAnnotation = method.getAnnotation(Handler::class.java)?:continue

            val fullPath = if(handlerAnnotation.path == "")  pathCombine(rootPath!!,method.name) else  handlerAnnotation.path

            // process auth annotation
            val authAnnotation = method.getAnnotation(Auth::class.java)

            if(authAnnotation != null) {
                val handler = AuthHandlers.get(fullPath)?:throw IllegalArgumentException("can't find valid AuthHandler for path :$fullPath")

                router!!.route(fullPath).handler(handler)

                authAnnotation.roles.forEach {
                    role ->

                    handler.addAuthority(role)
                }

                logger.info("register AuthHandler($handler) roles(${authAnnotation.roles.reduce { source, s -> source + s + ","  }}) for path:$fullPath")
            }


            // get router method
            val httpMethodAnnotation = method.getAnnotation(Method::class.java)

            logger.info("register method ${this.javaClass.name}#${method.name} as http route($fullPath) handler")

            createRouteHandlers(fullPath, httpMethodAnnotation, method)

            // invoke template engine

            val templateAnnotation = method.getAnnotation(Template::class.java)?:continue

            templateHandler(httpMethodAnnotation,templateAnnotation,fullPath)

        }

    }

    private fun createRouteHandlers(fullPath: String, httpMethodAnnotation: Method?, method: java.lang.reflect.Method) {
        val routes = createRoutes(fullPath, httpMethodAnnotation)

        val access = MethodAccess.get(javaClass)

        routes.forEach {
            route ->

            route.handler {
                context ->

                access.invoke(this, method.name, context)
            }
        }
    }

    private fun createRoutes(fullPath: String, httpMethodAnnotation: Method?): ArrayList<Route> {

        val routes = arrayListOf<Route>()

        if ( httpMethodAnnotation != null ) {
            httpMethodAnnotation.allowedMethods.forEach { httpMethod ->
                logger.info("register path method($httpMethod) :$fullPath")
                routes.add(router!!.route(httpMethod, fullPath))
            }
        } else {
            logger.info("register path method :$fullPath")
            routes.add(router!!.route(fullPath))
        }

        return routes
    }

    private fun templateHandler(httpMethodAnnotation: Method?, templateAnnotation: Template, fullPath: String) {
        if(templateEngine == null) {
            throw IllegalArgumentException("require template engine for router :$javaClass")
        }

        val routes = createRoutes(fullPath, httpMethodAnnotation)

        val templateFilePath = "${config().getString("template.root","template")}/${templateAnnotation.path}"

        routes.forEach {
            route ->

            route.handler {
                context ->

                templateEngine!!.render(context, templateFilePath) { res ->
                    if (res.succeeded()) {
                        context.response().putHeader(HttpHeaders.CONTENT_TYPE, templateAnnotation.contentType).end(res.result())
                    } else {
                        context.fail(res.cause())
                    }
                }
            }
        }
    }

    private fun pathCombine(root:String,relative:String):String {
        var combine = "$root/$relative"

        if(!combine.startsWith('/')) {
            combine = "/$combine"
        }

        combine = combine.replace("//","/")

        return combine
    }

    private fun shutdownRouter() {

    }
}