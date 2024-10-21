package com.example.data

interface UserDataSource {
    suspend fun getUserByPhoneNumber(phoneNumber: String): User?
    suspend fun insertUser(user: User): Boolean
}