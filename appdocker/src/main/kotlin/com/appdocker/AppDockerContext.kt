package com.appdocker

import java.util.concurrent.ConcurrentHashMap


object AppDockerContext {

    val sharedMap = ConcurrentHashMap<String,Any>()
}