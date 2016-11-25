package com.appdocker.servicediscovery

import com.appdocker.appdockerServiceLease
import com.appdocker.appdockerServiceTimestamp
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.impl.AsyncMap
import io.vertx.servicediscovery.spi.ServiceDiscoveryBackend
import java.util.*


class DeadCheckServiceDiscoveryBackend : ServiceDiscoveryBackend {

    private val logger = LoggerFactory.getLogger(DeadCheckServiceDiscoveryBackend::class.java)

    private var registry: AsyncMap<String, String>? = null

    override fun getRecords(resultHandler: Handler<AsyncResult<MutableList<Record>>>?) {
        registry!!.getAll { ar ->
            if (ar.succeeded()) {
                resultHandler?.handle(Future.succeededFuture(ar.result().values
                        .map {
                            s -> Record(JsonObject(s))
                        }
                        .filter {

                            record ->

                            val timestamp = record.metadata.getLong(appdockerServiceTimestamp)

                            val lease = record.metadata.getLong(appdockerServiceLease)

                            if (timestamp == null || lease == null || ((System.currentTimeMillis() - timestamp) > lease)) {
                                remove(record) {
                                    ar ->

                                    if (ar.succeeded()) {
                                        logger.debug("remove record -- success\n\t$record")
                                    } else {
                                        logger.error("remove record -- failed\n\t$record",ar.cause())
                                    }
                                }

                                return@filter false
                            }

                            return@filter true
                        }
                        .toMutableList()
                ))
            } else {
                resultHandler?.handle(Future.failedFuture(ar.cause()))
            }
        }
    }

    override fun remove(record: Record?, resultHandler: Handler<AsyncResult<Record>>?) {
        remove(record!!.registration, resultHandler)
    }

    override fun remove(uuid: String?, resultHandler: Handler<AsyncResult<Record>>?) {
        registry!!.remove(uuid, Handler { ar ->
            if (ar.succeeded()) {
                if (ar.result() == null) {
                    // Not found
                    resultHandler?.handle(Future.failedFuture("Record '$uuid' not found"))
                } else {
                    resultHandler?.handle(Future.succeededFuture<Record>(
                            Record(JsonObject(ar.result()))))
                }
            } else {
                resultHandler?.handle(Future.failedFuture(ar.cause()))
            }
        })
    }

    override fun init(vertx: Vertx?, config: JsonObject?) {
        this.registry = AsyncMap<String, String>(vertx, "service.registry")
    }

    override fun store(record: Record?, resultHandler: Handler<AsyncResult<Record>>?) {

        if (record!!.registration != null) {
            throw IllegalArgumentException("The record has already been registered")
        }

        record.registration = UUID.randomUUID().toString()

        registry!!.put(record.registration, record.toJson().encode()) { ar ->
            if (ar.succeeded()) {
                resultHandler?.handle(Future.succeededFuture<Record>(record))
            } else {
                resultHandler?.handle(Future.failedFuture(ar.cause()))
            }
        }
    }

    override fun getRecord(uuid: String?, resultHandler: Handler<AsyncResult<Record>>?) {
        registry!!.get(uuid, Handler{ ar ->
            if (ar.succeeded()) {
                if (ar.result() != null) {
                    resultHandler?.handle(Future.succeededFuture<Record>(Record(JsonObject(ar.result()))))
                } else {
                    resultHandler?.handle(Future.succeededFuture(null))
                }
            } else {
                resultHandler?.handle(Future.failedFuture(ar.cause()))
            }
        })
    }

    override fun update(record: Record?, resultHandler: Handler<AsyncResult<Void>>?) {
        registry!!.put(record!!.registration, record.toJson().encode()) { ar ->
            if (ar.succeeded()) {
                resultHandler?.handle(Future.succeededFuture())
            } else {
                resultHandler?.handle(Future.failedFuture(ar.cause()))
            }
        }
    }

}