package com.example.serviceHelpers

import com.example.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select

/**
 * Maps one Exposed ResultRow into a fully-loaded IPSModel, including medications, allergies,
 * conditions, observations and immunizations.
 */
fun toFullIpsModel(row: ResultRow): IPSModel {
  val pk = row[IPSModelDao.id]

  val meds =
      MedicationDao.select { MedicationDao.ipsModelId eq pk }
          .map { m ->
            Medication(
                id = m[MedicationDao.id],
                name = m[MedicationDao.name],
                date = m[MedicationDao.date].toString(),
                dosage = m[MedicationDao.dosage],
                system = m[MedicationDao.system],
                code = m[MedicationDao.code],
                status = m[MedicationDao.status],
                ipsModelId = m[MedicationDao.ipsModelId])
          }

  val allgs =
      AllergyDao.select { AllergyDao.ipsModelId eq pk }
          .map { a ->
            Allergy(
                id = a[AllergyDao.id],
                name = a[AllergyDao.name],
                date = a[AllergyDao.date].toString(),
                criticality = a[AllergyDao.criticality],
                system = a[AllergyDao.system],
                code = a[AllergyDao.code],
                ipsModelId = a[AllergyDao.ipsModelId])
          }

  val conds =
      ConditionDao.select { ConditionDao.ipsModelId eq pk }
          .map { c ->
            Condition(
                id = c[ConditionDao.id],
                name = c[ConditionDao.name],
                date = c[ConditionDao.date].toString(),
                system = c[ConditionDao.system],
                code = c[ConditionDao.code],
                ipsModelId = c[ConditionDao.ipsModelId])
          }

  val obs =
      ObservationDao.select { ObservationDao.ipsModelId eq pk }
          .map { o ->
            Observation(
                id = o[ObservationDao.id],
                name = o[ObservationDao.name],
                date = o[ObservationDao.date].toString(),
                value = o[ObservationDao.value],
                system = o[ObservationDao.system],
                code = o[ObservationDao.code],
                valueCode = o[ObservationDao.valueCode],
                bodySite = o[ObservationDao.bodySite],
                status = o[ObservationDao.status],
                ipsModelId = o[ObservationDao.ipsModelId])
          }

  val imms =
      ImmunizationDao.select { ImmunizationDao.ipsModelId eq pk }
          .map { i ->
            Immunization(
                id = i[ImmunizationDao.id],
                name = i[ImmunizationDao.name],
                system = i[ImmunizationDao.system],
                date = i[ImmunizationDao.date].toString(),
                code = i[ImmunizationDao.code],
                status = i[ImmunizationDao.status],
                ipsModelId = i[ImmunizationDao.ipsModelId])
          }

  return IPSModel(
      id = pk,
      packageUUID = row[IPSModelDao.packageUUID],
      timeStamp = row[IPSModelDao.timeStamp].toString(),
      patientName = row[IPSModelDao.patientName],
      patientGiven = row[IPSModelDao.patientGiven],
      patientDob = row[IPSModelDao.patientDob].toString(),
      patientGender = row[IPSModelDao.patientGender],
      patientNation = row[IPSModelDao.patientNation],
      patientPractitioner = row[IPSModelDao.patientPractitioner],
      patientOrganization = row[IPSModelDao.patientOrganization],
      patientIdentifier = row[IPSModelDao.patientIdentifier],
      medications = meds,
      allergies = allgs,
      conditions = conds,
      observations = obs,
      immunizations = imms)
}
