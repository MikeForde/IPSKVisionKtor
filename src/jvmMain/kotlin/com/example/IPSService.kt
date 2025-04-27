// package com.example

// import com.example.Db.dbQuery
// import com.example.IIPSService

// import io.ktor.server.application.ApplicationCall
// import io.ktor.server.sessions.get
// import io.ktor.server.sessions.sessions
// import org.jetbrains.exposed.sql.*
// import org.jetbrains.exposed.sql.transactions.transaction
// import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

// // Helper to ensure the user is logged in
// suspend fun <T> ApplicationCall.withProfile(block: suspend (Profile) -> T): T {
//     val profile = this.sessions.get<Profile>()
//     return profile ?: throw IllegalStateException("Not authenticated").let { /* unreachable */ }
// }

// class IPSService(private val call: ApplicationCall) : IIPSService {

//     override suspend fun getAll(): List<IPSModel> = call.withProfile { _ ->
//         dbQuery {
//             // fetch all parents
//             IPSModels.selectAll().map { parentRow ->
//                 toFullIPSModel(parentRow)
//             }
//         }
//     }

//     override suspend fun getById(id: Int): IPSModel? = call.withProfile { _ ->
//         dbQuery {
//             IPSModels.select { IPSModels.id eq id }
//                 .mapNotNull { toFullIPSModel(it) }
//                 .singleOrNull()
//         }
//     }

//     override suspend fun create(model: IPSModel): IPSModel = call.withProfile { _ ->
//         dbQuery {
//             // insert parent
//             val parentId = IPSModels.insertAndGetId { row ->
//                 row[packageUUID]         = model.packageUUID
//                 row[timeStamp]           = model.timeStamp
//                 row[patientName]         = model.patientName
//                 row[patientGiven]        = model.patientGiven
//                 row[patientDob]          = model.patientDob
//                 row[patientGender]       = model.patientGender
//                 row[patientNation]       = model.patientNation
//                 row[patientPractitioner] = model.patientPractitioner
//                 row[patientOrganization] = model.patientOrganization
//                 row[patientIdentifier]   = model.patientIdentifier
//             }.value

//             // insert children in bulk
//             model.medications.forEach { m ->
//                 Medications.insert {
//                     it[name]     = m.name
//                     it[date]     = m.date
//                     it[dosage]   = m.dosage
//                     it[system]   = m.system
//                     it[code]     = m.code
//                     it[status]   = m.status
//                     it[ipsModel] = parentId
//                 }
//             }
//             model.allergies.forEach { a ->
//                 Allergies.insert {
//                     it[name]       = a.name
//                     it[criticality]= a.criticality
//                     it[date]       = a.date
//                     it[system]     = a.system
//                     it[code]       = a.code
//                     it[ipsModel]   = parentId
//                 }
//             }
//             // ... same pattern for conditions, observations, immunizations ...

//             // return freshly loaded
//             toFullIPSModel(IPSModels.select { IPSModels.id eq parentId }.single())
//         }
//     }

//     override suspend fun update(id: Int, model: IPSModel): IPSModel? = call.withProfile { _ ->
//         dbQuery {
//             // update parent
//             IPSModels.update({ IPSModels.id eq id }) { row ->
//                 row[packageUUID]         = model.packageUUID
//                 row[timeStamp]           = model.timeStamp
//                 row[patientName]         = model.patientName
//                 row[patientGiven]        = model.patientGiven
//                 row[patientDob]          = model.patientDob
//                 row[patientGender]       = model.patientGender
//                 row[patientNation]       = model.patientNation
//                 row[patientPractitioner] = model.patientPractitioner
//                 row[patientOrganization] = model.patientOrganization
//                 row[patientIdentifier]   = model.patientIdentifier
//             }
//             // for simplicity, delete & re-insert children
//             Medications.deleteWhere { Medications.ipsModel eq id }
//             model.medications.forEach { m ->
//                 Medications.insert {
//                     it[name]     = m.name
//                     it[date]     = m.date
//                     it[dosage]   = m.dosage
//                     it[system]   = m.system
//                     it[code]     = m.code
//                     it[status]   = m.status
//                     it[ipsModel] = id
//                 }
//             }
//             // ... do same delete+insert for allergies, conditions, observations, immunizations ...

//             // return updated
//             IPSModels.select { IPSModels.id eq id }
//                 .mapNotNull { toFullIPSModel(it) }
//                 .singleOrNull()
//         }
//     }

//     override suspend fun delete(id: Int): Boolean = call.withProfile { _ ->
//         dbQuery {
//             // cascades will remove children
//             IPSModels.deleteWhere { IPSModels.id eq id } > 0
//         }
//     }

//     /** Pulls a parent row + all its children into one DTO */
//     private fun toFullIPSModel(row: ResultRow): IPSModel {
//         val parentId = row[IPSModels.id].value
//         return IPSModel(
//             id                  = parentId,
//             packageUUID         = row[IPSModels.packageUUID],
//             timeStamp           = row[IPSModels.timeStamp],
//             patientName         = row[IPSModels.patientName],
//             patientGiven        = row[IPSModels.patientGiven],
//             patientDob          = row[IPSModels.patientDob],
//             patientGender       = row[IPSModels.patientGender],
//             patientNation       = row[IPSModels.patientNation],
//             patientPractitioner = row[IPSModels.patientPractitioner],
//             patientOrganization = row[IPSModels.patientOrganization],
//             patientIdentifier   = row[IPSModels.patientIdentifier],
//             medications    = Medications.select { Medications.ipsModel eq parentId }
//                                           .map { toMedicationDTO(it) },
//             allergies      = Allergies.select { Allergies.ipsModel eq parentId }
//                                           .map { toAllergyDTO(it) },
//             conditions     = Conditions.select { Conditions.ipsModel eq parentId }
//                                           .map { toConditionDTO(it) },
//             observations   = Observations.select { Observations.ipsModel eq parentId }
//                                           .map { toObservationDTO(it) },
//             immunizations  = Immunizations.select { Immunizations.ipsModel eq parentId }
//                                           .map { toImmunizationDTO(it) }
//         )
//     }

//     // row → DTO mappers (similar to what we sketched previously)
//     private fun toMedicationDTO(r: ResultRow) = Medication(
//         id          = r[Medications.id].value,
//         name        = r[Medications.name],
//         date        = r[Medications.date],
//         dosage      = r[Medications.dosage],
//         system      = r[Medications.system],
//         code        = r[Medications.code],
//         status      = r[Medications.status],
//         ipsModelId  = r[Medications.ipsModel].value
//     )
//     private fun toAllergyDTO(r: ResultRow) = Allergy(
//         id          = r[Allergies.id].value,
//         name        = r[Allergies.name],
//         criticality = r[Allergies.criticality],
//         date        = r[Allergies.date],
//         system      = r[Allergies.system],
//         code        = r[Allergies.code],
//         ipsModelId  = r[Allergies.ipsModel].value
//     )
//     private fun toConditionDTO(r: ResultRow) = Condition(
//         id          = r[Conditions.id].value,
//         name        = r[Conditions.name],
//         date        = r[Conditions.date],
//         system      = r[Conditions.system],
//         code        = r[Conditions.code],
//         ipsModelId  = r[Conditions.ipsModel].value
//     )
//     private fun toObservationDTO(r: ResultRow) = Observation(
//         id          = r[Observations.id].value,
//         name        = r[Observations.name],
//         date        = r[Observations.date],
//         value       = r[Observations.value],
//         system      = r[Observations.system],
//         code        = r[Observations.code],
//         valueCode   = r[Observations.valueCode],
//         bodySite    = r[Observations.bodySite],
//         status      = r[Observations.status],
//         ipsModelId  = r[Observations.ipsModel].value
//     )
//     private fun toImmunizationDTO(r: ResultRow) = Immunization(
//         id          = r[Immunizations.id].value,
//         name        = r[Immunizations.name],
//         system      = r[Immunizations.system],
//         date        = r[Immunizations.date],
//         code        = r[Immunizations.code],
//         status      = r[Immunizations.status],
//         ipsModelId  = r[Immunizations.ipsModel].value
//     )
// }
