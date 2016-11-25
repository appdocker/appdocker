package com.appdocker.samples.test;


import com.appdocker.samples.EchoService;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Repeat;
import io.vertx.ext.unit.junit.RepeatRule;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;

import io.vertx.spi.cluster.jgroups.JGroupsClusterManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EchoServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(EchoServiceTest.class);

    private ServiceDiscovery discovery = null;

    private Vertx vertx = null;

    private EchoService service = null;

    public EchoServiceTest() {
        System.setProperty("java.net.preferIPv4Stack","true");
    }

    @Rule
    public RepeatRule rule = new RepeatRule();

    @Before
    public void setUp(TestContext context) throws Exception {

        Async async = context.async();

        Vertx.clusteredVertx(new VertxOptions().setClusterManager(new JGroupsClusterManager()), ar -> {
            context.assertTrue(ar.succeeded());

            vertx = ar.result();

            discovery = ServiceDiscovery.create(ar.result());

            EventBusService.getProxy(discovery,EchoService.class, ar2 -> {
                context.assertTrue(ar2.succeeded());

                service = ar2.result();

                async.complete();
            });
        });
    }

    @After
    public void tearDown(TestContext context) throws Exception {



        ServiceDiscovery.releaseServiceObject(discovery,service);

        Async async = context.async();

        discovery.close();

        vertx.close(v -> {
            async.complete();
        });

    }

    @Repeat(1000)
    @Test
    public void sayHello(TestContext context) {

        Async async = context.async();

        callHello(context,async);
    }

    @Test
    public void sayHelloV(TestContext context) {

        Async async = context.async();

        for(int i =0; i < 1000; i ++) callHello(context,null);


        callHello(context,async);
    }

    private void callHello(TestContext context, Async async) {

        service.hello("hello vertx remote service", ar2 -> {

            if(ar2.failed()) logger.error("call hello exception :",ar2.cause());

            context.assertTrue(ar2.succeeded());

            logger.info("received echo :{0}",ar2.result());

            if(async != null) async.complete();
        });
    }
}
