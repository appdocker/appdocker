package com.appdocker.servicediscovery

import com.appdocker.AppDockerContext
import com.appdocker.appdockerServiceDiscovery
import com.appdocker.appdockerServiceLease
import com.appdocker.appdockerServiceTimestamp
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.types.EventBusService
import io.vertx.serviceproxy.ProxyHelper
import java.util.concurrent.ConcurrentHashMap


class ServiceDiscoveryVerticle: AbstractVerticle() {

    var discovery: ServiceDiscovery? = null

    val logger = LoggerFactory.getLogger(ServiceDiscoveryVerticle::class.java)!!

    override fun stop() {
        super.stop()
    }

    override fun start() {

        discovery = ServiceDiscovery.create(vertx)

        // start service discovery
        exportService()

        AppDockerContext.sharedMap.put(appdockerServiceDiscovery, discovery!!)
    }

    private fun exportService() {

        val exports = config().getJsonArray("export") ?: return

        exports
                .filterIsInstance<JsonObject>()
                .forEach { export ->


                    registerService(export)

                    publishRecordWithHeartbeat(export)

                }
    }

    private fun publishRecordWithHeartbeat(export: JsonObject) {

        val timeout = config().getLong("timeout",5000)

        val record = createRecord(export, timeout)

        discovery!!.publish(record) {
            ar ->

            if (ar.succeeded()) {

                logger.debug("update service record -- success :\n\t{}",export)
            } else {
                logger.error("update service record -- failed :\n\t{}",export,ar.cause())
            }
        }

        vertx.setPeriodic(timeout) {

            record.metadata = record.metadata.put(appdockerServiceTimestamp, System.currentTimeMillis()).put(appdockerServiceLease, timeout * 2)

            discovery!!.update(record) {
                ar ->

                if (ar.succeeded()) {

                    logger.debug("update service record -- success :\n\t{}",record.toJson())
                } else {
                    logger.error("update service record -- failed :\n\t{}",record.toJson(),ar.cause())
                }
            }
        }


    }

    private fun createRecord(export: JsonObject, timeout: Long): Record {
        val metadata = export.getJsonObject("metadata")

        metadata.put(appdockerServiceTimestamp, System.currentTimeMillis())

        metadata.put(appdockerServiceLease, timeout + 5000)

        val record = EventBusService.createRecord(
                export.getString("name"),
                export.getString("address"),
                export.getString("service"),
                metadata)
        return record
    }

    private fun registerService(export: JsonObject) {
        val implClassName = export.getString("impl")

        if (implClassName == null) {
            logger.error("export service must indicate impl class\n\t", export)
            System.exit(1)
        }

        val implClass = Class.forName(implClassName)

        val obj = implClass.newInstance()

        @Suppress("UNCHECKED_CAST")
        ProxyHelper.registerService(
                Class.forName(export.getString("service")) as Class<Any>,
                vertx,
                obj,
                export.getString("address"))
    }
}