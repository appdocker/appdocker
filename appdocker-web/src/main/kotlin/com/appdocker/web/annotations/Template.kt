package com.appdocker.web.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Template(val value:String,val contentType:String ="text/html")