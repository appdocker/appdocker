package com.appdocker.web.controller

import com.appdocker.AppDockerContext
import com.appdocker.appdockerServiceDiscovery
import com.appdocker.web.annotations.Handler
import com.appdocker.web.annotations.Template
import com.appdocker.web.appdockerHttpServerRouter
import com.appdocker.web.appdockerHttpServerTemplate
import com.esotericsoftware.reflectasm.MethodAccess
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.TemplateEngine
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.Status
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.*


open class ControllerVerticle : AbstractVerticle(){

    protected var logger: Logger

    protected var router: Router? = null

    protected var serviceDiscovery:ServiceDiscovery? = null

    protected var templateEngine: TemplateEngine? = null

    protected var routeRootPath:String? = null

    init {
        logger = LoggerFactory.getLogger(javaClass)
    }


    override fun start(startFuture: Future<Void>?) {
        router = AppDockerContext.sharedMap[appdockerHttpServerRouter] as Router

        templateEngine = AppDockerContext.sharedMap[appdockerHttpServerTemplate] as TemplateEngine

        serviceDiscovery = AppDockerContext.sharedMap[appdockerServiceDiscovery] as ServiceDiscovery

        if (javaClass != StaticResourceController::class.java) {
            // process controller's relative path
            routeRootPath = config().getString("path")?:throw IllegalArgumentException("web controller must config with path arg")
        }

        // process annotations

        processAnnotations()

        super.start(startFuture)
    }


    private fun processAnnotations() {
        javaClass.declaredMethods
                .filter { it.getAnnotation(Handler::class.java) != null }
                .forEach  {
                    method ->

                    val httpMethodAnnotation = method.getAnnotation(com.appdocker.web.annotations.Method::class.java)

                    createRoutPath(method).forEach loop@ {
                        routePath ->

                        logger.info("register method ${this.javaClass.name}#${method.name} as http route($routePath) handler")

                        createRouteHandlers(routePath, httpMethodAnnotation, method)

                        logger.info("register method ${this.javaClass.name}#${method.name} as http route($routePath) handler -- success")

                        val templateAnnotation = method.getAnnotation(Template::class.java)?:return@loop

                        logger.info("register method ${this.javaClass.name}#${method.name}'s template handler for http route($routePath)")
                        templateHandler(httpMethodAnnotation,templateAnnotation,routePath)
                        logger.info("register method ${this.javaClass.name}#${method.name}'s template handler for http route($routePath) -- success")
                    }
                }
    }

    private fun createRoutPath(method: Method) :List<String> {

        val routes = arrayListOf<String>()

        val regex = Regex(pattern = "(.*)(V[0-9]+)$")

        val matchResult = regex.matchEntire(method.name)

        if (matchResult != null) {

            val name = matchResult.groupValues[1]
            var version = matchResult.groupValues[2]

            routes.add(pathCombine(pathCombine(routeRootPath!!, version), name))

            version = "v" + version.substring(1)

            routes.add(pathCombine(pathCombine(routeRootPath!!, version), name))

        } else {

            routes.add(pathCombine(routeRootPath!!,method.name))
        }

        return routes
    }

    private fun createRouteHandlers(fullPath: String, httpMethodAnnotation: com.appdocker.web.annotations.Method?, method: java.lang.reflect.Method) {
        val routes = createRoutes(fullPath, httpMethodAnnotation)

        val access = MethodAccess.get(javaClass)

        routes.forEach {
            route ->

            if (method.parameters.isEmpty() || method.parameters[0].type != RoutingContext::class.java) {
                logger.error("http handler method first parameter must be declare as io.vertx.ext.web.RoutingContext type\n\t$method")
                System.exit(1)
            }

            val discovery = if (method.parameters.size > 1)
                createServiceDiscovery(method, method.parameters.slice(1..method.parameters.size - 1))
            else
                null

            route.handler {
                context ->

                if (discovery == null) {
                    access.invoke(this, method.name, context)
                } else {

                    logger.debug("prepare invoke $method ...")

                    discovery {

                        ar ->

                        try {

                            if (ar.succeeded()) {
                                logger.debug("prepare invoke $method -- success")

                                access.invoke(this, method.name, context, *ar.result())

                            } else {
                                logger.error("prepare invoke $method -- failed", ar.cause())

                                // TODO: add formula error log
                                throw ar.cause()
                            }
                        } catch (e:Throwable) {
                            if (!ar.succeeded()) {
                                context.fail(e)
                                return@discovery
                            }

                            ar.result().forEach loop@ {
                                service ->

                                service?:return@loop

                                if (service.javaClass.isArray) {

                                    val size = java.lang.reflect.Array.getLength(service) - 1

                                    for(i in 1..size) {
                                        ServiceDiscovery.releaseServiceObject(serviceDiscovery!!,java.lang.reflect.Array.get(service, i))
                                    }
                                } else {
                                    ServiceDiscovery.releaseServiceObject(serviceDiscovery!!,service)
                                }
                            }

                            context.fail(e)
                        }
                    }
                }

            }
        }
    }

    private fun createServiceDiscovery(method: java.lang.reflect.Method,parameters : List<Parameter>) : ( (AsyncResult<Array<Any?>>) -> Unit ) -> Unit {

        val context = arrayListOf<(( (AsyncResult<Any?>) -> Unit ) -> Unit)>()

        parameters.mapIndexed {

            index, parameter ->

            if (!parameter.type.isInterface) {
                if (!parameter.type.isArray || !parameter.type.componentType.isInterface) {
                    logger.error("http method parameter(${index+1}) must be interface or array of interface\n\tmethod:$method")
                }
            }

            return@mapIndexed parameter.type

        }.forEach {

            parameter ->


            if (parameter.isArray) {

                context.add {
                    handler ->

                    serviceDiscovery!!.getRecords( JsonObject().put("service.interface", parameter.componentType.name) ) {
                        ar ->

                        if(ar.failed()) {
                            logger.error("invoke discovery service error",ar.cause())
                            handler(Future.failedFuture(ar.cause()))
                        } else {

                            val result = ar.result().filter {

                                r ->

                                r.status == Status.UP
                            }

                            val services = java.lang.reflect.Array.newInstance(parameter.componentType,result.size)

                            result.forEachIndexed { i, record ->

                                java.lang.reflect.Array.set(services, i, serviceDiscovery!!.getReference(record).get())
                            }

                            handler(Future.succeededFuture(services))
                        }
                    }
                }
            } else {


                context.add {
                    handler ->

                    serviceDiscovery!!.getRecord( JsonObject().put("service.interface", parameter.name) ) {
                        ar ->

                        if(ar.failed()) {
                            logger.error("invoke discovery service error",ar.failed())
                            handler(Future.failedFuture(ar.cause()))
                        } else {

                            if (ar.result() != null) {
                                handler(Future.succeededFuture(serviceDiscovery!!.getReference(ar.result()).get()))
                            } else {
                                handler(Future.succeededFuture(null))
                            }


                        }
                    }
                }

            }
        }

        return {
            handler ->
            invokeServiceDiscovery(context, 0, arrayListOf<Any?>(), handler)
        }
    }

    private fun invokeServiceDiscovery(
            context:ArrayList<(( (AsyncResult<Any?>) -> Unit ) -> Unit)>,
            index : Int,
            result : ArrayList<Any?>,
            handler : (AsyncResult<Array<Any?>>) -> Unit ) {

        if (index == context.size) {
            handler(Future.succeededFuture(result.toArray()))
            return
        }

        context[index] {
            ar ->

            if(ar.failed()) {
                handler(Future.failedFuture(ar.cause()))
            } else {

                result.add(ar.result())

                invokeServiceDiscovery(context,index + 1, result, handler)
            }
        }
    }

    private fun createRoutes(fullPath: String, httpMethodAnnotation: com.appdocker.web.annotations.Method?): ArrayList<Route> {

        val routes = arrayListOf<Route>()

        if ( httpMethodAnnotation != null ) {
            httpMethodAnnotation.allowedMethods.forEach { httpMethod ->
                logger.info("register path method($httpMethod) :$fullPath")
                routes.add(router!!.route(httpMethod, fullPath))
            }
        } else {
            logger.info("register path generic method :$fullPath")
            routes.add(router!!.route(fullPath))
        }

        return routes
    }

    private fun templateHandler(httpMethodAnnotation: com.appdocker.web.annotations.Method?, templateAnnotation: Template, fullPath: String) {
        if(templateEngine == null) {
            throw IllegalArgumentException("require template engine for router :$javaClass")
        }

        val routes = createRoutes(fullPath, httpMethodAnnotation)

        val templateFilePath = "${config().getString("template.root","template")}/${templateAnnotation.value}"

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
}