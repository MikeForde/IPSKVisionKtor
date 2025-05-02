package com.example.serviceHelpers

import com.example.*
import java.util.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*

fun generateIPSBundleUnified(ips: IPSModel): JsonObject {
  val ptId = "pt1"
  var medCount = 0
  var algCount = 0
  var condCount = 0
  var obsCount = 0

  val entries = mutableListOf<JsonObject>()

  // Utility functions
  fun stripMilliseconds(date: String): String = date.substringBeforeLast('.')
  fun stripTime(date: String): String = date.substringBefore('T')
  fun containsNumber(str: String): Boolean = str.any { it.isDigit() }

  // Patient
  entries += buildJsonObject {
    put(
        "resource",
        buildJsonObject {
          put("resourceType", "Patient")
          put("id", ptId)
          putJsonArray("identifier") {
            add(
                buildJsonObject {
                  put("system", "NATO_Id")
                  put("value", ips.patientIdentifier ?: UUID.randomUUID().toString().split("-")[0])
                })
            add(
                buildJsonObject {
                  put("system", "National_Id")
                  put("value", ips.patientIdentifier2 ?: UUID.randomUUID().toString().split("-")[0])
                })
          }
          putJsonArray("name") {
            add(
                buildJsonObject {
                  put("family", ips.patientName)
                  putJsonArray("given") { add(JsonPrimitive(ips.patientGiven)) }
                })
          }
          put("gender", ips.patientGender?.lowercase() ?: "unknown")
          put("birthDate", stripTime(ips.patientDob))
          putJsonArray("address") { add(buildJsonObject { put("country", ips.patientNation) }) }
        })
  }

  // Organization
  if (!ips.patientOrganization.isNullOrBlank()) {
    entries += buildJsonObject {
      put(
          "resource",
          buildJsonObject {
            put("resourceType", "Organization")
            put("id", "org1")
            put("name", ips.patientOrganization)
          })
    }
  }

  // Medications
  ips.medications
      ?.flatMap { med ->
        medCount += 1
        listOf(
            buildJsonObject {
              put(
                  "resource",
                  buildJsonObject {
                    put("resourceType", "MedicationRequest")
                    put("id", "medreq$medCount")
                    put("status", med.status?.lowercase() ?: "active")
                    put(
                        "medicationReference",
                        buildJsonObject {
                          put("reference", "med$medCount")
                          put("display", med.name ?: "Unknown")
                        })
                    put("subject", buildJsonObject { put("reference", "Patient/$ptId") })
                    put("authoredOn", stripMilliseconds(med.date ?: Clock.System.now().toString()))
                    putJsonArray("dosageInstruction") {
                      add(buildJsonObject { put("text", med.dosage ?: "Unknown") })
                    }
                  })
            },
            buildJsonObject {
              put(
                  "resource",
                  buildJsonObject {
                    put("resourceType", "Medication")
                    put("id", "med$medCount")
                    put(
                        "code",
                        buildJsonObject {
                          putJsonArray("coding") {
                            add(
                                buildJsonObject {
                                  put("display", med.name ?: "Unknown")
                                  med.system?.let { put("system", it) }
                                  med.code?.let { put("code", it) }
                                })
                          }
                        })
                  })
            })
      }
      ?.let { entries.addAll(it) }

  // Allergies
  ips.allergies
      ?.map { allergy ->
        algCount += 1
        buildJsonObject {
          put(
              "resource",
              buildJsonObject {
                put("resourceType", "AllergyIntolerance")
                put("id", "allergy$algCount")
                putJsonArray("category") { add(JsonPrimitive("medication")) }
                put("criticality", allergy.criticality?.lowercase() ?: "high")
                put(
                    "code",
                    buildJsonObject {
                      putJsonArray("coding") {
                        add(
                            buildJsonObject {
                              put("display", allergy.name ?: "Unknown")
                              allergy.system?.let { put("system", it) }
                              allergy.code?.let { put("code", it) }
                            })
                      }
                    })
                put("patient", buildJsonObject { put("reference", "Patient/$ptId") })
                put(
                    "onsetDateTime",
                    stripMilliseconds(allergy.date ?: Clock.System.now().toString()))
              })
        }
      }
      ?.let { entries.addAll(it) }

  // Conditions
  ips.conditions
      ?.map { condition ->
        condCount += 1
        buildJsonObject {
          put(
              "resource",
              buildJsonObject {
                put("resourceType", "Condition")
                put("id", "condition$condCount")
                put(
                    "code",
                    buildJsonObject {
                      putJsonArray("coding") {
                        add(
                            buildJsonObject {
                              put("display", condition.name ?: "Unknown")
                              condition.system?.let { put("system", it) }
                              condition.code?.let { put("code", it) }
                            })
                      }
                    })
                put("subject", buildJsonObject { put("reference", "Patient/$ptId") })
                put(
                    "onsetDateTime",
                    stripMilliseconds(condition.date ?: Clock.System.now().toString()))
              })
        }
      }
      ?.let { entries.addAll(it) }

  // Observations
  ips.observations
      ?.map { observation ->
        obsCount += 1
        val obsId = "ob$obsCount"
        val resource = buildJsonObject {
          put("resourceType", "Observation")
          put("id", obsId)
          put("status", observation.status?.lowercase() ?: "final")
          put(
              "code",
              buildJsonObject {
                putJsonArray("coding") {
                  add(
                      buildJsonObject {
                        put("display", observation.name ?: "Unknown")
                        observation.system?.let { put("system", it) }
                        observation.code?.let { put("code", it) }
                      })
                }
              })
          put("subject", buildJsonObject { put("reference", "Patient/$ptId") })
          put(
              "effectiveDateTime",
              stripMilliseconds(observation.date ?: Clock.System.now().toString()))

          val value = observation.value
          if (value != null) {
            if (containsNumber(value)) {
              if (value.contains("-") && (value.contains("mmHg") || value.contains("mm[Hg]"))) {
                val parts =
                    value.replace("mmHg", "").replace("mm[Hg]", "").trim().split("-").map {
                      it.trim().toDoubleOrNull()
                    }
                if (parts.size == 2 && parts.all { it != null }) {
                  putJsonArray("component") {
                    add(buildBPComponent("271649006", "Systolic blood pressure", parts[0]!!))
                    add(buildBPComponent("271650006", "Diastolic blood pressure", parts[1]!!))
                  }
                }
              } else {
                val regex = Regex("""([\d.]+)\s*([^\d\s]+)""")
                val match = regex.find(value)
                if (match != null) {
                  val (num, unit) = match.destructured
                  put(
                      "valueQuantity",
                      buildJsonObject {
                        put("value", num.toDouble())
                        put("unit", unit)
                        put("system", "http://unitsofmeasure.org")
                        put("code", unit)
                      })
                }
              }
            } else {
              put(
                  "bodySite",
                  buildJsonObject {
                    putJsonArray("coding") { add(buildJsonObject { put("display", value) }) }
                  })
            }
          }

          observation.bodySite?.let {
            put(
                "bodySite",
                buildJsonObject {
                  putJsonArray("coding") { add(buildJsonObject { put("display", it) }) }
                })
          }
        }
        buildJsonObject { put("resource", resource) }
      }
      ?.let { entries.addAll(it) }

  // Final bundle
  return buildJsonObject {
    put("resourceType", "Bundle")
    put("id", ips.packageUUID)
    put("timestamp", stripMilliseconds(ips.timeStamp))
    put("type", "collection")
    put("total", entries.size)
    putJsonArray("entry") { entries.forEach { add(it) } }
  }
}

// Blood pressure component builder
private fun buildBPComponent(code: String, display: String, value: Double): JsonObject =
    buildJsonObject {
      put(
          "code",
          buildJsonObject {
            putJsonArray("coding") {
              add(
                  buildJsonObject {
                    put("system", "http://snomed.info/sct")
                    put("code", code)
                    put("display", display)
                  })
            }
          })
      put(
          "valueQuantity",
          buildJsonObject {
            put("value", value)
            put("unit", "mm[Hg]")
            put("system", "http://unitsofmeasure.org")
            put("code", "mm[Hg]")
          })
    }
