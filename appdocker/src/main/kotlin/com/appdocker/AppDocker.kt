package com.appdocker

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import java.io.File


class AppDocker {
    private val logger = LoggerFactory.getLogger(AppDocker::class.java)

    private var vertx:Vertx? = null

    init {

        val options = VertxOptions().setClusterManager(HazelcastClusterManager())

        Vertx.clusteredVertx(options,{
            res ->

            if (res.succeeded()) {
                vertx = res.result()

                try {

                    val config = loadConfig()

                    val deploymentOptions = DeploymentOptions()
                            .setConfig(config?.getJsonObject("service-discovery"))

                    vertx?.deployVerticle(ServiceDiscoveryVerticle(),deploymentOptions,{
                        result ->

                        if (result.succeeded()) {
                            logger.info("load service discovery verticle success")
                        } else {
                            logger.error("load service discovery verticle -- failed", result.cause())

                            System.exit(1) // stop appdocker immediately
                        }

                    })

                } catch (e: Exception) {
                    logger.error("load service discovery verticle -- failed", e)

                    System.exit(1) // stop appdocker immediately
                }
            }
        })
    }

    private fun loadConfig() : JsonObject? {
        // check jvm property config
        val configPath = java.lang.System.getProperty("config")

        var config:JsonObject? = null

        if(configPath != null) {
            config = JsonObject(File(configPath).readText())
        } else {
            val classLoader = Thread.currentThread().contextClassLoader

            val url = classLoader.getResource("iris.json")

            if(url != null) {
                config = JsonObject(File(url.toURI()).readText())
            }
        }

        return config
    }
}