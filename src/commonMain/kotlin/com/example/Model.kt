@file:UseContextualSerialization(LocalDateTime::class)
package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import io.kvision.types.LocalDateTime

@Serializable
data class IPSModel(
    val id: Int? = null,
    val packageUUID: String,
    val timeStamp: LocalDateTime,
    val patientName: String,
    val patientGiven: String,
    val patientDob: LocalDateTime,
    val patientGender: String? = null,
    val patientNation: String,
    val patientPractitioner: String,
    val patientOrganization: String? = null,
    val patientIdentifier: String? = null,
    // embed the relations
    val medications: List<Medication> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val conditions: List<Condition> = emptyList(),
    val observations: List<Observation> = emptyList(),
    val immunizations: List<Immunization> = emptyList()
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
    val ipsModelId: Int? = null
)

@Serializable
data class Allergy(
    val id: Int? = null,
    val name: String,
    val criticality: String,
    val date: LocalDateTime,
    val system: String,
    val code: String,
    val ipsModelId: Int? = null
)

@Serializable
data class Condition(
    val id: Int? = null,
    val name: String,
    val date: LocalDateTime,
    val system: String,
    val code: String,
    val ipsModelId: Int? = null
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
    val ipsModelId: Int? = null
)

@Serializable
data class Immunization(
    val id: Int? = null,
    val name: String,
    val system: String,
    val date: LocalDateTime,
    val code: String,
    val status: String,
    val ipsModelId: Int? = null
)
