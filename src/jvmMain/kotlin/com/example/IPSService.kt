package com.example

import com.example.Db.dbQuery
import com.example.compression.*
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

// ************* REST methods ****************
// Don't have to do this here, as can just add to ApiRoutes but shown for illustration
// However, these are NOT Rpc methods.
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

  suspend fun getIpsRecordByPackageUUId(id: String): IPSModel? {
    return Db.dbQuery {
      IPSModelDao.select { IPSModelDao.packageUUID eq id }
          .singleOrNull()
          ?.let { toFullIpsModel(it) }
    }
  }
}

// ************** RPC SERVICE ****************
// Need to be 'registered' by being included in the commmonMain Service.kt file
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

  // Conversions from Schema to other formats
  // Generate unified FhiR JSON bundle
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

  // gemerate HL7 2_3
  override suspend fun generateHL7(id: Int?): String {
    if (id == null) {
      return """{ "error": "No ID provided" }"""
    }

    val model =
        Db.dbQuery {
          IPSModelDao.select { IPSModelDao.id eq id }.singleOrNull()?.let { toFullIpsModel(it) }
        }

    return if (model != null) {
      val hl7text = generateIpsModelToHl72_3(model)
      hl7text // return text
    } else {
      """{ "error": "Record not found" }"""
    }
  }

  // Conversions From other formats to Schema
  // Any of kwown project IPS FHiR formats (unified, legacy or expanded) to schema
  override suspend fun convertBundleToSchema(bundleJson: String): String {
    val jsonObj = Json.parseToJsonElement(bundleJson).jsonObject
    val model = convertIPSBundleToSchema(jsonObj)
    return Json.encodeToString(model)
  }

  // Extend the above to convert Bundle to model then add to the database
  override suspend fun addBundleToDatabase(bundleJson: String): String {
    val jsonObj = Json.parseToJsonElement(bundleJson).jsonObject
    val model = convertIPSBundleToSchema(jsonObj)
    val addedModel = addIPSRecord(model)
    return addedModel.id.toString()
  }

  // HL7 2_x to schema (designed to work with 2.3 but would also work with 'legacy' 2.8 - at least
  // as was produced by IPS MERN)
  // Uses parseHL72_xToIpsModel
  override suspend fun convertHL7ToSchema(bundleHL7: String): String {
    val model = parseHL72_xToIpsModel(bundleHL7)
    return Json.encodeToString(model)
  }

  override suspend fun encryptText(data: String, useBase64: Boolean): EncryptedPayloadDTO {
    val payload = CryptoHelper.encrypt(data, useBase64)
    return EncryptedPayloadDTO(payload.encryptedData, payload.iv, payload.mac, payload.key)
  }

  // gzip (using gzipEncode(data: Any): ByteArray) then encrypt
  override suspend fun encryptTextGzip(data: String, useBase64: Boolean): EncryptedPayloadDTO {
    val compressed = gzipEncode(data)
    val payload = CryptoHelper.encrypt(compressed, useBase64)
    return EncryptedPayloadDTO(payload.encryptedData, payload.iv, payload.mac, payload.key)
  }

  override suspend fun decryptText(
      encryptedData: String,
      iv: String,
      mac: String,
      useBase64: Boolean
  ): String {
    val payloadDTO = EncryptedPayloadDTO(encryptedData, iv, mac, key = "")
    val decrypted = CryptoHelper.decrypt(payloadDTO.toInternal(), useBase64)
    return decrypted.toString(Charsets.UTF_8)
  }

  override suspend fun generateQrCode(text: String): String {
    return generateQrCodeBase64(text) // safe!
  }

  // Left to remember that binary encrypt/decrypt doesn't work as Rpc service with Kilua
  // Problem is Rpc Kilua doesn't support ByteArray and the workaround - to use base64 - doesn't
  // work on the frontend
  // As the encode/decode64 frontend libraries don't support non-Latin characters

  //
  // override suspend fun encryptBinary(data: String): BinaryEncryptResponse {
  //   val raw = Base64.getDecoder().decode(data)
  //   val encrypted = CryptoHelper.encryptBinary(raw)
  //   return BinaryEncryptResponse(Base64.getEncoder().encodeToString(encrypted))
  // }

  // override suspend fun decryptBinary(data: String): BinaryDecryptResponse {
  //   val raw = Base64.getDecoder().decode(data)
  //   val decrypted = CryptoHelper.decryptBinary(raw)
  //   return BinaryDecryptResponse(Base64.getEncoder().encodeToString(decrypted))
  // }

  // override suspend fun decryptBinaryCbor(encrypted: ByteArray): ByteArray {
  //   return CryptoHelper.decryptBinary(encrypted)
  // }
}
