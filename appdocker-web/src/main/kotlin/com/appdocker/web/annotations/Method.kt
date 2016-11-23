package com.appdocker.web.annotations

import io.vertx.core.http.HttpMethod

/**
 * indicate function's bind http methods
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Method(vararg val allowedMethods: HttpMethod)