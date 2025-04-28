
// package com.example

// import com.example.Db.dbQuery
// import com.example.Db.queryList
// import com.github.andrewoma.kwery.core.builder.query
// import io.ktor.server.application.ApplicationCall
// import io.ktor.server.sessions.get
// import io.ktor.server.sessions.sessions
// import io.ktor.server.request.receive
// import io.ktor.server.response.respond
// import io.ktor.server.plugins.NotFoundException
// import org.apache.commons.codec.digest.DigestUtils
// import org.jetbrains.exposed.sql.ResultRow
// import org.jetbrains.exposed.sql.and
// import org.jetbrains.exposed.sql.deleteWhere
// import org.jetbrains.exposed.sql.insert
// import org.jetbrains.exposed.sql.select
// import org.jetbrains.exposed.sql.update
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
// import org.joda.time.DateTime
// import java.sql.ResultSet
// import java.time.ZoneId
// import kotlinx.datetime.Instant
// import kotlinx.datetime.TimeZone
// import kotlinx.datetime.toLocalDateTime
// import kotlinx.datetime.LocalDateTime

// suspend fun <RESP> ApplicationCall.withProfile(block: suspend (Profile) -> RESP): RESP {
//     val profile = this.sessions.get<Profile>()
//     return profile?.let {
//         block(profile)
//     } ?: throw IllegalStateException("Profile not set!")
// }

// class AddressService(private val call: ApplicationCall) : IAddressService {

//     override suspend fun getAddressList(search: String?, types: String, sort: Sort) =
//         call.withProfile { profile ->
//             dbQuery {
//                 val query = query {
//                     select("SELECT * FROM address")
//                     whereGroup {
//                         where("user_id = :user_id")
//                         parameter("user_id", profile.id)
//                         search?.let {
//                             where(
//                                 """(lower(first_name) like :search
//                             OR lower(last_name) like :search
//                             OR lower(email) like :search
//                             OR lower(phone) like :search
//                             OR lower(postal_address) like :search)""".trimMargin()
//                             )
//                             parameter("search", "%${it.lowercase()}%")
//                         }
//                         if (types == "fav") {
//                             where("favourite")
//                         }
//                     }
//                     when (sort) {
//                         Sort.FN -> orderBy("lower(first_name)")
//                         Sort.LN -> orderBy("lower(last_name)")
//                         Sort.E -> orderBy("lower(email)")
//                         Sort.F -> orderBy("favourite")
//                     }
//                 }
//                 queryList(query.sql, query.parameters) {
//                     toAddress(it)
//                 }
//             }
//         }

//     override suspend fun addAddress(address: Address) = call.withProfile { profile ->
//         val key = dbQuery {
//             (AddressDao.insert {
//                 it[firstName] = address.firstName
//                 it[lastName] = address.lastName
//                 it[email] = address.email
//                 it[phone] = address.phone
//                 it[postalAddress] = address.postalAddress
//                 it[favourite] = address.favourite ?: false
//                 it[createdAt] = DateTime()
//                 it[userId] = profile.id!!

//             } get AddressDao.id)
//         }
//         getAddress(key)!!
//     }

//     override suspend fun updateAddress(address: Address) = call.withProfile { profile ->
//         address.id?.let {
//             getAddress(it)?.let { oldAddress ->
//                 dbQuery {
//                     AddressDao.update({ AddressDao.id eq it }) {
//                         it[firstName] = address.firstName
//                         it[lastName] = address.lastName
//                         it[email] = address.email
//                         it[phone] = address.phone
//                         it[postalAddress] = address.postalAddress
//                         it[favourite] = address.favourite ?: false
//                         it[createdAt] = null
//                         it[userId] = profile.id!!
//                     }
//                 }
//             }
//             getAddress(it)
//         } ?: throw IllegalArgumentException("The ID of the address not set")
//     }

//     override suspend fun deleteAddress(id: Int): Boolean = call.withProfile { profile ->
//         dbQuery {
//             AddressDao.deleteWhere { (AddressDao.userId eq profile.id!!) and (AddressDao.id eq id) } > 0
//         }
//     }

//     private suspend fun getAddress(id: Int): Address? = dbQuery {
//         AddressDao.select {
//             AddressDao.id eq id
//         }.mapNotNull { toAddress(it) }.singleOrNull()
//     }

//     private fun toAddress(row: ResultRow): Address =
//         Address(
//             id = row[AddressDao.id],
//             firstName = row[AddressDao.firstName],
//             lastName = row[AddressDao.lastName],
//             email = row[AddressDao.email],
//             phone = row[AddressDao.phone],
//             postalAddress = row[AddressDao.postalAddress],
//             favourite = row[AddressDao.favourite],
//             createdAt = null,
//             userId = row[AddressDao.userId]
//         )

//     private fun toAddress(rs: ResultSet): Address =
//         Address(
//             id = rs.getInt(AddressDao.id.name),
//             firstName = rs.getString(AddressDao.firstName.name),
//             lastName = rs.getString(AddressDao.lastName.name),
//             email = rs.getString(AddressDao.email.name),
//             phone = rs.getString(AddressDao.phone.name),
//             postalAddress = rs.getString(AddressDao.postalAddress.name),
//             favourite = rs.getBoolean(AddressDao.favourite.name),
//             createdAt = null,
//             userId = rs.getInt(AddressDao.userId.name),
//         )
// }

// class ProfileService(private val call: ApplicationCall) : IProfileService {

//     override suspend fun getProfile() = call.withProfile { it }

// }

// class RegisterProfileService : IRegisterProfileService {

//     override suspend fun registerProfile(profile: Profile, password: String): Boolean {
//         try {
//             dbQuery {
//                 UserDao.insert {
//                     it[this.name] = profile.name!!
//                     it[this.username] = profile.username!!
//                     it[this.password] = DigestUtils.sha256Hex(password)
//                 }
//             }
//         } catch (e: Exception) {
//             throw Exception("Register operation failed!")
//         }
//         return true
//     }

// }

// // /** Request DTO for our new endpoint */
// // @kotlinx.serialization.Serializable
// // data class PatientNameRequest(val id: String)

// // /** Response DTO for our new endpoint */
// // @kotlinx.serialization.Serializable
// // data class PatientNameResponse(val patientName: String?)

// // /** A tiny service to fetch patientName by IPSModel id */
// // @kotlinx.serialization.Serializable
// // class PatientService(private val call: ApplicationCall) {

// //     /** 
// //      * Reads JSON { id: Int } from the body, queries ipsAlt,
// //      * and returns the patientName (or null if not found). 
// //      */
// //     suspend fun getPatientName(): PatientNameResponse {
// //         // 1) bind the incoming JSON to our request DTO
// //         val req = call.receive<PatientNameRequest>()

// //         // 2) do a simple dbQuery against the IPSModelDao
// //         val name = dbQuery {
// //             IPSModelDao
// //               .select { IPSModelDao.packageUUID eq req.id }
// //               .map { row -> row[IPSModelDao.patientName] }
// //               .singleOrNull()
// //         }

// //         // 3) wrap it in our response DTO
// //         return PatientNameResponse(name)
// //     }
// // }


// // // DTO for the incoming JSON
// // @kotlinx.serialization.Serializable
// // data class IpsRequest(val id: String)

// // // A tiny service just for IPS lookups
// // class IpsService(private val call: ApplicationCall) {

// //     suspend fun getIpsRecord(): IPSModel? {
// //         // bind incoming JSON
// //         val req = call.receive<IpsRequest>()

// //         return Db.dbQuery {
// //             // 1) find the main IPS row
// //             val row = IPSModelDao
// //               .select { IPSModelDao.packageUUID eq req.id }
// //               .singleOrNull() ?: return@dbQuery null

// //             val pk = row[IPSModelDao.id]

// //             // 2) load each child collection
// //             val medications = MedicationDao.select { MedicationDao.ipsModelId eq pk }
// //               .map {
// //                 Medication(
// //                   id            = it[MedicationDao.id],
// //                   name          = it[MedicationDao.name],
// //                   date          = it[MedicationDao.date].toKvLocal(),
// //                   dosage        = it[MedicationDao.dosage],
// //                   system        = it[MedicationDao.system],
// //                   code          = it[MedicationDao.code],
// //                   status        = it[MedicationDao.status],
// //                   ipsModelId    = it[MedicationDao.ipsModelId]
// //                 )
// //               }

// //             val allergies = AllergyDao.select { AllergyDao.ipsModelId eq pk }
// //               .map {
// //                 Allergy(
// //                   id            = it[AllergyDao.id],
// //                   name          = it[AllergyDao.name],
// //                   criticality   = it[AllergyDao.criticality],
// //                   date          = it[AllergyDao.date].toKvLocal(),
// //                   system        = it[AllergyDao.system],
// //                   code          = it[AllergyDao.code],
// //                   ipsModelId    = it[AllergyDao.ipsModelId]
// //                 )
// //               }

// //             val conditions = ConditionDao.select { ConditionDao.ipsModelId eq pk }
// //               .map {
// //                 Condition(
// //                   id            = it[ConditionDao.id],
// //                   name          = it[ConditionDao.name],
// //                   date          = it[ConditionDao.date].toKvLocal(),
// //                   system        = it[ConditionDao.system],
// //                   code          = it[ConditionDao.code],
// //                   ipsModelId    = it[ConditionDao.ipsModelId]
// //                 )
// //               }

// //             val observations = ObservationDao.select { ObservationDao.ipsModelId eq pk }
// //               .map {
// //                 Observation(
// //                   id            = it[ObservationDao.id],
// //                   name          = it[ObservationDao.name],
// //                   date          = it[ObservationDao.date].toKvLocal(),
// //                   value         = it[ObservationDao.value],
// //                   system        = it[ObservationDao.system],
// //                   code          = it[ObservationDao.code],
// //                   valueCode     = it[ObservationDao.valueCode],
// //                   bodySite      = it[ObservationDao.bodySite],
// //                   status        = it[ObservationDao.status],
// //                   ipsModelId    = it[ObservationDao.ipsModelId]
// //                 )
// //               }

// //             val immunizations = ImmunizationDao.select { ImmunizationDao.ipsModelId eq pk }
// //               .map {
// //                 Immunization(
// //                   id            = it[ImmunizationDao.id],
// //                   name          = it[ImmunizationDao.name],
// //                   system        = it[ImmunizationDao.system],
// //                   date          = it[ImmunizationDao.date].toKvLocal(),
// //                   code          = it[ImmunizationDao.code],
// //                   status        = it[ImmunizationDao.status],
// //                   ipsModelId    = it[ImmunizationDao.ipsModelId]
// //                 )
// //               }

// //             // 3) assemble your shared‐Main IPSModel
// //             IPSModel(
// //               id                  = pk,
// //               packageUUID         = row[IPSModelDao.packageUUID],
// //               timeStamp           = row[IPSModelDao.timeStamp].toKvLocal(),
// //               patientName         = row[IPSModelDao.patientName],
// //               patientGiven        = row[IPSModelDao.patientGiven],
// //               patientDob          = row[IPSModelDao.patientDob].toKvLocal(),
// //               patientGender       = row[IPSModelDao.patientGender],
// //               patientNation       = row[IPSModelDao.patientNation],
// //               patientPractitioner = row[IPSModelDao.patientPractitioner],
// //               patientOrganization = row[IPSModelDao.patientOrganization],
// //               patientIdentifier   = row[IPSModelDao.patientIdentifier],
// //               medications         = medications,
// //               allergies           = allergies,
// //               conditions          = conditions,
// //               observations        = observations,
// //               immunizations       = immunizations
// //             )
// //         }
// //     }
// // }
