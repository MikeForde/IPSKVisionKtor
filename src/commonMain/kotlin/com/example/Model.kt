@file:UseContextualSerialization(LocalDateTime::class)

package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import io.kvision.types.LocalDateTime

@Serializable
data class Profile(
    val id: Int? = null,
    val name: String? = null,
    val username: String? = null,
    val password: String? = null,
    val password2: String? = null
)

@Serializable
data class Address(
    val id: Int? = 0,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val postalAddress: String? = null,
    val favourite: Boolean? = false,
    val createdAt: LocalDateTime? = null,
    val userId: Int? = null,
)

@Serializable
data class IPSModel(
    val id: Int? = null,                     // PK (null before insert)
    val packageUUID: String,                 // maps Sequelize’s STRING, non-null
    val timeStamp: LocalDateTime,            // Sequelize DATE → LocalDateTime
    val patientName: String,
    val patientGiven: String,
    val patientDob: LocalDateTime,
    val patientGender: String? = null,       // allowNull: true
    val patientNation: String,
    val patientPractitioner: String,
    val patientOrganization: String? = null, // allowNull: true
    val patientIdentifier: String? = null,   // allowNull: true
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,

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
    val date: LocalDateTime,
    val dosage: String,
    val system: String,
    val code: String,
    val status: String,
    val ipsModelId: Int? = null,           // FK back to IPSModel.id
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class Allergy(
    val id: Int? = null,
    val name: String,
    val criticality: String,
    val date: LocalDateTime,
    val system: String,
    val code: String,
    val ipsModelId: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class Condition(
    val id: Int? = null,
    val name: String,
    val date: LocalDateTime,
    val system: String,
    val code: String,
    val ipsModelId: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class Observation(
    val id: Int? = null,
    val name: String,
    val date: LocalDateTime,
    val value: String,
    val system: String,
    val code: String,
    val valueCode: String,
    val bodySite: String,
    val status: String,
    val ipsModelId: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class Immunization(
    val id: Int? = null,
    val name: String,
    val system: String,
    val date: LocalDateTime,
    val code: String,
    val status: String,
    val ipsModelId: Int? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
