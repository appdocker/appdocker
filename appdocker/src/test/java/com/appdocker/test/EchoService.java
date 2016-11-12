package com.appdocker.test;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@VertxGen
@ProxyGen
interface EchoService {


    void hello(final String message, final Handler<AsyncResult<String>> resultHandler);


    @ProxyClose
    void close();
}
