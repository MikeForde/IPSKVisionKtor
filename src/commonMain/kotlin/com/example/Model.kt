
package com.example

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
data class IPSModel(
    val id: Int? = null,                     // PK (null before insert)
    val packageUUID: String,                 // maps Sequelize’s STRING, non-null
    val timeStamp: kotlinx.datetime.LocalDateTime,            // Sequelize DATE → LocalDateTime
    val patientName: String,
    val patientGiven: String,
    val patientDob: kotlinx.datetime.LocalDateTime,
    val patientGender: String? = null,       // allowNull: true
    val patientNation: String,
    val patientPractitioner: String,
    val patientOrganization: String? = null, // allowNull: true
    val patientIdentifier: String? = null,   // allowNull: true

    // child collections (nullable until loaded)
    val medications: List<Medication>? = null,
    val allergies:   List<Allergy>?    = null,
    val conditions:  List<Condition>?  = null,
    val observations:List<Observation>? = null,
    val immunizations:List<Immunization>?= null
)

@Serializable
data class Medication(
    val id: Int? = null,
    val name: String,
    val date: kotlinx.datetime.LocalDateTime,
    val dosage: String,
    val system: String,
    val code: String,
    val status: String,
    val ipsModelId: Int? = null,           // FK back to IPSModel.id
)

@Serializable
data class Allergy(
    val id: Int? = null,
    val name: String,
    val criticality: String,
    val date: kotlinx.datetime.LocalDateTime,
    val system: String,
    val code: String,
    val ipsModelId: Int? = null,
)

@Serializable
data class Condition(
    val id: Int? = null,
    val name: String,
    val date: kotlinx.datetime.LocalDateTime,
    val system: String,
    val code: String,
    val ipsModelId: Int? = null,
)

@Serializable
data class Observation(
    val id: Int? = null,
    val name: String,
    val date: kotlinx.datetime.LocalDateTime,
    val value: String,
    val system: String,
    val code: String,
    val valueCode: String,
    val bodySite: String,
    val status: String,
    val ipsModelId: Int? = null,
)

@Serializable
data class Immunization(
    val id: Int? = null,
    val name: String,
    val system: String,
    val date: kotlinx.datetime.LocalDateTime,
    val code: String,
    val status: String,
    val ipsModelId: Int? = null,
)
