package com.example.serviceHelpers

import com.example.*
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

fun convertIPSBundleToSchema(ipsBundle: JsonObject): IPSModel {
  val entry = ipsBundle["entry"]?.jsonArray ?: JsonArray(emptyList())
  var packageUUID = ipsBundle["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
  var timeStamp =
      ipsBundle["timestamp"]?.jsonPrimitive?.contentOrNull ?: Clock.System.now().toString()

  var patientName = "Unknown"
  var patientGiven = "Unknown"
  var patientDob = "1900-01-01"
  var patientGender = "Unknown"
  var patientNation = "Unknown"
  var patientIdentifier: String? = null
  var patientIdentifier2: String? = null
  var patientPractitioner = "Unknown"
  var patientOrganization: String? = null

  // val medications = mutableListOf<Medication>()
  val medicationMap = mutableMapOf<String, JsonObject>()
  val allergies = mutableListOf<Allergy>()
  val conditions = mutableListOf<Condition>()
  val observations = mutableListOf<Observation>()
  val immunizations = mutableListOf<Immunization>()
  val tempMedications = mutableListOf<TempMedication>()
  val medicationResourceMap = mutableMapOf<String, JsonObject>()

  entry.forEach { item ->
    val resource = item.jsonObject["resource"]?.jsonObject ?: return@forEach
    when (resource["resourceType"]?.jsonPrimitive?.content?.lowercase()) {

      // Patient
      "patient" -> {
        val nameObj = resource["name"]?.jsonArray?.firstOrNull()?.jsonObject
        patientName = nameObj?.get("family")?.jsonPrimitive?.contentOrNull ?: "Unknown"
        patientGiven =
            nameObj?.get("given")?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                ?: "Unknown"
        if (patientGiven == "") patientGiven = "Unknown"
        patientDob = resource["birthDate"]?.jsonPrimitive?.contentOrNull ?: "1900-01-01"
        patientGender = resource["gender"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        patientNation =
            resource["address"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("country")
                ?.jsonPrimitive
                ?.contentOrNull ?: "Unknown"
        val ids = resource["identifier"]?.jsonArray ?: JsonArray(emptyList())
        patientIdentifier = ids.getOrNull(0)?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
        patientIdentifier2 =
            ids.getOrNull(1)?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
      }

      // Practitioner
      "practitioner" -> {
        val nameObj = resource["name"]?.jsonArray?.firstOrNull()?.jsonObject
        patientPractitioner =
            nameObj?.get("text")?.jsonPrimitive?.contentOrNull
                ?: listOfNotNull(
                        nameObj?.get("family")?.jsonPrimitive?.contentOrNull,
                        nameObj
                            ?.get("given")
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonPrimitive
                            ?.contentOrNull)
                    .joinToString(", ")
                    .ifEmpty { "Unknown" }
      }

      // Organization
      "organization" -> {
        patientOrganization = resource["name"]?.jsonPrimitive?.contentOrNull
      }

      // MedicationStatement
      "medicationstatement" -> {
        val effectiveDate =
            resource["effectivePeriod"]?.jsonObject?.get("start")?.jsonPrimitive?.contentOrNull
        val dosageObj = resource["dosage"]?.jsonArray?.firstOrNull()?.jsonObject
        val dosage =
            dosageObj?.get("text")?.jsonPrimitive?.contentOrNull
                ?: dosageObj?.get("doseAndRate")?.jsonArray?.firstOrNull()?.jsonObject?.let {
                    doseRate ->
                  val dose = doseRate["doseQuantity"]?.jsonObject
                  val value = dose?.get("value")?.jsonPrimitive?.contentOrNull
                  val unit = dose?.get("unit")?.jsonPrimitive?.contentOrNull
                  val frequency =
                      dosageObj["timing"]
                          ?.jsonObject
                          ?.get("repeat")
                          ?.jsonObject
                          ?.get("frequency")
                          ?.jsonPrimitive
                          ?.contentOrNull
                  val periodUnit =
                      dosageObj["timing"]
                          ?.jsonObject
                          ?.get("repeat")
                          ?.jsonObject
                          ?.get("periodUnit")
                          ?.jsonPrimitive
                          ?.contentOrNull
                  "$value $unit${if (frequency != null && periodUnit != null) " $frequency$periodUnit" else ""}"
                }
                ?: "Unknown"

        val medRef =
            resource["medicationReference"]
                ?.jsonObject
                ?.get("reference")
                ?.jsonPrimitive
                ?.contentOrNull
        val display =
            resource["medicationReference"]
                ?.jsonObject
                ?.get("display")
                ?.jsonPrimitive
                ?.contentOrNull

        tempMedications.add(
            TempMedication(
                name = display,
                date = effectiveDate ?: Clock.System.now().toString(),
                dosage = dosage,
                status = "active",
                medRef = medRef))
      }

      // MedicationRequest
      "medicationrequest" -> {
        val authoredOn =
            resource["authoredOn"]?.jsonPrimitive?.contentOrNull ?: Clock.System.now().toString()
        val dosageInstruction = resource["dosageInstruction"]?.jsonArray?.firstOrNull()?.jsonObject
        val dosage =
            dosageInstruction?.get("text")?.jsonPrimitive?.contentOrNull
                ?: dosageInstruction
                    ?.get("timing")
                    ?.jsonObject
                    ?.get("code")
                    ?.jsonObject
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.contentOrNull
                ?: "Unknown"

        var name: String? = "Unknown"
        var system: String? = null
        var code: String? = null
        var medRef: String? = null

        when {
          resource["medicationReference"] != null -> {
            val medRefObj = resource["medicationReference"]!!.jsonObject
            name = medRefObj["display"]?.jsonPrimitive?.contentOrNull
            medRef = medRefObj["reference"]?.jsonPrimitive?.contentOrNull
          }

          resource["medicationCodeableConcept"] != null -> {
            val concept = resource["medicationCodeableConcept"]!!.jsonObject
            name = concept["text"]?.jsonPrimitive?.contentOrNull
            val coding = concept["coding"]?.jsonArray?.firstOrNull()?.jsonObject
            system = coding?.get("system")?.jsonPrimitive?.contentOrNull
            code = coding?.get("code")?.jsonPrimitive?.contentOrNull
          }

          resource["contained"] != null -> {
            name =
                resource["contained"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("code")
                    ?.jsonObject
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.contentOrNull ?: "Unknown"
          }
        }

        tempMedications.add(
            TempMedication(
                name = name,
                date = authoredOn,
                dosage = dosage,
                system = system,
                code = code,
                status = "active",
                medRef = medRef))
      }

      // MedicationAdministration
      "medicationadministration" -> {
        val date =
            resource["effectivePeriod"]?.jsonObject?.get("start")?.jsonPrimitive?.contentOrNull
                ?: Clock.System.now().toString()

        var name: String? = null
        var code: String? = null
        var system: String? = null
        var medRef: String? = null
        var dosage = "Unknown"

        when {
          resource["medicationReference"] != null -> {
            val refObj = resource["medicationReference"]!!.jsonObject
            name = refObj["display"]?.jsonPrimitive?.contentOrNull
            medRef = refObj["reference"]?.jsonPrimitive?.contentOrNull
          }

          resource["medicationCodeableConcept"] != null -> {
            val medCodeable = resource["medicationCodeableConcept"]!!.jsonObject
            val coding = medCodeable["coding"]?.jsonArray?.firstOrNull()?.jsonObject
            name =
                coding?.get("display")?.jsonPrimitive?.contentOrNull
                    ?: medCodeable["text"]?.jsonPrimitive?.contentOrNull
            system = coding?.get("system")?.jsonPrimitive?.contentOrNull
            code = coding?.get("code")?.jsonPrimitive?.contentOrNull
          }
        }

        val dosageObj = resource["dosage"]?.jsonObject
        dosage =
            dosageObj?.get("text")?.jsonPrimitive?.contentOrNull
                ?: dosageObj?.get("dose")?.jsonObject?.let {
                  val value = it["value"]?.jsonPrimitive?.contentOrNull
                  val unit = it["unit"]?.jsonPrimitive?.contentOrNull
                  "$value $unit"
                }
                ?: "Unknown"

        tempMedications.add(
            TempMedication(
                name = name,
                date = date,
                dosage = dosage,
                system = system,
                code = code,
                status = "active",
                medRef = medRef))
      }

      // Medication resources (used for resolution)
      "medication" -> {
        val id = resource["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
        medicationResourceMap["$id"] = resource
      }

      // AllergyIntolerance
      "allergyintolerance" -> {
        val code = resource["code"]?.jsonObject
        val coding = code?.get("coding")?.jsonArray?.firstOrNull()?.jsonObject
        allergies.add(
            Allergy(
                id = 0,
                name =
                    coding?.get("display")?.jsonPrimitive?.contentOrNull
                        ?: code?.get("text")?.jsonPrimitive?.contentOrNull,
                criticality = resource["criticality"]?.jsonPrimitive?.contentOrNull,
                date =
                    resource["onsetDateTime"]?.jsonPrimitive?.contentOrNull
                        ?: Clock.System.now().toString(),
                system = coding?.get("system")?.jsonPrimitive?.contentOrNull,
                code = coding?.get("code")?.jsonPrimitive?.contentOrNull))
      }

      // Condition
      "condition" -> {
        val code = resource["code"]?.jsonObject
        val coding = code?.get("coding")?.jsonArray?.firstOrNull()?.jsonObject
        conditions.add(
            Condition(
                id = 0,
                name =
                    coding?.get("display")?.jsonPrimitive?.contentOrNull
                        ?: code?.get("text")?.jsonPrimitive?.contentOrNull,
                date =
                    resource["onsetDateTime"]?.jsonPrimitive?.contentOrNull
                        ?: Clock.System.now().toString(),
                system = coding?.get("system")?.jsonPrimitive?.contentOrNull,
                code = coding?.get("code")?.jsonPrimitive?.contentOrNull))
      }

      // Observation
      "observation" -> {
        val codeObj = resource["code"]?.jsonObject
        val coding = codeObj?.get("coding")?.jsonArray?.firstOrNull()?.jsonObject
        val name =
            coding?.get("display")?.jsonPrimitive?.contentOrNull
                ?: codeObj?.get("text")?.jsonPrimitive?.contentOrNull
        val date =
            resource["effectiveDateTime"]?.jsonPrimitive?.contentOrNull
                ?: resource["issued"]?.jsonPrimitive?.contentOrNull
                ?: Clock.System.now().toString()
        val system = coding?.get("system")?.jsonPrimitive?.contentOrNull
        val code = coding?.get("code")?.jsonPrimitive?.contentOrNull

        var value: String? = null
        val components = resource["component"]?.jsonArray
        if (components != null && components.size == 2) {
          val v1 =
              components[0]
                  .jsonObject["valueQuantity"]
                  ?.jsonObject
                  ?.get("value")
                  ?.jsonPrimitive
                  ?.contentOrNull
          val v2 =
              components[1]
                  .jsonObject["valueQuantity"]
                  ?.jsonObject
                  ?.get("value")
                  ?.jsonPrimitive
                  ?.contentOrNull
          val unit =
              components[0]
                  .jsonObject["valueQuantity"]
                  ?.jsonObject
                  ?.get("unit")
                  ?.jsonPrimitive
                  ?.contentOrNull
          if (v1 != null && v2 != null) value = "$v1-$v2 $unit"
        } else if (resource["valueQuantity"] != null) {
          val q = resource["valueQuantity"]!!.jsonObject
          val valQ = q["value"]?.jsonPrimitive?.contentOrNull
          val unit = q["unit"]?.jsonPrimitive?.contentOrNull ?: ""
          value = "$valQ $unit"
        } else if (resource["valueString"] != null) {
          value = resource["valueString"]?.jsonPrimitive?.contentOrNull
        }

        val bodySite =
            resource["bodySite"]
                ?.jsonObject
                ?.get("coding")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("display")
                ?.jsonPrimitive
                ?.contentOrNull

        observations.add(
            Observation(
                id = 0,
                name = name,
                date = date,
                value = value ?: bodySite,
                system = system,
                code = code,
                bodySite = bodySite,
                status = null))
      }

      // Immunization (same as before)
      "immunization" -> {
        val code =
            resource["vaccineCode"]?.jsonObject?.get("coding")?.jsonArray?.firstOrNull()?.jsonObject
        val date =
            resource["occurrenceDateTime"]?.jsonPrimitive?.contentOrNull
                ?: Clock.System.now().toString()
        immunizations.add(
            Immunization(
                id = 0,
                name = code?.get("code")?.jsonPrimitive?.contentOrNull,
                system = code?.get("system")?.jsonPrimitive?.contentOrNull,
                date = date,
                code = code?.get("code")?.jsonPrimitive?.contentOrNull,
                status = "completed"))
      }
    }
  }

  // Resolve medication references
  val medications =
      tempMedications.map {
        val medData =
            it.medRef?.let { ref ->
              medicationResourceMap[ref]?.let { res ->
                val coding =
                    res["code"]?.jsonObject?.get("coding")?.jsonArray?.firstOrNull()?.jsonObject
                mapOf(
                    "system" to coding?.get("system")?.jsonPrimitive?.contentOrNull,
                    "code" to coding?.get("code")?.jsonPrimitive?.contentOrNull)
              }
            } ?: emptyMap()

        Medication(
            id = 0,
            name = it.name,
            date = it.date,
            dosage = it.dosage,
            system = medData["system"] ?: it.system,
            code = medData["code"] ?: it.code,
            status = it.status)
      }

  return IPSModel(
      packageUUID = packageUUID,
      timeStamp = timeStamp,
      patientName = patientName,
      patientGiven = patientGiven,
      patientDob = patientDob,
      patientGender = patientGender,
      patientNation = patientNation,
      patientPractitioner = patientPractitioner,
      patientOrganization = patientOrganization,
      patientIdentifier = patientIdentifier,
      patientIdentifier2 = patientIdentifier2,
      medications = medications,
      allergies = allergies,
      conditions = conditions,
      observations = observations,
      immunizations = immunizations)
}

private data class TempMedication(
    val name: String? = null,
    val date: String? = null,
    val dosage: String? = null,
    var system: String? = null,
    var code: String? = null,
    val status: String? = null,
    val medRef: String? = null // will be used to resolve later
)
