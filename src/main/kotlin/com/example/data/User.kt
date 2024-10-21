package com.example.data

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    val phoneNumber: String,
    val name: String,
    @BsonId val id: ObjectId = ObjectId()
)
