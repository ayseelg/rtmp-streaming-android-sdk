package com.example.rtmp.sdk.models

data class User(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
