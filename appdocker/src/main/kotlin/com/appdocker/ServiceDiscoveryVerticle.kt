package com.appdocker

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.types.EventBusService
import io.vertx.serviceproxy.ProxyHelper
import java.util.concurrent.ConcurrentHashMap


class ServiceDiscoveryVerticle:AbstractVerticle() {

    var discovery:ServiceDiscovery? = null

    val published = ConcurrentHashMap<JsonObject,Record>()

    val logger = LoggerFactory.getLogger(ServiceDiscoveryVerticle::class.java)!!

    override fun stop() {
        super.stop()
    }

    override fun start() {

        discovery = ServiceDiscovery.create(vertx)

        // start service discovery
        exportService()
    }

    private fun exportService() {

        val exports = config().getJsonArray("export") ?: return

        exports
                .filterIsInstance<JsonObject>()
                .forEach { export ->

                    val record = EventBusService.createRecord(
                            export.getString("name"),
                            export.getString("address"),
                            export.getString("service"),
                            export.getJsonObject("metadata"))

                    discovery!!.publish(record,{
                        ar ->

                        if (ar.succeeded()) {

                            published[export] = ar.result()

                            //                    // reflect create service implement
                            val implClassName = export.getString("impl")

                            if (implClassName == null) {
                                logger.error("export service must indicate impl class\n\t",export)
                                System.exit(1)
                            }

                            val implClass = Class.forName(implClassName)

                            val obj = implClass.newInstance()

                            @Suppress("UNCHECKED_CAST")
                            ProxyHelper.registerService(Class.forName(export.getString("service")) as Class<Any>,vertx,obj,export.getString("address"))

                            logger.info("create event bus service -- success :\n\t{}",export)
                        } else {
                            logger.error("create event bus service -- failed :\n\t{}",export,ar.cause())
                        }
                    })
                }
    }
}