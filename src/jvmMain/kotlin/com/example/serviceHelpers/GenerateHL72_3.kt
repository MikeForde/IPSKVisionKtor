package com.example.serviceHelpers

import com.example.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Generate an HL7 v2.3 message (string) from our IPSModel schema. */
fun generateIpsModelToHl72_3(model: IPSModel): String {
  // formatters
  val dtFmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC)
  val dFmt = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

  val sb = StringBuilder()

  // ---- MSH Segment ----
  sb.append("MSH|^~\\&|IPSMERN|UKMOD|ReceivingApp|ReceivingFac|")
      .append(Instant.parse(model.timeStamp).let { dtFmt.format(it) })
      .append("||MDM^T01|")
      .append(model.packageUUID)
      .append("|P|2.3\n")

  // ---- PID Segment ----
  val genderCode = model.patientGender?.firstOrNull()?.toString() ?: ""
  sb.append("PID|||123456^^^")
      .append(model.patientOrganization.orEmpty())
      .append("^ISO||")
      .append(model.patientName)
      .append("^")
      .append(model.patientGiven)
      .append("||")
      .append(Instant.parse(model.patientDob).let { dFmt.format(it) }) // "yyyy-MM-dd" -> "yyyyMMdd"
      .append("|")
      .append(genderCode)
      .append("|||^^^")
      .append(model.patientNation)
      .append("|||\n")

  // ---- IVC Segment (practitioner) ----
  model.patientPractitioner
      .takeIf { it.isNotBlank() }
      ?.let { sb.append("IVC||").append(it).append("\n") }

  // ---- RXA Segments: medications ----
  model.medications.orEmpty().forEachIndexed { idx, med ->
    val ts = Instant.parse(med.date).let { dtFmt.format(it) }
    sb.append("RXA|0|1|")
        .append(ts)
        .append("|")
        .append(ts)
        .append("|${med.code}^${med.name}^${med.system}^MED^${med.dosage}")
        .append("\n")
  }

  // ---- RXA Segments: immunizations ----
  model.immunizations.orEmpty().forEachIndexed { idx, imm ->
    val ts = Instant.parse(imm.date).let { dtFmt.format(it) }
    sb.append("RXA|0|1|")
        .append(ts)
        .append("|")
        .append(ts)
        .append("|${imm.code}^${imm.name}^${imm.system}^IMM")
        .append("\n")
  }

  // ---- AL1 Segments (allergies) ----
  val critMap = mapOf("high" to "SV", "moderate" to "MO", "mild" to "MI", "unknown" to "U")
  model.allergies.orEmpty().forEachIndexed { idx, alg ->
    val sev = critMap[alg.criticality.orEmpty().lowercase()] ?: "U"
    val date = Instant.parse(alg.date).let { dFmt.format(it) }
    sb.append("AL1|${idx+1}|DA|${alg.code}^${alg.name}^${alg.system}|$sev||$date\n")
  }

  // ---- DG1 Segments (conditions) ----
  model.conditions.orEmpty().forEachIndexed { idx, cond ->
    val date = Instant.parse(cond.date).let { dFmt.format(it) }
    sb.append("DG1|${idx+1}||${cond.code}^${cond.name}^${cond.system}||$date\n")
  }

  // ---- OBX Segments (observations) ----
  model.observations.orEmpty().forEachIndexed { idx, obx ->
    // determine type/units
    val (valueType, value, units) =
        obx.value?.let { v ->
          Regex("""^(\d+(?:\.\d+)?(?:-\d+(?:\.\d+)?)?)\s*(\S+)?$""")
              .matchEntire(v)
              ?.destructured
              ?.let { (num, unit) -> Triple("NM", num, unit ?: "") } ?: Triple("TX", v, "")
        } ?: Triple("CE", "", "")

    val ts = obx.date?.let { Instant.parse(it).let { dtFmt.format(it) } }.orEmpty()

    sb.append("OBX|${idx+1}|$valueType|${obx.code}^${obx.name}^${obx.system}")
        .append("||$value|$units|||F|||$ts\n")
  }

  // ---- PV1 Segment ----
  sb.append("PV1|1|N\n")

  return sb.toString()
}
