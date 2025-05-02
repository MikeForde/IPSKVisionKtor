package com.example

import com.example.Db.dbQuery
import com.example.serviceHelpers.toFullIpsModel
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.sessions.get
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
}
