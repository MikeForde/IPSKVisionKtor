
package com.example

import com.example.Db.dbQuery
import com.example.Db.queryList
import com.github.andrewoma.kwery.core.builder.query
import io.ktor.server.application.ApplicationCall
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.plugins.NotFoundException
import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.lowerCase
// import org.joda.time.DateTime
import java.sql.ResultSet
// import java.time.ZoneId
// import kotlinx.datetime.Instant
// import kotlinx.datetime.TimeZone
// import kotlinx.datetime.toLocalDateTime
// import kotlinx.datetime.LocalDateTime

// private fun DateTime.toKvLocal(): LocalDateTime =
//     Instant.fromEpochMilliseconds(this.millis)
//         .toLocalDateTime(TimeZone.currentSystemDefault())



/** Request DTO for our new endpoint */
@kotlinx.serialization.Serializable
data class PatientNameRequest(val id: String)

/** Response DTO for our new endpoint */
@kotlinx.serialization.Serializable
data class PatientNameResponse(val patientName: String?)

/** A tiny service to fetch patientName by IPSModel id */
@kotlinx.serialization.Serializable
class PatientService(private val call: ApplicationCall) {

    /** 
     * Reads JSON { id: Int } from the body, queries ipsAlt,
     * and returns the patientName (or null if not found). 
     */
    suspend fun getPatientName(): PatientNameResponse {
        // 1) bind the incoming JSON to our request DTO
        val req = call.receive<PatientNameRequest>()

        // 2) do a simple dbQuery against the IPSModelDao
        val name = dbQuery {
            IPSModelDao
              .select { IPSModelDao.packageUUID eq req.id }
              .map { row -> row[IPSModelDao.patientName] }
              .singleOrNull()
        }

        // 3) wrap it in our response DTO
        return PatientNameResponse(name)
    }
}


// DTO for the incoming JSON
@kotlinx.serialization.Serializable
data class IpsRequest(val id: String)

@kotlinx.serialization.Serializable
data class IpsNameRequest(val name: String)

// A tiny service just for IPS lookups
class IpsService(private val call: ApplicationCall) {

    suspend fun getIpsRecord(): IPSModel? {
        val req = call.receive<IpsRequest>()
        return Db.dbQuery {
            IPSModelDao
              .select { IPSModelDao.packageUUID eq req.id }
              .singleOrNull()
              ?.let { toFullIpsModel(it) }
        }
    }

    suspend fun getIpsRecordByName(): IPSModel? {
        val req = call.receive<IpsNameRequest>()
        return Db.dbQuery {
            IPSModelDao
              .select { IPSModelDao.patientName eq req.name }
              .singleOrNull()
              ?.let { toFullIpsModel(it) }
        }
    }
}


/** RPC implementation for listing all IPS records */
class IPSServiceRpc(private val call: ApplicationCall) : IIPSService {
     override suspend fun getIPSList(): List<IPSModel> = Db.dbQuery {
        IPSModelDao.selectAll().map { toFullIpsModel(it) }
    }

    override suspend fun findByLastName(surname: String): List<IPSModel> = Db.dbQuery {
        val pattern = "%${surname.lowercase()}%"
        IPSModelDao
          .select { IPSModelDao.patientName.lowerCase() like pattern }
          .map { toFullIpsModel(it) }
    }
}

// maps one Exposed ResultRow into a fully-loaded IPSModel, including medications, allergies, etc.
private fun toFullIpsModel(row: ResultRow): IPSModel {
    val pk = row[IPSModelDao.id]
    val meds = MedicationDao.select { MedicationDao.ipsModelId eq pk }.map { m ->
        Medication(
            id         = m[MedicationDao.id],
            name       = m[MedicationDao.name],
            date       = m[MedicationDao.date].toString(),
            dosage     = m[MedicationDao.dosage],
            system     = m[MedicationDao.system],
            code       = m[MedicationDao.code],
            status     = m[MedicationDao.status],
            ipsModelId = m[MedicationDao.ipsModelId]
        )
    }
    val allgs = AllergyDao.select { AllergyDao.ipsModelId eq pk }.map { a ->
        Allergy(
            id          = a[AllergyDao.id],
            name        = a[AllergyDao.name],
            date        = a[AllergyDao.date].toString(),
            criticality = a[AllergyDao.criticality],
            system      = a[AllergyDao.system],
            code        = a[AllergyDao.code],
            ipsModelId  = a[AllergyDao.ipsModelId]
        )
    }
    val conds = ConditionDao.select { ConditionDao.ipsModelId eq pk }.map { c ->
        Condition(
            id         = c[ConditionDao.id],
            name       = c[ConditionDao.name],
            date       = c[ConditionDao.date].toString(),
            system     = c[ConditionDao.system],
            code       = c[ConditionDao.code],
            ipsModelId = c[ConditionDao.ipsModelId]
        )
    }
    val obs = ObservationDao.select { ObservationDao.ipsModelId eq pk }.map { o ->
        Observation(
            id            = o[ObservationDao.id],
            name          = o[ObservationDao.name],
            date          = o[ObservationDao.date].toString(),
            value         = o[ObservationDao.value],
            system        = o[ObservationDao.system],
            code          = o[ObservationDao.code],
            valueCode     = o[ObservationDao.valueCode],
            bodySite      = o[ObservationDao.bodySite],
            status        = o[ObservationDao.status],
            ipsModelId    = o[ObservationDao.ipsModelId]
        )
    }
    val imms = ImmunizationDao.select { ImmunizationDao.ipsModelId eq pk }.map { i ->
        Immunization(
            id         = i[ImmunizationDao.id],
            name       = i[ImmunizationDao.name],
            system     = i[ImmunizationDao.system],
            date       = i[ImmunizationDao.date].toString(),
            code       = i[ImmunizationDao.code],
            status     = i[ImmunizationDao.status],
            ipsModelId = i[ImmunizationDao.ipsModelId]
        )
    }
    return IPSModel(
        id                  = pk,
        packageUUID         = row[IPSModelDao.packageUUID],
        timeStamp           = row[IPSModelDao.timeStamp].toString(),
        patientName         = row[IPSModelDao.patientName],
        patientGiven        = row[IPSModelDao.patientGiven],
        patientDob          = row[IPSModelDao.patientDob].toString(),
        patientGender       = row[IPSModelDao.patientGender],
        patientNation       = row[IPSModelDao.patientNation],
        patientPractitioner = row[IPSModelDao.patientPractitioner],
        patientOrganization = row[IPSModelDao.patientOrganization],
        patientIdentifier   = row[IPSModelDao.patientIdentifier],
        medications         = meds,
        allergies           = allgs,
        conditions          = conds,
        observations        = obs,
        immunizations       = imms
    )
}

