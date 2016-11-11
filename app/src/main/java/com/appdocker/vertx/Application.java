package com.appdocker.vertx;


import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);

    private Vertx vertx = null;

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, "UTF-8");
    }

    private static JsonObject loadConfig() throws IOException, NotFoundException {
        logger.debug("try load appdocker.json ...");

        String configPath = java.lang.System.getProperty("config");


        if (configPath == null) {
            URL url = Thread.currentThread().getContextClassLoader().getResource("appdocker.json");

            if (url == null) {
                throw new NotFoundException("not found valid appdocker.json file in classpath");
            }

            configPath = new File(url.getFile()).getCanonicalPath();
        }

        return new JsonObject(readFile(configPath));
    }

    private Application() {
        ClusterManager mgr = new HazelcastClusterManager();

        VertxOptions options = new VertxOptions().setClusterManager(mgr);

        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                vertx = res.result();

                try {

                    JsonObject config = loadConfig();

                    DeploymentOptions deploymentOptions = new DeploymentOptions();

                    deploymentOptions.setConfig(config.getJsonObject("service-discovery"));

                    ServiceDiscoveryVerticle verticle = new ServiceDiscoveryVerticle();

                    vertx.deployVerticle(verticle,deploymentOptions,result -> {
                        if(result.succeeded()) {
                            logger.info("load service:{0} -- succeeded","ServiceDiscoveryVerticle");
                        } else {
                            logger.error("load service:{0} -- failed","ServiceDiscoveryVerticle",result.cause());

                            System.exit(1);
                        }
                    });

                } catch (Exception e) {
                    logger.error("load appdocker services error",e);

                    System.exit(2);
                }

            } else {
                logger.error("create clustered vertx error",res.cause());

                System.exit(2);
            }
        });
    }

    private void loadService(JsonArray services, int index) {

        if(services == null) return;

        if(services.size() == index) return;

        if (services.getString(index) == null) return ;

        logger.info("loading service:{0}",services.getString(index));


        vertx.deployVerticle("service:" + services.getString(index), result -> {
            if(result.succeeded()) {
                logger.info("load service:{0} -- succeeded", services.getString(index));
            } else {
                logger.error("load service:{0} -- failed", services.getString(index),result.cause());

                System.exit(1);
            }

            loadService(services,index + 1);
        });
    }

    public static void main(String[] args) throws IOException, NotFoundException {

        System.setProperty("vertx.logger-delegate-factory-class-name","io.vertx.core.logging.SLF4JLogDelegateFactory");

        new Application();
    }
}
