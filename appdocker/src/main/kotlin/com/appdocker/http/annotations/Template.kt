package com.appdocker.http.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Template(val path:String,val contentType:String ="text/html")