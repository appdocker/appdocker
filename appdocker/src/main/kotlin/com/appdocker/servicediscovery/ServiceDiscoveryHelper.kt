@file:JvmName("ServiceDiscoveryHelper")

package com.appdocker.servicediscovery

import io.vertx.core.eventbus.ReplyException
import io.vertx.servicediscovery.ServiceDiscovery


fun checkAndReleaseServiceObject(discovery: ServiceDiscovery, obj :Any, cause : Throwable?) {

    var remove = false

    val replyException = cause as? ReplyException

    if (replyException != null) {
        remove = true
    }

    discovery.bindings().filter { obj.equals(it.cached()) }.forEach {
        r ->

        discovery.release(r)

        if(remove) {
            discovery.unpublish(r.record().registration) {
                ar ->
            }
        }
    }


}