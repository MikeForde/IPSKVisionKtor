package com.example

import dev.kilua.rpc.annotations.RpcService
import kotlinx.serialization.Serializable

@Serializable
enum class Sort {
  FN,
  LN,
  E,
  F
}

@RpcService
interface IIPSService {
  suspend fun getIPSList(): List<IPSModel>

  suspend fun findByLastName(surname: String): List<IPSModel>

  suspend fun generateUnifiedBundle(id: Int?): String

  suspend fun convertBundleToSchema(bundleJson: String): String

  // Encrypt and decrypt services
  suspend fun encryptText(data: String, useBase64: Boolean): EncryptedPayloadDTO
  suspend fun decryptText(encryptedData: String, iv: String, mac: String, useBase64: Boolean): String
  suspend fun encryptBinary(data: String): BinaryEncryptResponse
  suspend fun decryptBinary(data: String): BinaryDecryptResponse

  
}
