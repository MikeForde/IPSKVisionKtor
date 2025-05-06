package com.example

import com.example.serviceHelpers.*
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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.kvision.remote.registerRemoteTypes
import org.jetbrains.exposed.sql.and

fun Application.main() {
  val frontHost =
      System.getenv("FRONTEND_HOST")
          ?: error("FRONTEND_HOST must be set (just host[:port], no scheme)")
  val frontScheme =
      System.getenv("FRONTEND_SCHEME") ?: error("FRONTEND_SCHEME must be set (http or https)")

  registerRemoteTypes()
  install(Compression)
  install(DefaultHeaders)
  install(CallLogging)
  install(CORS) {
    allowHost(host = frontHost, schemes = listOf(frontScheme))
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Post)
    allowHeader(HttpHeaders.ContentType)
    allowNonSimpleContentTypes = true
  }

  Db.init(environment.config)

  // Delegate API routing to external file - a bit less cluttered!
  apiRoutes()

  // Initialize RPC after routes
  initRpc { registerService<IIPSService> { IPSServiceRpc(it) } }
}
