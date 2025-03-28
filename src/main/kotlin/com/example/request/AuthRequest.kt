package com.example.request

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val phoneNumber: String,
    val name: String
)
@Serializable
data class AuthRequest1(
    val phoneNumber: String
)
