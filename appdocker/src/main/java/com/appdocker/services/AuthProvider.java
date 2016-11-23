package com.appdocker.services;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;


@ProxyGen
@VertxGen
public interface AuthProvider {
    void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler);
}
