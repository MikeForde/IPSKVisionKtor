package com.example

import com.example.compression.*
import com.example.encryption.CryptoHelper
import com.example.serviceHelpers.*
import dev.kilua.rpc.applyRoutes
import dev.kilua.rpc.getServiceManager
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.response.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.get
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.and

fun Application.apiRoutes() {
  routing {
    // RPC-generated routes
    applyRoutes(getServiceManager<IIPSService>())

    // Patient name endpoint
    post("/api/patientName") {
      val resp = PatientService(call).getPatientName()
      call.respond(resp)
    }

    // GET endpoint that returns a IPS record - enables QR url to be used
    // The url will be $base_url/api/ipsRecord?id=1234
    // The id will actually be the packageUUID

    get("/api/ipsRecord") {
      val id = call.request.queryParameters["id"]
      val ips =
          if (id != null) {
            IpsService(call).getIpsRecordByPackageUUId(id)
          } else {
            null
          }
      if (ips != null) call.respond(ips)
      else call.respond(HttpStatusCode.NotFound, "No IPS records not found")
    }

    // IPS record endpoints
    post("/api/ipsRecord") {
      val ips = IpsService(call).getIpsRecord()
      if (ips != null) call.respond(ips)
      else call.respond(HttpStatusCode.NotFound, "No IPS records not found")
    }
    post("/api/ipsRecordByName") {
      val ips = IpsService(call).getIpsRecordByName()
      if (ips != null) call.respond(ips)
      else call.respond(HttpStatusCode.NotFound, "No IPS records not found")
    }

    // IPS record creation fron IPS Bundle - first convert (using convertIPSBundleToSchema) then add
    // (using addIPSRecord)
    post("/api/ipsRecordFromBundle") {
      val json = call.receive<JsonObject>()
      val model = convertIPSBundleToSchema(json)
      val addedModel = addIPSRecord(model)
      call.respond(addedModel)
    }

    post("/api/ipsRecordFromHL72_x") {
      val raw = call.receive<String>()
      val ips = parseHL72_xToIpsModel(raw)
      val addedModel = addIPSRecord(ips)
      call.respond(addedModel)
    }

    // Conversion endpoints - TO schema
    post("/api/convertIPS") {
      val json = call.receive<JsonObject>()
      val model = convertIPSBundleToSchema(json)
      call.respond(model)
    }

    post("/api/parseHL7") {
      val raw = call.receive<String>()
      val ips = parseHL72_xToIpsModel(raw)
      call.respond(ips)
    }

    post("/api/parseBEER") {
      val raw = call.receive<String>()
      val ips = parseBeer(raw)
      call.respond(ips)
    }

    // Conversion endpoints - FROM schema
    post("/api/convertSchemaToUnified") {
      val model = call.receive<IPSModel>()
      val json = generateIPSBundleUnified(model)
      call.respond(json)
    }
    post("/api/convertSchemaToHL72_3") {
      val model = call.receive<IPSModel>()
      val hl7 = generateIpsModelToHl72_3(model)
      call.respondText(hl7)
    }
    post("/api/convertSchemaToBEER") {
      val delim = call.request.queryParameters["delim"] ?: "newline"
      val model = call.receive<IPSModel>()
      val beer = generateIpsBeer(model, delim)
      call.respondText(beer)
    }

    // Encryption endpoints
    post("/api/encryptText") {
      val input = call.receive<String>()
      val payload = CryptoHelper.encrypt(input, useBase64 = true)
      call.respond(payload)
    }
    post("/api/decryptText") {
      val payload = call.receive<CryptoHelper.EncryptedPayload>()
      val decryptedBytes = CryptoHelper.decrypt(payload, useBase64 = true)
      val text = decryptedBytes.toString(Charsets.UTF_8)
      call.respondText(text)
    }
    post("/api/encryptBinary") {
      val inputBytes = call.receiveStream().readBytes()
      val encrypted = CryptoHelper.encryptBinary(inputBytes)
      call.respondBytes(encrypted, ContentType.Application.OctetStream)
    }
    post("/api/decryptBinary") {
      println("Decrypting binary data")
      val inputBytes = call.receiveStream().readBytes()
      val decrypted = CryptoHelper.decryptBinary(inputBytes)
      call.respondBytes(decrypted, ContentType.Application.OctetStream)
    }

    // CBOR encryption endpoints - worked alone but couldn't integrate with the rest
    post("/api/decryptBinaryCbor") {
      println("Decrypting binary data using Cbor")
      val inputBytes = call.receive<ByteArray>()
      val outputBytes = CryptoHelper.decryptBinary(inputBytes)
      call.respond(outputBytes)
    }
    post("/api/TestCBor") {
      println("Decrypting binary data using Cbor")
      val inputBytes = call.receive<ByteArray>()
      call.respond(inputBytes)
    }

    // Compression endpoints
    post("/api/gzipEncode") {
      val input = call.receive<String>()
      val compressed = gzipEncode(input)
      call.respondBytes(compressed, ContentType.Application.OctetStream)
    }
    post("/api/gzipDecode") {
      val compressed = call.receive<ByteArray>()
      val decompressed = gzipDecode(compressed)
      call.respondText(decompressed)
    }

    // Combined compression and encryption
    // Binary version
    post("/api/gzipEncrypt") {
      val input = call.receive<String>()
      val compressed = gzipEncode(input)
      val encrypted = CryptoHelper.encryptBinary(compressed)
      call.respondBytes(encrypted, ContentType.Application.OctetStream)
    }
    post("/api/gzipDecrypt") {
      val encrypted = call.receive<ByteArray>()
      val decrypted = CryptoHelper.decryptBinary(encrypted)
      val decompressed = gzipDecode(decrypted)
      call.respondText(decompressed)
    }
    // Text version
    post("/api/gzipEncryptText") {
      val input = call.receive<String>()
      val compressed = gzipEncode(input)
      val encrypted = CryptoHelper.encrypt(compressed, useBase64 = true)
      call.respond(encrypted)
    }
    post("/api/decryptUnzipText") {
      val encrypted = call.receive<CryptoHelper.EncryptedPayload>()
      val decryptedBytes = CryptoHelper.decrypt(encrypted, useBase64 = true)
      val decompressed = gzipDecode(decryptedBytes)
      call.respondText(decompressed)
    }
  }
}
