package com.example

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.time.format.DateTimeFormatter

// IPS header table
object IPSModelDao : Table("ipsAlt") {
    val id                    = integer("id").autoIncrement().primaryKey()
    val packageUUID         = varchar("packageUUID",        255)
    val timeStamp           = datetime("timeStamp")
    val patientName         = varchar("patientName",        255)
    val patientGiven        = varchar("patientGiven",       255)
    val patientDob          = datetime("patientDob")
    val patientGender       = varchar("patientGender",      50).nullable()
    val patientNation       = varchar("patientNation",      255)
    val patientPractitioner = varchar("patientPractitioner",255)
    val patientOrganization = varchar("patientOrganization",255).nullable()
    val patientIdentifier   = varchar("patientIdentifier",  255).nullable()
    // val patientIdentifier2  = varchar("patientIdentifier2", 255).nullable()
}

// Child tables, each with FK back to IPSModel
object MedicationDao : Table("Medications") {
    val id          = integer("id").autoIncrement().primaryKey()
    val name        = varchar("name", 255)
    val date        = datetime("date")
    val dosage      = varchar("dosage", 255)
    val system      = varchar("system", 255)
    val code        = varchar("code", 255)
    val status      = varchar("status", 255)
    val ipsModelId  = reference(
        "IPSModelId",
        IPSModelDao.id,
        ReferenceOption.CASCADE,
        ReferenceOption.CASCADE
    )
}

object AllergyDao : Table("Allergies") {
    val id          = integer("id").autoIncrement().primaryKey()
    val name        = varchar("name", 255)
    val criticality = varchar("criticality", 255)
    val date        = datetime("date")
    val system      = varchar("system", 255)
    val code        = varchar("code", 255)
    val ipsModelId  = reference(
        "IPSModelId",
        IPSModelDao.id,
        ReferenceOption.CASCADE,
        ReferenceOption.CASCADE
    )
}

object ConditionDao : Table("Conditions") {
    val id          = integer("id").autoIncrement().primaryKey()
    val name        = varchar("name", 255)
    val date        = datetime("date")
    val system      = varchar("system", 255)
    val code        = varchar("code", 255)
    val ipsModelId  = reference(
        "IPSModelId",
        IPSModelDao.id,
        ReferenceOption.CASCADE,
        ReferenceOption.CASCADE
    )
}

object ObservationDao : Table("Observations") {
    val id          = integer("id").autoIncrement().primaryKey()
    val name        = varchar("name", 255)
    val date        = datetime("date")
    val value       = varchar("value", 255)
    val system      = varchar("system", 255)
    val code        = varchar("code", 255)
    val valueCode   = varchar("valueCode", 255)
    val bodySite    = varchar("bodySite", 255)
    val status      = varchar("status", 255)
    val ipsModelId  = reference(
        "IPSModelId",
        IPSModelDao.id,
        ReferenceOption.CASCADE,
        ReferenceOption.CASCADE
    )
}

object ImmunizationDao : Table("Immunizations") {
    val id          = integer("id").autoIncrement().primaryKey()
    val name        = varchar("name", 255)
    val system      = varchar("system", 255)
    val date        = datetime("date")
    val code        = varchar("code", 255)
    val status      = varchar("status", 255)
    val ipsModelId  = reference(
        "IPSModelId",
        IPSModelDao.id,
        ReferenceOption.CASCADE,
        ReferenceOption.CASCADE
    )
}

