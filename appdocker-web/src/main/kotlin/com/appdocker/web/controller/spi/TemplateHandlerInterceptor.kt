package com.appdocker.web.controller.spi

import com.appdocker.AppDockerContext
import com.appdocker.web.annotations.Template
import com.appdocker.web.appdockerHttpServerTemplate
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.templ.TemplateEngine


class TemplateHandlerInterceptor : AbstractHandlerInterceptor() {

    override fun after(controller: JsonObject, router: Router, routePath: String, handler: java.lang.reflect.Method) {

        val templateAnnotation = handler.getAnnotation(Template::class.java)?:return

        val templateEngine = AppDockerContext.sharedMap[appdockerHttpServerTemplate] as TemplateEngine

        val httpMethodAnnotation = handler.getAnnotation(com.appdocker.web.annotations.Method::class.java)

        val routes = createRoutes(router, routePath, httpMethodAnnotation)

        val templateFilePath = "${controller.getString("template.root","template")}/${templateAnnotation.value}"

        routes.forEach {
            route ->

            route.handler {
                context ->

                templateEngine.render(context, templateFilePath) { res ->
                    if (res.succeeded()) {
                        context.response().putHeader(HttpHeaders.CONTENT_TYPE, templateAnnotation.contentType).end(res.result())
                    } else {
                        context.fail(res.cause())
                    }
                }
            }
        }
    }
}