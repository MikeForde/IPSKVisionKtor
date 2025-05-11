package com.example.serviceHelpers

import com.example.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

/**
 * Helper to merge an incoming IPSModel into an existing database record. Updates header and merges
 * child records (medications, allergies, conditions, observations, immunizations).
 */
fun mergeIPSRecord(model: IPSModel): IPSModel {
  return transaction {
    // Find existing header record by packageUUID
    val existing =
        IPSModelDao.select { IPSModelDao.packageUUID eq model.packageUUID }.singleOrNull()
            ?: throw IllegalArgumentException(
                "No existing IPSModel with packageUUID ${model.packageUUID}")
    val id = existing[IPSModelDao.id]

    // Update header fields
    IPSModelDao.update({ IPSModelDao.id eq id }) { row ->
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

    // Merge medications
    model.medications?.forEach { med ->
      val medDate = parseTimestamp(med.date ?: model.timeStamp)
      val medName = med.name ?: ""
      val updatedCount =
          MedicationDao.update({
            (MedicationDao.ipsModelId eq id) and
                (MedicationDao.name eq medName) and
                (MedicationDao.date eq medDate)
          }) { row ->
            med.dosage?.let { row[MedicationDao.dosage] = it }
            med.system?.let { row[MedicationDao.system] = it }
            med.code?.let { row[MedicationDao.code] = it }
            med.status?.let { row[MedicationDao.status] = it }
          }
      if (updatedCount == 0) {
        MedicationDao.insert { m ->
          m[MedicationDao.name] = med.name ?: ""
          m[MedicationDao.date] = medDate
          m[MedicationDao.dosage] = med.dosage ?: ""
          m[MedicationDao.system] = med.system ?: ""
          m[MedicationDao.code] = med.code ?: ""
          m[MedicationDao.status] = med.status ?: ""
          m[MedicationDao.ipsModelId] = id
        }
      }
    }

    // Merge allergies
    model.allergies?.forEach { allg ->
      val allgDate = parseTimestamp(allg.date ?: model.timeStamp)
      val allgName = allg.name ?: ""
      val updatedCount =
          AllergyDao.update({
            (AllergyDao.ipsModelId eq id) and
                (AllergyDao.name eq allgName) and
                (AllergyDao.date eq allgDate)
          }) { row ->
            allg.criticality?.let { row[AllergyDao.criticality] = it }
            allg.system?.let { row[AllergyDao.system] = it }
            allg.code?.let { row[AllergyDao.code] = it }
          }
      if (updatedCount == 0) {
        AllergyDao.insert { a ->
          a[AllergyDao.name] = allg.name ?: ""
          a[AllergyDao.criticality] = allg.criticality ?: ""
          a[AllergyDao.date] = allgDate
          a[AllergyDao.system] = allg.system ?: ""
          a[AllergyDao.code] = allg.code ?: ""
          a[AllergyDao.ipsModelId] = id
        }
      }
    }

    // Merge conditions
    model.conditions?.forEach { cond ->
      val condDate = parseTimestamp(cond.date ?: model.timeStamp)
      val condName = cond.name ?: ""
      val updatedCount =
          ConditionDao.update({
            (ConditionDao.ipsModelId eq id) and
                (ConditionDao.name eq condName) and
                (ConditionDao.date eq condDate)
          }) { row ->
            cond.system?.let { row[ConditionDao.system] = it }
            cond.code?.let { row[ConditionDao.code] = it }
          }
      if (updatedCount == 0) {
        ConditionDao.insert { c ->
          c[ConditionDao.name] = cond.name ?: ""
          c[ConditionDao.date] = condDate
          c[ConditionDao.system] = cond.system ?: ""
          c[ConditionDao.code] = cond.code ?: ""
          c[ConditionDao.ipsModelId] = id
        }
      }
    }

    // Merge observations
    model.observations?.forEach { obs ->
      val obsDate = parseTimestamp(obs.date ?: model.timeStamp)
      val obsName = obs.name ?: ""
      val updatedCount =
          ObservationDao.update({
            (ObservationDao.ipsModelId eq id) and
                (ObservationDao.name eq obsName) and
                (ObservationDao.date eq obsDate)
          }) { row ->
            obs.value?.let { row[ObservationDao.value] = it }
            obs.system?.let { row[ObservationDao.system] = it }
            obs.code?.let { row[ObservationDao.code] = it }
            obs.valueCode?.let { row[ObservationDao.valueCode] = it }
            obs.bodySite?.let { row[ObservationDao.bodySite] = it }
            obs.status?.let { row[ObservationDao.status] = it }
          }
      if (updatedCount == 0) {
        ObservationDao.insert { o ->
          o[ObservationDao.name] = obs.name ?: ""
          o[ObservationDao.date] = obsDate
          o[ObservationDao.value] = obs.value ?: ""
          o[ObservationDao.system] = obs.system ?: ""
          o[ObservationDao.code] = obs.code ?: ""
          o[ObservationDao.valueCode] = obs.valueCode ?: ""
          o[ObservationDao.bodySite] = obs.bodySite ?: ""
          o[ObservationDao.status] = obs.status ?: ""
          o[ObservationDao.ipsModelId] = id
        }
      }
    }

    // Merge immunizations
    model.immunizations?.forEach { imm ->
      val immDate = parseTimestamp(imm.date ?: model.timeStamp)
      val immName = imm.name ?: ""
      val updatedCount =
          ImmunizationDao.update({
            (ImmunizationDao.ipsModelId eq id) and
                (ImmunizationDao.name eq immName) and
                (ImmunizationDao.date eq immDate)
          }) { row ->
            imm.system?.let { row[ImmunizationDao.system] = it }
            imm.code?.let { row[ImmunizationDao.code] = it }
            imm.status?.let { row[ImmunizationDao.status] = it }
          }
      if (updatedCount == 0) {
        ImmunizationDao.insert { i ->
          i[ImmunizationDao.name] = imm.name ?: ""
          i[ImmunizationDao.system] = imm.system ?: ""
          i[ImmunizationDao.date] = immDate
          i[ImmunizationDao.code] = imm.code ?: ""
          i[ImmunizationDao.status] = imm.status ?: ""
          i[ImmunizationDao.ipsModelId] = id
        }
      }
    }

    // Return merged model with existing ID
    model.copy(id = id)
  }
}

// Date parsing utilities (copied from addIPSRecord.kt)
private fun parseTimestamp(ts: String): DateTime = DateTime.parse(ts)

private fun parseDate(ds: String): DateTime = DateTime.parse(ds)
