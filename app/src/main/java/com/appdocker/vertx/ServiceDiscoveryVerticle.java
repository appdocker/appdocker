package com.appdocker.vertx;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;

final class ServiceDiscoveryVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryVerticle.class);

    private ServiceDiscovery discovery = null;


    @Override
    public void start(Future<Void> startFuture) throws Exception {



        discovery = ServiceDiscovery.create(vertx);

        createExport(config().getJsonArray("export"));

        createImport(config().getJsonArray("import"));

        super.start(startFuture);
    }

    private void createExport(JsonArray services) {

        if (services == null) return;

        for(Object object : services.getList()) {

            JsonObject jsonObject = (JsonObject)object;

            Record record = EventBusService.createRecord(
                    jsonObject.getString("name"),
                    jsonObject.getString("address"),
                    jsonObject.getString("service"),
                    jsonObject.getJsonObject("metadata"));

            discovery.publish(record,ar -> {
                if (!ar.succeeded()) {
                    logger.error("publish service error .",ar.cause());
                }
            });
        }
    }

    private void createImport(JsonArray services) {

        if (services == null) return;

        for(Object object : services.getList()) {

            discovery.getRecord((JsonObject) object, ar -> {
                if (ar.succeeded()) {
                    if (ar.result() != null) {
                        logger.info("service({0}) status {1} type {2}",ar.result().getName(),ar.result().getStatus(),ar.result().getType());
                    }
                } else {
                    logger.info("getRecord error .",ar.cause());
                }
            });
        }
    }
}
