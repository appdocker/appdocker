package com.appdocker.samples;


import com.appdocker.web.annotations.Handler;
import com.appdocker.web.annotations.Template;
import com.appdocker.web.controller.ControllerVerticle;
import com.appdocker.web.handler.APIException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Objects;

public class SampleAPI2 extends ControllerVerticle {
    private static final Logger logger = LoggerFactory.getLogger(SampleAPI2.class);
    @Handler
    @Template("/login")
    public void login(RoutingContext context, EchoService[] services) {

        for (EchoService service : services) {
            service.hello("hello" , ar -> {

                Objects.requireNonNull(getServiceDiscovery());

                ServiceDiscovery.releaseServiceObject(getServiceDiscovery(),service);

                if(ar.failed()) {
                    logger.error("call service hello error",ar.cause());

                    return;
                }

                logger.debug(ar.result());
            });
        }

        context.put("hello",services.length);

        context.next();
    }

    @Handler
    public void error(RoutingContext context) throws APIException {
        throw new APIException(1);
    }
}
