package com.appdocker.http.auth


/**
 * throw this exception when loaded auth provider with same name twice or above
 */
class DuplicationAuthProviderNameException(message:String):Exception(message)