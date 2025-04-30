package com.example

import kotlinx.serialization.Serializable

@Serializable
data class IPSModel(
    val id: Int? = null,                     // PK (null before insert)
    val packageUUID: String,                 // maps Sequelize’s STRING, non-null
    val timeStamp: String,            
    val patientName: String,
    val patientGiven: String,
    val patientDob: String,
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
    val id: Int,
    val name: String? = null,
    val date: String? = null,
    val dosage: String? = null,
    val system: String? = null,
    val code: String? = null,
    val status: String? = null,
    val ipsModelId: Int? = null,           // FK back to IPSModel.id
)

@Serializable
data class Allergy(
    val id: Int,
    val name: String? = null,
    val criticality: String? = null,
    val date: String? = null,
    val system: String? = null,
    val code: String? = null,
    val ipsModelId: Int? = null,
)

@Serializable
data class Condition(
    val id: Int,
    val name: String? = null,
    val date: String? = null,
    val system: String? = null,
    val code: String? = null,
    val ipsModelId: Int? = null,
)

@Serializable
data class Observation(
    val id: Int,
    val name: String? = null,
    val date: String? = null,
    val value: String? = null,
    val system: String? = null,
    val code: String? = null,
    val valueCode: String? = null,
    val bodySite: String? = null,
    val status: String? = null,
    val ipsModelId: Int? = null,
)

@Serializable
data class Immunization(
    val id: Int,
    val name: String? = null,
    val system: String? = null,
    val date: String? = null,
    val code: String? = null,
    val status: String? = null,
    val ipsModelId: Int? = null,
)
