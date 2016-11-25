package com.appdocker

import com.appdocker.servicediscovery.ServiceDiscoveryVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.spi.cluster.jgroups.JGroupsClusterManager

import java.io.File


class AppDocker {
    private val logger = LoggerFactory.getLogger(AppDocker::class.java)

    private var vertx:Vertx? = null

    companion object {


        @JvmStatic  fun main(args: Array<String>) {

            if (System.getProperty("vertx.logger-delegate-factory-class-name") == null) {
                System.setProperty("vertx.logger-delegate-factory-class-name","io.vertx.core.logging.SLF4JLogDelegateFactory")
                System.setProperty("hazelcast.logging.type","slf4j")

                System.setProperty("java.net.preferIPv4Stack","true")
            }

            AppDocker()
        }
    }

    init {

        val options = VertxOptions().setClusterManager(JGroupsClusterManager())

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
                            loadService(config?.getJsonArray("services"),0)
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

    private fun loadService(services: JsonArray?, index:Int) {

        if (services == null) return

        if(services.size() == index) return

        val name = services.getString(index)?:return

        logger.info("loading service:$name")

        vertx?.deployVerticle("service:$name") {
            result ->

            if(result.succeeded()) {
                logger.info("load service:$name -- succeeded")
            } else {
                logger.info("load service:$name -- failed\n${result.causeWithStackTrace()}")
                System.exit(1)
            }

            loadService(services,index + 1)
        }
    }

    private fun loadConfig() : JsonObject? {
        // check jvm property config
        val configPath = java.lang.System.getProperty("config")

        var config:JsonObject? = null

        if(configPath != null) {
            config = JsonObject(File(configPath).readText())
        } else {
            val classLoader = Thread.currentThread().contextClassLoader

            val url = classLoader.getResource("appdocker.json")

            if(url != null) {
                config = JsonObject(File(url.toURI()).readText())
            }
        }

        return config
    }
}