package com.appdocker

import io.vertx.core.AsyncResult

fun <T> AsyncResult<T>.causeWithStackTrace():String {
    val stream = StringBuilder()

    this.cause().stackTrace.forEach {
        stack ->

        stream.append(stack.toString())
        stream.append("\n\t")
    }

    return "${this.cause()}\n\t${stream.toString()}"
}