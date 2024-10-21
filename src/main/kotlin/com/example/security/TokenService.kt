package com.example.security

interface TokenService {
    fun generate( config: TokenConfig, vararg claims: TokenClaim): String
}