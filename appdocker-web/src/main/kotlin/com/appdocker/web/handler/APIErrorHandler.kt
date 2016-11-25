package com.appdocker.web.handler

import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext


class APIErrorHandler(val config:JsonObject?) : Handler<RoutingContext> {

    // TODO: implement load backend
    private val backend:APIErrorHandlerBackend? = null

    private val template:String

    init {

        val obj = config!!.getJsonObject("template")?:
                JsonObject().put("code","{errorCode}").put("message","{errorMessage}")

        template = obj.toString()
    }


    override fun handle(context: RoutingContext?) {

        val response = context!!.response()

        val errorMessage :String
        val errorCode:Int

        if (context.statusCode() != -1) {
            response.statusCode = context.statusCode()
            errorMessage = context.response().statusMessage
            errorCode = -1

        } else {
            // Internal error
            response.statusCode = 500

            val failure = context.failure() as? APIException

            if (failure == null) {
                errorMessage = "Internal Server Error"
                errorCode = -1
            } else {
                //TODO: load error message from backend
                errorMessage = "error_${failure.errorCode}\t${failure.message}"

                errorCode = failure.errorCode
            }
        }

        response.headers().set(HttpHeaders.CONTENT_TYPE,"application/json")

        sendError(context,errorCode,errorMessage)
    }

    private fun sendError(context: RoutingContext, errorCode: Int, errorMessage: String) {
        val response = context.response()

        response.end(
                template.replace("{errorCode}", Integer.toString(errorCode))
                        .replace("{errorMessage}", errorMessage)
        )
    }
}