package com.example.encryption

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable

object CryptoHelper {
  private const val AES_ALGORITHM = "AES"
  private const val AES_MODE = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH = 16 // in bytes
  private const val IV_LENGTH = 16 // in bytes

  // 32-byte key (for AES-256) – should be securely stored in production
  private val keyBytes = "YOUR_AES_256_KEY_123456789012345".toByteArray(Charsets.UTF_8)
  private val keySpec = SecretKeySpec(keyBytes, AES_ALGORITHM)
  private val random = SecureRandom()

  @Serializable
  data class EncryptedPayload(
      val encryptedData: String,
      val iv: String,
      val mac: String,
      val key: String
  )

  fun encrypt(data: String, useBase64: Boolean = false): EncryptedPayload {
    val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
    val cipher = Cipher.getInstance(AES_MODE)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

    val plainBytes = data.toByteArray(Charsets.UTF_8)
    val encryptedBytes = cipher.doFinal(plainBytes)
    val mac = encryptedBytes.takeLast(GCM_TAG_LENGTH).toByteArray()
    val ciphertext = encryptedBytes.dropLast(GCM_TAG_LENGTH).toByteArray()

    return EncryptedPayload(
        encryptedData = ciphertext.encode(useBase64),
        iv = iv.encode(useBase64),
        mac = mac.encode(useBase64),
        key = keyBytes.encode(useBase64))
  }

  fun decrypt(payload: EncryptedPayload, useBase64: Boolean = false): ByteArray {
    val ciphertext = payload.encryptedData.decode(useBase64)
    val iv = payload.iv.decode(useBase64)
    val mac = payload.mac.decode(useBase64)

    val cipher = Cipher.getInstance(AES_MODE)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

    // Combine ciphertext and mac to get full encrypted blob
    val encryptedBlob = ciphertext + mac
    return cipher.doFinal(encryptedBlob)
  }

  fun encryptBinary(data: ByteArray): ByteArray {
    val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
    val cipher = Cipher.getInstance(AES_MODE)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

    val encrypted = cipher.doFinal(data)
    val mac = encrypted.takeLast(GCM_TAG_LENGTH).toByteArray()
    val ciphertext = encrypted.dropLast(GCM_TAG_LENGTH).toByteArray()

    return iv + mac + ciphertext
  }

  fun decryptBinary(data: ByteArray): ByteArray {
    val iv = data.sliceArray(0 until IV_LENGTH)
    val mac = data.sliceArray(IV_LENGTH until IV_LENGTH + GCM_TAG_LENGTH)
    val ciphertext = data.sliceArray(IV_LENGTH + GCM_TAG_LENGTH until data.size)

    val cipher = Cipher.getInstance(AES_MODE)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

    val encryptedBlob = ciphertext + mac
    return cipher.doFinal(encryptedBlob)
  }

  private fun ByteArray.encode(base64: Boolean) =
      if (base64) Base64.getEncoder().encodeToString(this)
      else this.joinToString("") { "%02x".format(it) }

  private fun String.decode(base64: Boolean) =
      if (base64) Base64.getDecoder().decode(this)
      else chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
