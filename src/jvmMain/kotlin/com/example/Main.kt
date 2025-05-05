package com.example

import com.example.encryption.CryptoHelper
import com.example.serviceHelpers.*
import dev.kilua.rpc.applyRoutes
import dev.kilua.rpc.getServiceManager
import dev.kilua.rpc.initRpc
import dev.kilua.rpc.registerService
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
import io.kvision.remote.registerRemoteTypes
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.and

fun Application.main() {

  val frontHost = System.getenv("FRONTEND_HOST")
    ?: error("FRONTEND_HOST must be set (just host[:port], no scheme)")

  val frontScheme = System.getenv("FRONTEND_SCHEME")
    ?: error("FRONTEND_SCHEME must be set (http or https)")

  registerRemoteTypes()
  install(Compression)
  install(DefaultHeaders)
  install(CallLogging)
  install(CORS) {
    allowHost(
        host = "$frontHost",
        schemes = listOf("$frontScheme"),
    )
    allowMethod(HttpMethod.Options) // Allows preflight requests
    allowMethod(HttpMethod.Post) // Allows POST requests
    allowHeader(HttpHeaders.ContentType) // Allows 'Content-Type' header
    allowNonSimpleContentTypes =
        true // Allows non-simple content types like 'application/octet-stream'
  }

  Db.init(environment.config)

  routing {
    applyRoutes(getServiceManager<IIPSService>())
    post("/api/patientName") {
      val resp = PatientService(call).getPatientName()
      call.respond(resp)
    }
    // new “full IPS” endpoint
    post("/api/ipsRecord") {
      val ips = IpsService(call).getIpsRecord()
      if (ips != null) {
        call.respond(ips)
      } else {
        call.respond(HttpStatusCode.NotFound, "No IPS records not found")
      }
    }
    // Get by name
    post("/api/ipsRecordByName") {
      val ips = IpsService(call).getIpsRecordByName()
      if (ips != null) {
        call.respond(ips)
      } else {
        call.respond(HttpStatusCode.NotFound, "No IPS records not found")
      }
    }
    post("/api/convertIPS") {
      val json = call.receive<JsonObject>()
      val model = convertIPSBundleToSchema(json)
      call.respond(model)
    }
    post("/api/convertSchemaToUnified") {
      val model = call.receive<IPSModel>()
      val json = generateIPSBundleUnified(model)
      call.respond(json)
    }
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
      // output message to terminal
      println("Decrypting binary data")
      val inputBytes = call.receiveStream().readBytes()
      val decrypted = CryptoHelper.decryptBinary(inputBytes)
      call.respondBytes(decrypted, ContentType.Application.OctetStream)
    }
    post("/api/decryptBinaryCbor") {
      // output message to terminal
      println("Decrypting binary data using Cbor")
      val inputBytes = call.receive<ByteArray>()
      val outputBytes = CryptoHelper.decryptBinary(inputBytes)
      call.respond(outputBytes)
    }
    post("/api/TestCBor") {
      // output message to terminal
      println("Decrypting binary data using Cbor")
      val inputBytes = call.receive<ByteArray>()
      call.respond(inputBytes)
    }
  }
  initRpc { registerService<IIPSService> { IPSServiceRpc(it) } }
}
