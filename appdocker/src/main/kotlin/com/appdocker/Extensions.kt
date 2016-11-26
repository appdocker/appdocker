package com.appdocker

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

fun <T> AsyncResult<T>.causeWithStackTrace():String {
    val stream = StringBuilder()

    this.cause().stackTrace.forEach {
        stack ->

        stream.append(stack.toString())
        stream.append("\n\t")
    }

    return "${this.cause()}\n\t${stream.toString()}"
}

fun <T> Handler(handler: (T) -> Unit): Handler<T> = Handler<T> { event -> handler(event) }

val appdocker = "__appdocker"

val appdockerConfig = "__appdocker_config"

val appdockerServiceDiscovery = "__appdocker_service_discovery"

val appdockerServiceTimestamp = "__appdocker_service_timestamp"

val appdockerServiceLease = "__appdocker_service_lease"