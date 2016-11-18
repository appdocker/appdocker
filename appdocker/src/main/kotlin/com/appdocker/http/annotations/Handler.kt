package com.appdocker.http.annotations


/**
 * annotation the handle function
 */

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Handler(val path:String = "")