package com.example

import com.example.data.User
import com.example.data.UserDataSource
import com.example.hashing.HashingService
import com.example.request.AuthRequest
import com.example.responses.AuthResponse
import com.example.security.TokenClaim
import com.example.security.TokenConfig
import com.example.security.TokenService
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.signUp(
//    hashingService: HashingService,
    userDataSource: UserDataSource,
) {
    post("signup") {
        val request = call.receiveNullable<AuthRequest>() ?: kotlin.run {
            call.respond(
                HttpStatusCode.BadRequest
            )
            return@post
        }
        val areFieldBlank = request.phoneNumber.isBlank() || request.name.isBlank()
        if (areFieldBlank) {
            return@post
        }
//        val saltedHash = hashingService.generateSaltedHash(request.password)
        val user = User(
            phoneNumber = request.phoneNumber,
            name = request.name
        )
        val wasAcknowledged = userDataSource.insertUser(user)
        if (!wasAcknowledged) {
            call.respond(Conflict)
            return@post
        }
        call.respond(OK)
    }
}
fun Route.signIn(
//    hashingService: HashingService,
    userDataSource: UserDataSource,
    tokenService: TokenService,
    tokenConfig: TokenConfig
){
    post("signin") {
        val request = call.receiveNullable<AuthRequest>() ?: kotlin.run {
            call.respond(
                HttpStatusCode.BadRequest
            )
            return@post
        }
        val user = userDataSource.getUserByPhoneNumber(phoneNumber = request.phoneNumber)
        if (user == null) {
            call.respond(
                Conflict
            )
        }
        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim(name = "userId", value = user?.id.toString())
        )
        call.respond(
            OK,
            AuthResponse(token)
        )
    }
}
fun Route.authenticate(){
    authenticate {
        get ("authenticate"){
            call.respond(OK)
        }
    }
}
fun Route.getSecretInfo(){
    authenticate {
        get("secret") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", String::class)
            call.respond(OK, "Your userId is $userId")
        }
    }
}