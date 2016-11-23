package com.appdocker.samples

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler


class EchoServiceImpl : EchoService {

    override fun hello(message: String?, resultHandler: Handler<AsyncResult<String>>?) {
        resultHandler!!.handle(Future.succeededFuture("hello" + this))
    }
}