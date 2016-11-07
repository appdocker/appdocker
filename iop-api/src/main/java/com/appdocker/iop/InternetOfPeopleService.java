package com.appdocker.iop;

import io.vertx.codegen.annotations.ProxyClose;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
interface InternetOfPeopleService {

    /**
     * Method to create the proxy
     * @param vertx vertx instance
     * @param address address
     * @return  the service proxy
     */
    static InternetOfPeopleService createProxy(Vertx vertx, String address) {
        return new InternetOfPeopleServiceVertxEBProxy(vertx, address);
    }

    // Actual service operations here...
    void hello(final String message,
               final Handler<AsyncResult<String>> resultHandler);


    @ProxyClose
    void close();
}
