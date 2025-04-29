
package com.example

import com.example.Db.dbQuery
import dev.kilua.rpc.getServiceManager
import dev.kilua.rpc.initRpc
import dev.kilua.rpc.registerService
import dev.kilua.rpc.applyRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.kvision.remote.registerRemoteTypes
import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.PatientService    // new service class

fun Application.main() {
    registerRemoteTypes()
    install(Compression)
    install(DefaultHeaders)
    install(CallLogging)

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
            call.respond(HttpStatusCode.NotFound, "IPS record not found")
        }
  }
       
    }
    initRpc {
        registerService<IIPSService> { IPSServiceRpc(it) }
    }
}
