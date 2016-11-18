package com.appdocker.http.auth

import com.appdocker.http.auth.AuthHandlers
import io.vertx.core.AbstractVerticle
import io.vertx.ext.auth.jdbc.JDBCAuth
import io.vertx.ext.jdbc.JDBCClient


val authenticationQuery = JDBCAuth.DEFAULT_AUTHENTICATE_QUERY
val permissionsQuery = JDBCAuth.DEFAULT_PERMISSIONS_QUERY
val rolesQuery = JDBCAuth.DEFAULT_ROLES_QUERY
val rolePrefix = JDBCAuth.DEFAULT_ROLE_PREFIX

class JDBCAuth : AbstractVerticle() {

    // register jdbc auth provider
    override fun start() {

        val jdbcConfig = config().getJsonObject("jdbc") ?: throw IllegalArgumentException("'jdbc' configuration property can't be null")

        val jdbcClient = JDBCClient.createShared(vertx,jdbcConfig)

        val authProvider = JDBCAuth.create(jdbcClient)

        authProvider.setAuthenticationQuery(config().getString("authenticationQuery", authenticationQuery))

        authProvider.setPermissionsQuery(config().getString("permissionsQuery", permissionsQuery))

        authProvider.setRolePrefix(config().getString("rolePrefix", rolePrefix))

        authProvider.setRolesQuery(config().getString("rolesQuery", rolesQuery))

        val handlerConfig = config().getJsonObject("handler")?: throw IllegalArgumentException("'handler' configuration property can't be null")

        AuthHandlers.add(config().getString("tag","$this"),authProvider,handlerConfig,config().getJsonArray("path"))
    }
}