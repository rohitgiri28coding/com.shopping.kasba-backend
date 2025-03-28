package com.example.plugins

import com.example.authenticate
import com.example.data.UserDataSource
import com.example.getSecretInfo
import com.example.hashing.HashingService
import com.example.security.TokenConfig
import com.example.security.TokenService
import com.example.signIn
import com.example.signUp
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
//    hashingService: HashingService,
    userDataSource: UserDataSource,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    routing {
        signUp(userDataSource, tokenService, tokenConfig)
        signIn(userDataSource, tokenService, tokenConfig)
        authenticate()
        getSecretInfo()
    }
}
