package com.example.data

import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull

class MongoUserDataSource(db: MongoDatabase): UserDataSource {
    private val users = db.getCollection<User>("users")
    override suspend fun getUserByPhoneNumber(phoneNumber: String): User? {
        return users.find(eq("phoneNumber", phoneNumber)).firstOrNull()
    }

    override suspend fun insertUser(user: User): Boolean {
        return users.insertOne(user).wasAcknowledged()
    }

}