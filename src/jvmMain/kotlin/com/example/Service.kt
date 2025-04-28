
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
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.joda.time.DateTime
import java.sql.ResultSet
import java.time.ZoneId

suspend fun <RESP> ApplicationCall.withProfile(block: suspend (Profile) -> RESP): RESP {
    val profile = this.sessions.get<Profile>()
    return profile?.let {
        block(profile)
    } ?: throw IllegalStateException("Profile not set!")
}

class AddressService(private val call: ApplicationCall) : IAddressService {

    override suspend fun getAddressList(search: String?, types: String, sort: Sort) =
        call.withProfile { profile ->
            dbQuery {
                val query = query {
                    select("SELECT * FROM address")
                    whereGroup {
                        where("user_id = :user_id")
                        parameter("user_id", profile.id)
                        search?.let {
                            where(
                                """(lower(first_name) like :search
                            OR lower(last_name) like :search
                            OR lower(email) like :search
                            OR lower(phone) like :search
                            OR lower(postal_address) like :search)""".trimMargin()
                            )
                            parameter("search", "%${it.lowercase()}%")
                        }
                        if (types == "fav") {
                            where("favourite")
                        }
                    }
                    when (sort) {
                        Sort.FN -> orderBy("lower(first_name)")
                        Sort.LN -> orderBy("lower(last_name)")
                        Sort.E -> orderBy("lower(email)")
                        Sort.F -> orderBy("favourite")
                    }
                }
                queryList(query.sql, query.parameters) {
                    toAddress(it)
                }
            }
        }

    override suspend fun addAddress(address: Address) = call.withProfile { profile ->
        val key = dbQuery {
            (AddressDao.insert {
                it[firstName] = address.firstName
                it[lastName] = address.lastName
                it[email] = address.email
                it[phone] = address.phone
                it[postalAddress] = address.postalAddress
                it[favourite] = address.favourite ?: false
                it[createdAt] = DateTime()
                it[userId] = profile.id!!

            } get AddressDao.id)
        }
        getAddress(key)!!
    }

    override suspend fun updateAddress(address: Address) = call.withProfile { profile ->
        address.id?.let {
            getAddress(it)?.let { oldAddress ->
                dbQuery {
                    AddressDao.update({ AddressDao.id eq it }) {
                        it[firstName] = address.firstName
                        it[lastName] = address.lastName
                        it[email] = address.email
                        it[phone] = address.phone
                        it[postalAddress] = address.postalAddress
                        it[favourite] = address.favourite ?: false
                        it[createdAt] = oldAddress.createdAt
                            ?.let { DateTime(java.util.Date.from(it.atZone(ZoneId.systemDefault()).toInstant())) }
                        it[userId] = profile.id!!
                    }
                }
            }
            getAddress(it)
        } ?: throw IllegalArgumentException("The ID of the address not set")
    }

    override suspend fun deleteAddress(id: Int): Boolean = call.withProfile { profile ->
        dbQuery {
            AddressDao.deleteWhere { (AddressDao.userId eq profile.id!!) and (AddressDao.id eq id) } > 0
        }
    }

    private suspend fun getAddress(id: Int): Address? = dbQuery {
        AddressDao.select {
            AddressDao.id eq id
        }.mapNotNull { toAddress(it) }.singleOrNull()
    }

    private fun toAddress(row: ResultRow): Address =
        Address(
            id = row[AddressDao.id],
            firstName = row[AddressDao.firstName],
            lastName = row[AddressDao.lastName],
            email = row[AddressDao.email],
            phone = row[AddressDao.phone],
            postalAddress = row[AddressDao.postalAddress],
            favourite = row[AddressDao.favourite],
            createdAt = row[AddressDao.createdAt]?.millis?.let { java.util.Date(it) }?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime(),
            userId = row[AddressDao.userId]
        )

    private fun toAddress(rs: ResultSet): Address =
        Address(
            id = rs.getInt(AddressDao.id.name),
            firstName = rs.getString(AddressDao.firstName.name),
            lastName = rs.getString(AddressDao.lastName.name),
            email = rs.getString(AddressDao.email.name),
            phone = rs.getString(AddressDao.phone.name),
            postalAddress = rs.getString(AddressDao.postalAddress.name),
            favourite = rs.getBoolean(AddressDao.favourite.name),
            createdAt = rs.getTimestamp(AddressDao.createdAt.name)?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime(),
            userId = rs.getInt(AddressDao.userId.name),
        )
}

class ProfileService(private val call: ApplicationCall) : IProfileService {

    override suspend fun getProfile() = call.withProfile { it }

}

class RegisterProfileService : IRegisterProfileService {

    override suspend fun registerProfile(profile: Profile, password: String): Boolean {
        try {
            dbQuery {
                UserDao.insert {
                    it[this.name] = profile.name!!
                    it[this.username] = profile.username!!
                    it[this.password] = DigestUtils.sha256Hex(password)
                }
            }
        } catch (e: Exception) {
            throw Exception("Register operation failed!")
        }
        return true
    }

}

/** Request DTO for our new endpoint */
@kotlinx.serialization.Serializable
data class PatientNameRequest(val id: String)

/** Response DTO for our new endpoint */
@kotlinx.serialization.Serializable
data class PatientNameResponse(val patientName: String?)

/** A tiny service to fetch patientName by IPSModel id */
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

class IPSService(private val call: ApplicationCall) {

    suspend fun getFullIPS(): com.example.IPSModel {
        // a) bind the incoming { "id": "<packageUUID>" }
        val req = call.receive<PatientNameRequest>()

        // b) run a single dbQuery block to fetch parent + children
        return Db.dbQuery {
            // — fetch the parent row or 404
            val row = IPSModelDao
              .select { IPSModelDao.packageUUID eq req.id }
              .singleOrNull()
              ?: throw NotFoundException("No IPS record for UUID=${req.id}")

            // pull out the PK to use in child FKs
            val ipsId = row[IPSModelDao.id]

            // — helper to convert Joda DateTime → java.time.LocalDateTime
            fun DateTime.toLocal() =
                this.toDate().toInstant()
                  .atZone(ZoneId.systemDefault())
                  .toLocalDateTime()
            //   java.util.Date(this.millis)
            //     .toInstant()
            //     .atZone(ZoneId.systemDefault())
            //     .toLocalDateTime()

            // — load each child list
            val meds = MedicationDao
              .select { MedicationDao.ipsModelId eq ipsId }
              .map { rr ->
                com.example.Medication(
                  id            = rr[MedicationDao.id],
                  name          = rr[MedicationDao.name],
                  date          = rr[MedicationDao.date].toLocal(),
                  dosage        = rr[MedicationDao.dosage],
                  system        = rr[MedicationDao.system],
                  code          = rr[MedicationDao.code],
                  status        = rr[MedicationDao.status],
                  ipsModelId    = rr[MedicationDao.ipsModelId]
                )
              }

            // repeat for each of the other child tables…
            val alls = AllergyDao
              .select { AllergyDao.ipsModelId eq ipsId }
              .map { rr ->
                com.example.Allergy(
                  id            = rr[AllergyDao.id],
                  name          = rr[AllergyDao.name],
                  criticality   = rr[AllergyDao.criticality],
                  date          = rr[AllergyDao.date].toLocal(),
                  system        = rr[AllergyDao.system],
                  code          = rr[AllergyDao.code],
                  ipsModelId    = rr[AllergyDao.ipsModelId]
                )
              }

            val conds = ConditionDao
              .select { ConditionDao.ipsModelId eq ipsId }
              .map { rr ->
                com.example.Condition(
                  id            = rr[ConditionDao.id],
                  name          = rr[ConditionDao.name],
                  date          = rr[ConditionDao.date].toLocal(),
                  system        = rr[ConditionDao.system],
                  code          = rr[ConditionDao.code],
                  ipsModelId    = rr[ConditionDao.ipsModelId]
                )
              }

            val obs = ObservationDao
              .select { ObservationDao.ipsModelId eq ipsId }
              .map { rr ->
                com.example.Observation(
                  id            = rr[ObservationDao.id],
                  name          = rr[ObservationDao.name],
                  date          = rr[ObservationDao.date].toLocal(),
                  value         = rr[ObservationDao.value],
                  system        = rr[ObservationDao.system],
                  code          = rr[ObservationDao.code],
                  valueCode     = rr[ObservationDao.valueCode],
                  bodySite      = rr[ObservationDao.bodySite],
                  status        = rr[ObservationDao.status],
                  ipsModelId    = rr[ObservationDao.ipsModelId]
                )
              }

            val imms = ImmunizationDao
              .select { ImmunizationDao.ipsModelId eq ipsId }
              .map { rr ->
                com.example.Immunization(
                  id            = rr[ImmunizationDao.id],
                  name          = rr[ImmunizationDao.name],
                  system        = rr[ImmunizationDao.system],
                  date          = rr[ImmunizationDao.date].toLocal(),
                  code          = rr[ImmunizationDao.code],
                  status        = rr[ImmunizationDao.status],
                  ipsModelId    = rr[ImmunizationDao.ipsModelId]
                )
              }

            // — assemble and return the shared‐model
            com.example.IPSModel(
              id                  = ipsId,
              packageUUID         = row[IPSModelDao.packageUUID],
              timeStamp           = row[IPSModelDao.timeStamp].toLocal(),
              patientName         = row[IPSModelDao.patientName],
              patientGiven        = row[IPSModelDao.patientGiven],
              patientDob          = row[IPSModelDao.patientDob].toLocal(),
              patientGender       = row[IPSModelDao.patientGender],
              patientNation       = row[IPSModelDao.patientNation],
              patientPractitioner = row[IPSModelDao.patientPractitioner],
              patientOrganization = row[IPSModelDao.patientOrganization],
              patientIdentifier   = row[IPSModelDao.patientIdentifier],
              medications         = meds,
              allergies           = alls,
              conditions          = conds,
              observations        = obs,
              immunizations       = imms
            )
        }
    }
}
