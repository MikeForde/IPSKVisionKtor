package com.example

import kotlinx.serialization.Serializable

@Serializable data class EncryptRequest(val data: String, val useBase64: Boolean = false)

@Serializable
data class EncryptedPayloadDTO(
    val encryptedData: String,
    val iv: String,
    val mac: String,
    val key: String
)

@Serializable
data class DecryptRequest(
    val encryptedData: String,
    val iv: String,
    val mac: String,
    val useBase64: Boolean = false
)

@Serializable
data class BinaryEncryptRequest(val data: String) // base64-encoded input

@Serializable data class BinaryEncryptResponse(val data: String)

@Serializable data class BinaryDecryptRequest(val data: String)

@Serializable data class BinaryDecryptResponse(val data: String)
