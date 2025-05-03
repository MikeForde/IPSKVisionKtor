package com.example

import com.example.Db.dbQuery
import com.example.encryption.CryptoHelper
import com.example.serviceHelpers.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.sessions.get
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import com.example.toInternal
import java.util.Base64




// Simple test for API POST request
@kotlinx.serialization.Serializable data class PatientNameRequest(val id: String)

@kotlinx.serialization.Serializable data class PatientNameResponse(val patientName: String?)

@kotlinx.serialization.Serializable
class PatientService(private val call: ApplicationCall) {

  /**
   * Reads JSON { id: Int } from the body, queries ipsAlt, and returns the patientName (or null if
   * not found).
   */
  suspend fun getPatientName(): PatientNameResponse {
    // 1) bind the incoming JSON to our request DTO
    val req = call.receive<PatientNameRequest>()

    // 2) do a simple dbQuery against the IPSModelDao
    val name = dbQuery {
      IPSModelDao.select { IPSModelDao.packageUUID eq req.id }
          .map { row -> row[IPSModelDao.patientName] }
          .singleOrNull()
    }

    // 3) wrap it in our response DTO
    return PatientNameResponse(name)
  }
}

// DTO for the incoming JSON
@kotlinx.serialization.Serializable data class IpsRequest(val id: String)

@kotlinx.serialization.Serializable data class IpsNameRequest(val name: String)

// REST methods for IPS lookups
class IpsService(private val call: ApplicationCall) {

  suspend fun getIpsRecord(): IPSModel? {
    val req = call.receive<IpsRequest>()
    return Db.dbQuery {
      IPSModelDao.select { IPSModelDao.packageUUID eq req.id }
          .singleOrNull()
          ?.let { toFullIpsModel(it) }
    }
  }

  suspend fun getIpsRecordByName(): IPSModel? {
    val req = call.receive<IpsNameRequest>()
    return Db.dbQuery {
      IPSModelDao.select { IPSModelDao.patientName eq req.name }
          .singleOrNull()
          ?.let { toFullIpsModel(it) }
    }
  }
}

// @kotlinx.serialization.Serializable
// data class EncryptRequest(val data: String, val useBase64: Boolean = false)

// @kotlinx.serialization.Serializable
// data class EncryptedPayloadDTO(
//     val encryptedData: String,
//     val iv: String,
//     val mac: String,
//     val key: String
// )

// @kotlinx.serialization.Serializable
// data class DecryptRequest(
//     val encryptedData: String,
//     val iv: String,
//     val mac: String,
//     val useBase64: Boolean = false
// )

// @kotlinx.serialization.Serializable
// data class BinaryEncryptRequest(val data: String) // base64-encoded input
// @kotlinx.serialization.Serializable
// data class BinaryEncryptResponse(val data: String) // base64-encoded result

// @kotlinx.serialization.Serializable
// data class BinaryDecryptRequest(val data: String) // base64-encoded input
// @kotlinx.serialization.Serializable
// data class BinaryDecryptResponse(val data: String) // base64-encoded output



/** RPC implementation for listing all IPS records */
class IPSServiceRpc(private val call: ApplicationCall) : IIPSService {
  override suspend fun getIPSList(): List<IPSModel> =
      Db.dbQuery { IPSModelDao.selectAll().map { toFullIpsModel(it) } }

  override suspend fun findByLastName(surname: String): List<IPSModel> =
      Db.dbQuery {
        val pattern = "%${surname.lowercase()}%"
        IPSModelDao.select { IPSModelDao.patientName.lowerCase() like pattern }
            .map { toFullIpsModel(it) }
      }

  override suspend fun generateUnifiedBundle(id: Int?): String {
    if (id == null) {
      return """{ "error": "No ID provided" }"""
    }

    val model =
        Db.dbQuery {
          IPSModelDao.select { IPSModelDao.id eq id }.singleOrNull()?.let { toFullIpsModel(it) }
        }

    return if (model != null) {
      val json = generateIPSBundleUnified(model)
      json.toString() // return JSON as a string
    } else {
      """{ "error": "Record not found" }"""
    }
  }

  override suspend fun convertBundleToSchema(bundleJson: String): String {
    // 1) parse the incoming raw JSON
    val jsonObj = Json.parseToJsonElement(bundleJson).jsonObject
    // 2) convert to IPSModel
    val model = convertIPSBundleToSchema(jsonObj)
    // 3) re-serialize the IPSModel back to JSON text
    return Json.encodeToString(model)
  }

  override suspend fun encryptText(data: String, useBase64: Boolean): EncryptedPayloadDTO {
    val payload = CryptoHelper.encrypt(data, useBase64)
    return EncryptedPayloadDTO(payload.encryptedData, payload.iv, payload.mac, payload.key)
}

override suspend fun decryptText(encryptedData: String, iv: String, mac: String, useBase64: Boolean): String {
    val payloadDTO = EncryptedPayloadDTO(encryptedData, iv, mac, key = "")
    val decrypted = CryptoHelper.decrypt(payloadDTO.toInternal(), useBase64)
    return decrypted.toString(Charsets.UTF_8)
}

override suspend fun encryptBinary(data: String): BinaryEncryptResponse {
    val raw = Base64.getDecoder().decode(data)
    val encrypted = CryptoHelper.encryptBinary(raw)
    return BinaryEncryptResponse(Base64.getEncoder().encodeToString(encrypted))
}

override suspend fun decryptBinary(data: String): BinaryDecryptResponse {
    val raw = Base64.getDecoder().decode(data)
    val decrypted = CryptoHelper.decryptBinary(raw)
    return BinaryDecryptResponse(Base64.getEncoder().encodeToString(decrypted))
}

}
