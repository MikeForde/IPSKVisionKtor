package com.example.serviceHelpers

import com.example.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

fun generateIpsBeer(record: IPSModel, delim: String?): String {
  // sort delim
  val delimiterMap =
      mapOf("semi" to ";", "colon" to ":", "pipe" to "|", "at" to "@", "newline" to "\n")
  val delimiter = delimiterMap[delim] ?: "\n"
  // formatters
  val localDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
  val localDateTimeFmt = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC)

  fun formatDate(input: String?): String =
      input
          // parse the full ISO-8601 timestamp, then convert to a LocalDate
          ?.let { Instant.parse(it).atZone(ZoneOffset.UTC).toLocalDate() }
          ?.format(localDateFmt)
          .orEmpty()

  fun formatDateTime(input: String?): String =
      input
          // parse the full ISO-8601 timestamp directly to an Instant
          ?.let { Instant.parse(it) }
          ?.let { localDateTimeFmt.format(it) }
          .orEmpty()

  fun isVitalSign(name: String) =
      listOf("Blood Pressure", "Pulse", "Resp Rate", "Temperature", "Oxygen Sats", "AVPU")
          .contains(name)

  fun formatVitalSigns(vitals: List<Observation>, earliest: Instant): String {
    val typeMap =
        mapOf(
            "Blood Pressure" to "B",
            "Pulse" to "P",
            "Resp Rate" to "R",
            "Temperature" to "T",
            "Oxygen Sats" to "O",
            "AVPU" to "A")
    return vitals
        .groupBy { typeMap[it.name] ?: "?" }
        .map { (type, list) ->
          val entries =
              list.map { obs ->
                val diffMin =
                    ((Instant.parse(obs.date).epochSecond - earliest.epochSecond) / 60).toString()
                val rawVal =
                    if (obs.name == "Blood Pressure") {
                      // preserve both systolic/diastolic but strip any non‐digits
                      obs.value
                          .orEmpty()
                          .split('-')
                          .map { part -> part.replace(Regex("[^0-9]"), "") }
                          .joinToString("-")
                    } else {
                      // drop everything except digits for P, R, T, O, etc.
                      obs.value.orEmpty().replace(Regex("[^0-9]"), "")
                    }
                "$diffMin+$rawVal"
              }
          type + entries.joinToString(",")
        }
        .joinToString(delimiter)
  }

  val now = Instant.parse(record.timeStamp)

  val sb =
      StringBuilder()
          .append("H9")
          .append(delimiter)
          .append("1")
          .append(delimiter)
          .append(record.packageUUID)
          .append(delimiter)
          .append(formatDateTime(record.timeStamp))
          .append(delimiter)
          .append(record.patientName)
          .append(delimiter)
          .append(record.patientGiven)
          .append(delimiter)
          .append(formatDate(record.patientDob))
          .append(delimiter)
          .append(
              mapOf("male" to "m", "female" to "f", "other" to "o")
                  .getOrDefault(record.patientGender.orEmpty().lowercase(), "u"))
          .append(delimiter)
          .append(record.patientPractitioner)
          .append(delimiter)
          .append(record.patientNation)
          .append(delimiter)
          .append(record.patientOrganization.orEmpty())
          .append(delimiter)

  // Medications
  val (pastMeds, futureMeds) =
      record.medications.orEmpty().partition { Instant.parse(it.date).isBefore(now) }
  if (pastMeds.isNotEmpty()) {
    val uniq = pastMeds.map { it.name }.distinct()
    sb.append("M3-${uniq.size}").append(delimiter)
    uniq.forEach { name ->
      val entries = pastMeds.filter { it.name == name }
      sb.append(name)
          .append(delimiter)
          .append(entries.joinToString(", ") { formatDate(it.date) })
          .append(delimiter)
          .append(entries.first().dosage)
          .append(delimiter)
    }
  }

  // Allergies
  if (record.allergies.orEmpty().isNotEmpty()) {
    val critMap = mapOf("high" to "h", "medium" to "m", "moderate" to "m", "low" to "l")
    val al = record.allergies.orEmpty()
    sb.append("A3-${al.size}").append(delimiter)
    al.forEach {
      sb.append(it.name)
          .append(delimiter)
          .append(critMap[it.criticality.orEmpty().lowercase()].orEmpty())
          .append(delimiter)
          .append(formatDate(it.date))
          .append(delimiter)
    }
  }

  // Conditions
  if (record.conditions.orEmpty().isNotEmpty()) {
    val conds = record.conditions.orEmpty()
    sb.append("C2-${conds.size}").append(delimiter)
    conds.forEach {
      val diffMin = ((Instant.parse(it.date).epochSecond - now.epochSecond) / 60).toLong()
      val timeStr = if (abs(diffMin) < 1440) diffMin.toString() else formatDate(it.date)
      sb.append(it.name).append(delimiter).append(timeStr).append(delimiter)
    }
  }

  // Observations
  val (pastObs, futureObs) =
      record.observations.orEmpty().partition { Instant.parse(it.date).isBefore(now) }
  if (pastObs.isNotEmpty()) {
    val uniq = pastObs.map { it.name }.distinct()
    sb.append("O3-${uniq.size}").append(delimiter)
    uniq.forEach { name ->
      val entries = pastObs.filter { it.name == name }
      sb.append(name)
          .append(delimiter)
          .append(entries.joinToString(",") { formatDate(it.date) })
          .append(delimiter)
          .append(entries.first().value.orEmpty())
          .append(delimiter)
    }
  }

  // Immunizations
  if (record.immunizations.orEmpty().isNotEmpty()) {
    val imms = record.immunizations.orEmpty()
    sb.append("I3-${imms.size}").append(delimiter)
    imms.forEach {
      sb.append(it.name)
          .append(delimiter)
          .append(it.system)
          .append(delimiter)
          .append(formatDate(it.date))
          .append(delimiter)
    }
  }

  // Future Medications
  if (futureMeds.isNotEmpty()) {
    val earliestMed = futureMeds.minByOrNull { Instant.parse(it.date) }!!
    sb.append(formatDateTime(earliestMed.date))
        .append(delimiter)
        .append("m3-${futureMeds.map { it.name }.distinct().size}")
        .append(delimiter)
    futureMeds
        .map { it.name }
        .distinct()
        .forEach { name ->
          val entries = futureMeds.filter { it.name == name }
          val diffs =
              entries
                  .map {
                    ((Instant.parse(it.date).epochSecond -
                            Instant.parse(earliestMed.date).epochSecond) / 60)
                        .toString()
                  }
                  .joinToString(",")
          sb.append(name)
              .append(delimiter)
              .append(diffs)
              .append(delimiter)
              .append("O${entries.size}")
              .append(delimiter)
        }
  }

  // Future Observations
  if (futureObs.isNotEmpty()) {
    val earliestObs = futureObs.minByOrNull { Instant.parse(it.date) }!!
    sb.append(formatDateTime(earliestObs.date)).append(delimiter)

    // use .orEmpty() so name (String?) becomes String
    val vitals = futureObs.filter { isVitalSign(it.name.orEmpty()) }
    if (vitals.isNotEmpty()) {
      sb.append("v${vitals.map { it.name.orEmpty() }.distinct().size}")
          .append(delimiter)
          .append(formatVitalSigns(vitals, Instant.parse(earliestObs.date)))
          .append(delimiter)
    }

    val others = futureObs.filterNot { isVitalSign(it.name.orEmpty()) }
    if (others.isNotEmpty()) {
      sb.append("o3-${others.size}").append(delimiter)
      others.forEach {
        val diff =
            ((Instant.parse(it.date).epochSecond - Instant.parse(earliestObs.date).epochSecond) /
                60)
        sb.append(it.name.orEmpty())
            .append(delimiter)
            .append(diff)
            .append(delimiter)
            .append(it.value.orEmpty())
            .append(delimiter)
      }
    }
  }

  return sb.toString()
}
