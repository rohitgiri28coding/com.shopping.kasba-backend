package com.example

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.MongoUserDataSource
import com.example.data.User
import com.example.request.AuthRequest
import com.example.request.AuthRequest1
import com.example.responses.AuthResponse
import com.example.security.JwtTokenService
import com.example.security.TokenClaim
import com.example.security.TokenConfig
import com.google.gson.Gson
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.runBlocking

class LambdaHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    companion object {
        private val password = System.getenv("MONGO_PW") ?: "defaultPassword"
        private val uri =
            "mongodb+srv://sandipkumar9334:$password@cluster0.b6kcl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"

        private val mongoClient = MongoClient.create(uri)
        private val database = mongoClient.getDatabase("kasba")
        val userDataSource = MongoUserDataSource(database)
        val tokenService = JwtTokenService()
        val tokenConfig = TokenConfig(
            issuer = System.getenv("JWT_ISSUER") ?: "defaultIssuer",
            audience = System.getenv("JWT_AUDIENCE") ?: "defaultAudience",
            expiresIn = 365L * 1000L * 60L * 60L * 24L,
            secretKey = System.getenv("JWT_SECRET") ?: "defaultSecretKey"
        )
    }

    override fun handleRequest(
        input: APIGatewayProxyRequestEvent?,
        context: Context?
    ): APIGatewayProxyResponseEvent {

        val path = input?.path ?: "/"
        var body = input?.body ?: ""
        val headers = input?.headers ?: emptyMap()


        return when (path) {
            "/signup" -> signUp(body)
            "/signin" -> signIn(body)
            "/protected" -> authenticate(headers["Authorization"])
            "/secret" -> getSecretInfo(headers["Authorization"])
            else -> APIGatewayProxyResponseEvent().apply {
                statusCode = 404
                body = "Path not found"
            }
        }
    }

    private fun signUp(requestBody: String): APIGatewayProxyResponseEvent {
        val gson = Gson()
        val request = try {
            gson.fromJson(requestBody, AuthRequest::class.java)
        } catch (e: Exception) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 400
                body = "Invalid request format"
            }
        }

        val areFieldsBlank = request.phoneNumber.isBlank() || request.name.isBlank()
        if (areFieldsBlank) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 400
                body = "Phone number and name cannot be blank"
            }
        }

        val user = User(
            phoneNumber = request.phoneNumber,
            name = request.name
        )

        val wasAcknowledged = runBlocking {
            userDataSource.insertUser(user)
        }
        if (!wasAcknowledged) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 409
                body = "User already exists or could not be created"
            }
        }

        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim(name = "userId", value = user.id.toString())
        )

        val response = AuthResponse(token)

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            body = gson.toJson(response)
        }
    }


    private fun signIn(requestBody: String): APIGatewayProxyResponseEvent {
        val gson = Gson()
        val request = try {
            gson.fromJson(requestBody, AuthRequest1::class.java)
        } catch (e: Exception) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 400
                body = "Invalid request format"
            }
        }

        val user = runBlocking {
            userDataSource.getUserByPhoneNumber(request.phoneNumber)
        }

        if (user == null) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 401
                body = "Invalid phone number"
            }
        }

        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim(name = "userId", value = user.id.toString())
        )

        val response = AuthResponse(token)

        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            body = gson.toJson(response)
        }
    }


    private fun authenticate(authHeader: String?): APIGatewayProxyResponseEvent {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 401
                body = "Unauthorized: No token provided"
            }
        }

        val token = authHeader.removePrefix("Bearer ")

        return try {
            val verifier = JWT.require(Algorithm.HMAC256(tokenConfig.secretKey))
                .withAudience(tokenConfig.audience)
                .withIssuer(tokenConfig.issuer)
                .build()

            val decodedJWT = verifier.verify(token)
            val userId = decodedJWT.getClaim("userId").asString()

            APIGatewayProxyResponseEvent().apply {
                statusCode = 200
                body = "Welcome! Your userId is $userId"
            }
        } catch (e: Exception) {
            APIGatewayProxyResponseEvent().apply {
                statusCode = 401
                body = "Invalid token"
            }
        }
    }

    private fun getSecretInfo(authHeader: String?): APIGatewayProxyResponseEvent {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return APIGatewayProxyResponseEvent().apply {
                statusCode = 401
                body = "Unauthorized: No token provided"
            }
        }

        val token = authHeader.removePrefix("Bearer ")

        return try {
            val verifier = JWT.require(Algorithm.HMAC256(tokenConfig.secretKey))
                .withAudience(tokenConfig.audience)
                .withIssuer(tokenConfig.issuer)
                .build()

            val decodedJWT = verifier.verify(token)
            val userId = decodedJWT.getClaim("userId").asString()

            APIGatewayProxyResponseEvent().apply {
                statusCode = 200
                body = "Super secret information for userId: $userId"
            }
        } catch (e: Exception) {
            APIGatewayProxyResponseEvent().apply {
                statusCode = 401
                body = "Invalid token"
            }
        }
    }

}
