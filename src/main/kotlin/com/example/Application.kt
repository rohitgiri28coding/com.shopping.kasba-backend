package com.example

import com.example.data.MongoUserDataSource
import com.example.plugins.*
import com.example.security.JwtTokenService
import com.example.security.TokenConfig
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val password = System.getenv("MONGO_PW")
    val uri = "mongodb+srv://sandipkumar9334:$password@cluster0.b6kcl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"
    val mongoClient = MongoClient.create(uri)
    val database = mongoClient.getDatabase("kasba")
    val userDataSource = MongoUserDataSource(database)
    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secretKey = System.getenv("JWT_SECRET")
    )
    configureSerialization()
    configureMonitoring()
    configureSecurity(tokenConfig)
    configureRouting(
        userDataSource = userDataSource,
        tokenConfig = tokenConfig,
        tokenService = tokenService
    )
}
