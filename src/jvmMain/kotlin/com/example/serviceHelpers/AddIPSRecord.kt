package com.example.serviceHelpers

import com.example.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

/**
 * Helper to persist an IPSModel and its child collections into the database.
 * Returns the inserted model with its generated 'id'.
 */
fun addIPSRecord(model: IPSModel): IPSModel {
    return transaction {
        // Insert header and capture the generated primary key
        val insertStmt = IPSModelDao.insert { row ->
            row[IPSModelDao.packageUUID] = model.packageUUID
            row[IPSModelDao.timeStamp] = parseTimestamp(model.timeStamp)
            row[IPSModelDao.patientName] = model.patientName
            row[IPSModelDao.patientGiven] = model.patientGiven
            row[IPSModelDao.patientDob] = parseDate(model.patientDob)
            row[IPSModelDao.patientGender] = model.patientGender
            row[IPSModelDao.patientNation] = model.patientNation
            row[IPSModelDao.patientPractitioner] = model.patientPractitioner
            row[IPSModelDao.patientOrganization] = model.patientOrganization
            row[IPSModelDao.patientIdentifier] = model.patientIdentifier
            row[IPSModelDao.patientIdentifier2] = model.patientIdentifier2
        }
        val generatedId = insertStmt[IPSModelDao.id]

        // Insert medications
        model.medications?.forEach { med ->
            MedicationDao.insert { m ->
                m[MedicationDao.name] = med.name ?: ""
                m[MedicationDao.date] = parseTimestamp(med.date ?: model.timeStamp)
                m[MedicationDao.dosage] = med.dosage ?: ""
                m[MedicationDao.system] = med.system ?: ""
                m[MedicationDao.code] = med.code ?: ""
                m[MedicationDao.status] = med.status ?: ""
                m[MedicationDao.ipsModelId] = generatedId
            }
        }

        // Insert allergies
        model.allergies?.forEach { allg ->
            AllergyDao.insert { a ->
                a[AllergyDao.name] = allg.name ?: ""
                a[AllergyDao.criticality] = allg.criticality ?: ""
                a[AllergyDao.date] = parseTimestamp(allg.date ?: model.timeStamp)
                a[AllergyDao.system] = allg.system ?: ""
                a[AllergyDao.code] = allg.code ?: ""
                a[AllergyDao.ipsModelId] = generatedId
            }
        }

        // Insert conditions
        model.conditions?.forEach { cond ->
            ConditionDao.insert { c ->
                c[ConditionDao.name] = cond.name ?: ""
                c[ConditionDao.date] = parseTimestamp(cond.date ?: model.timeStamp)
                c[ConditionDao.system] = cond.system ?: ""
                c[ConditionDao.code] = cond.code ?: ""
                c[ConditionDao.ipsModelId] = generatedId
            }
        }

        // Insert observations
        model.observations?.forEach { obs ->
            ObservationDao.insert { o ->
                o[ObservationDao.name] = obs.name ?: ""
                o[ObservationDao.date] = parseTimestamp(obs.date ?: model.timeStamp)
                o[ObservationDao.value] = obs.value ?: ""
                o[ObservationDao.system] = obs.system ?: ""
                o[ObservationDao.code] = obs.code ?: ""
                o[ObservationDao.valueCode] = obs.valueCode ?: ""
                o[ObservationDao.bodySite] = obs.bodySite ?: ""
                o[ObservationDao.status] = obs.status ?: ""
                o[ObservationDao.ipsModelId] = generatedId
            }
        }

        // Insert immunizations
        model.immunizations?.forEach { imm ->
            ImmunizationDao.insert { i ->
                i[ImmunizationDao.name] = imm.name ?: ""
                i[ImmunizationDao.system] = imm.system ?: ""
                i[ImmunizationDao.date] = parseTimestamp(imm.date ?: model.timeStamp)
                i[ImmunizationDao.code] = imm.code ?: ""
                i[ImmunizationDao.status] = imm.status ?: ""
                i[ImmunizationDao.ipsModelId] = generatedId
            }
        }

        // Return the model with its new database ID
        model.copy(id = generatedId)
    }
}

/**
 * Parse an ISO-8601 timestamp or date string into a Joda DateTime for MySQL DATETIME.
 */
private fun parseTimestamp(ts: String): DateTime = DateTime.parse(ts)
private fun parseDate(ds: String): DateTime = DateTime.parse(ds)
