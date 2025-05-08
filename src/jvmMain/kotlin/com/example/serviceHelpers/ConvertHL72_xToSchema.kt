package com.example.serviceHelpers

import com.example.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.datetime.Clock

/** Parse a raw HL7 v2.3 message (string) into our IPSModel schema. */
fun parseHL72_xToIpsModel(hl7Message: String): IPSModel {
  // split on newlines, drop blanks
  val lines = hl7Message.split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }

  // defaults
  var packageUUID = java.util.UUID.randomUUID().toString()
  var timeStamp = Clock.System.now().toString()

  var patientName = "Unknown"
  var patientGiven = "Unknown"
  var patientDob = "1900-01-01"
  var patientGender: String? = null
  var patientNation = "Unknown"
  var patientOrganization: String? = null
  var patientPractitioner = "Unknown"

  val medications = mutableListOf<Medication>()
  val allergies = mutableListOf<Allergy>()
  val conditions = mutableListOf<Condition>()
  val observations = mutableListOf<Observation>()
  val immunizations = mutableListOf<Immunization>()

  // HL7 date/time formats
  val dtFmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
  val dFmt = DateTimeFormatter.ofPattern("yyyyMMdd")

  lines.forEach { line ->
    val seg = line.split('|')
    when (seg[0]) {
      // ---- message header ----
      "MSH" -> {
        // MSH-10 → packageUUID
        seg.getOrNull(9)?.takeIf(String::isNotBlank)?.let { packageUUID = it }

        // MSH-7 → timestamp
        seg.getOrNull(6)?.let { ts ->
          runCatching {
            val dtInst = LocalDateTime.parse(ts, dtFmt).toInstant(ZoneOffset.UTC)
            timeStamp = dtInst.toString()
          }
        }
      }

      // ---- patient ----
      "PID" -> {
        // PID-5 = family^given
        seg.getOrNull(5)?.split('^')?.let { parts ->
          if (parts.isNotEmpty()) patientName = parts[0]
          if (parts.size > 1) patientGiven = parts[1]
        }

        // PID-7 = YYYYMMDD
        seg.getOrNull(7)?.let { d ->
          runCatching { patientDob = LocalDate.parse(d, dFmt).toString() }
        }

        // PID-8 = gender
        seg.getOrNull(8)?.let {
          patientGender =
              when (it) {
                "M" -> "Male"
                "F" -> "Female"
                else -> "Other"
              }
        }

        // PID-11.4 = country
        seg.getOrNull(11)?.split('^')?.getOrNull(3)?.takeIf(String::isNotBlank)?.let {
          patientNation = it
        }

        // PID-3.4 = org (just re-using)
        seg.getOrNull(3)?.split('^')?.getOrNull(3)?.takeIf(String::isNotBlank)?.let {
          patientOrganization = it
        }
      }

      // ---- practitioner (custom) ----
      "IVC" -> {
        seg.getOrNull(2)?.takeIf(String::isNotBlank)?.let { patientPractitioner = it }
      }

      // ---- immunization vs medication ----
      "RXA" -> {
        // RXA-4 = date, RXA-6 = code^name^system^[…]^dosage
        val dateIso =
            seg.getOrNull(3)?.let { ts ->
              runCatching { LocalDateTime.parse(ts, dtFmt).toInstant(ZoneOffset.UTC).toString() }
                  .getOrNull()
            } ?: Clock.System.now().toString()

        seg.getOrNull(5)?.split('^')?.let { parts ->
          if (parts.size < 5) {
            // immunization
            immParts@ run {
              val code = parts.getOrNull(0)
              val name = parts.getOrNull(1)
              val system = parts.getOrNull(2)
              immunizations +=
                  Immunization(
                      id = 0,
                      name = name,
                      system = system,
                      date = dateIso,
                      code = code,
                      status = null)
            }
          } else {
            // medication
            val code = parts.getOrNull(0)
            val name = parts.getOrNull(1)
            val system = parts.getOrNull(2)
            val dosage = parts.getOrNull(4)
            medications +=
                Medication(
                    id = 0,
                    name = name,
                    date = dateIso,
                    dosage = dosage,
                    system = system,
                    code = code,
                    status = null,
                    ipsModelId = null)
          }
        }
      }

      // ---- allergies ----
      "AL1" -> {
        seg.getOrNull(3)?.split('^')?.let { parts ->
          val code = parts.getOrNull(0)
          val name = parts.getOrNull(1)
          val system = parts.getOrNull(2)
          val critMap = mapOf("SV" to "high", "MO" to "moderate", "MI" to "mild", "U" to "unknown")
          val crit = critMap[seg.getOrNull(4)] ?: "unknown"
          val date =
              seg.getOrNull(6)?.let { d ->
                runCatching { LocalDate.parse(d, dFmt).toString() }.getOrNull()
              }
          allergies +=
              Allergy(
                  id = 0,
                  name = name,
                  criticality = crit,
                  date = date,
                  system = system,
                  code = code,
                  ipsModelId = null)
        }
      }

      // ---- conditions ----
      "DG1" -> {
        seg.getOrNull(3)?.split('^')?.let { parts ->
          val code = parts.getOrNull(0)
          val name = parts.getOrNull(1)
          val system = parts.getOrNull(2)
          val date =
              seg.getOrNull(5)?.let { d ->
                runCatching { LocalDate.parse(d, dFmt).toString() }.getOrNull()
              }
          conditions +=
              Condition(
                  id = 0, name = name, date = date, system = system, code = code, ipsModelId = null)
        }
      }

      // ---- observations ----
      "OBX" -> {
        seg.getOrNull(3)?.split('^')?.let { parts ->
          val code = parts.getOrNull(0)
          val name = parts.getOrNull(1)
          val system = parts.getOrNull(2)
          val value = listOfNotNull(seg.getOrNull(5), seg.getOrNull(6)).joinToString(" ").trim()
          val date =
              seg.getOrNull(12)?.let { ts ->
                runCatching { LocalDateTime.parse(ts, dtFmt).toInstant(ZoneOffset.UTC).toString() }
                    .getOrNull()
              }
          observations +=
              Observation(
                  id = 0,
                  name = name,
                  date = date,
                  value = value,
                  system = system,
                  code = code,
                  valueCode = null,
                  bodySite = null,
                  status = null,
                  ipsModelId = null)
        }
      }
    // other segments are ignored…
    }
  }

  // return it all as an IPSModel
  return IPSModel(
      id = null,
      packageUUID = packageUUID,
      timeStamp = timeStamp,
      patientName = patientName,
      patientGiven = patientGiven,
      patientDob = patientDob,
      patientGender = patientGender,
      patientNation = patientNation,
      patientPractitioner = patientPractitioner,
      patientOrganization = patientOrganization,
      patientIdentifier = null,
      patientIdentifier2 = null,
      medications = medications,
      allergies = allergies,
      conditions = conditions,
      observations = observations,
      immunizations = immunizations)
}
