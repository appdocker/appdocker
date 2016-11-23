package com.appdocker

import io.vertx.core.AsyncResult

public fun <T> AsyncResult<T>.causeWithStackTrace():String {
    val stream = StringBuilder()

    this.cause().stackTrace.forEach {
        stack ->

        stream.append(stack.toString())
        stream.append("\n\t")
    }

    return "${this.cause()}\n\t${stream.toString()}"
}

val appdockerSharedData = "__appdocker_shared_data"

val appdockerServiceDiscovery = "__appdocker_service_discovery"