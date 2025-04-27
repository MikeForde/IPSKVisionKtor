package com.example

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

// parent table, maps to ipsAlt
object IPSModels : IntIdTable(name = "ipsAlt") {
    val packageUUID         = varchar("packageUUID", 255)
    val timeStamp           = datetime("timeStamp")
    val patientName         = varchar("patientName", 255)
    val patientGiven        = varchar("patientGiven", 255)
    val patientDob          = datetime("patientDob")
    val patientGender       = varchar("patientGender", 50).nullable()
    val patientNation       = varchar("patientNation", 255)
    val patientPractitioner = varchar("patientPractitioner", 255)
    val patientOrganization = varchar("patientOrganization", 255).nullable()
    val patientIdentifier   = varchar("patientIdentifier", 255).nullable()
}

object Medications : IntIdTable(name = "Medication") {
    val name     = varchar("name", 255)
    val date     = datetime("date")
    val dosage   = varchar("dosage", 255)
    val system   = varchar("system", 255)
    val code     = varchar("code", 255)
    val status   = varchar("status", 100)
    val ipsModel = reference("ipsModel_id", IPSModels, onDelete = ReferenceOption.CASCADE)
}

object Allergies : IntIdTable(name = "Allergy") {
    val name       = varchar("name", 255)
    val criticality= varchar("criticality", 100)
    val date       = datetime("date")
    val system     = varchar("system", 255)
    val code       = varchar("code", 255)
    val ipsModel   = reference("ipsModel_id", IPSModels, onDelete = ReferenceOption.CASCADE)
}

object Conditions : IntIdTable(name = "Condition") {
    val name     = varchar("name", 255)
    val date     = datetime("date")
    val system   = varchar("system", 255)
    val code     = varchar("code", 255)
    val ipsModel = reference("ipsModel_id", IPSModels, onDelete = ReferenceOption.CASCADE)
}

object Observations : IntIdTable(name = "Observation") {
    val name       = varchar("name", 255)
    val date       = datetime("date")
    val value      = varchar("value", 255)
    val system     = varchar("system", 255)
    val code       = varchar("code", 255)
    val valueCode  = varchar("valueCode", 100)
    val bodySite   = varchar("bodySite", 100)
    val status     = varchar("status", 100)
    val ipsModel   = reference("ipsModel_id", IPSModels, onDelete = ReferenceOption.CASCADE)
}

object Immunizations : IntIdTable(name = "Immunization") {
    val name     = varchar("name", 255)
    val system   = varchar("system", 255)
    val date     = datetime("date")
    val code     = varchar("code", 255)
    val status   = varchar("status", 100)
    val ipsModel = reference("ipsModel_id", IPSModels, onDelete = ReferenceOption.CASCADE)
}
