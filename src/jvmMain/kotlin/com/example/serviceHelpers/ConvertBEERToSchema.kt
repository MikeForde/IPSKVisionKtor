package com.example.serviceHelpers

import com.example.*
import java.time.Instant

/** Parse a BEER-encoded packet into our IPSModel schema. */
fun parseBeer(dataPacket: String): IPSModel {
  // 1) Choose delimiter by checking for "H9" header
  val candidateDelims = listOf("\n", "|", ";", ":", "@")
  val delim = candidateDelims.firstOrNull { dataPacket.split(it)[0] == "H9" } ?: "\n"

  // 2) Split lines, trimming trailing delimiter
  val lines = dataPacket.trimEnd(delim.single()).split(delim)
  var idx = 0

  // Header sanity checks
  if (lines.getOrNull(idx++) != "H9") throw IllegalArgumentException("Not a BEER packet")
  if (lines.getOrNull(idx++) != "1") throw IllegalArgumentException("Unsupported BEER version")

  // Helpers for date conversion
  fun parseDateYmd(text: String): String =
      text.replace(Regex("(\\d{4})(\\d{2})(\\d{2})"), "$1-$2-$3T00:00:00Z")

  // parse 12-digit datetime into Instant for future sections
  fun parseDateTimeYmdHm(text: String): Instant =
      Instant.parse(
          text.replace(Regex("(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})"), "$1-$2-$3T$4:$5:00Z"))
  //   fun parseDateYmd(text: String): Instant =
  //     Instant.parse(
  //       text.replace(Regex("(\\d{4})(\\d{2})(\\d{2})"), "$1-$2-$3T00:00:00Z")
  //     )
  //   fun parseDateTimeYmdHm(text: String): Instant =
  //     Instant.parse(
  //       text.replace(Regex("(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})"),
  //                    "$1-$2-$3T$4:$5:00Z")
  //     )

  // Read header + patient fields
  val pkgId = lines[idx++] // packageUUID
  val ts = parseDateTimeYmdHm(lines[idx++]).toString()
  val patientName = lines[idx++]
  val patientGiven = lines[idx++]
  val patientDob = parseDateYmd(lines[idx++]).toString()
  val patientGender =
      when (lines[idx++].lowercase()) {
        "m" -> "Male"
        "f" -> "Female"
        "o" -> "Other"
        else -> "Unknown"
      }
  val patientNation = lines[idx++]
  val patientOrg = lines[idx++]
  val patientPract = lines[idx++]

  // prepare mutable lists
  val meds = mutableListOf<Medication>()
  val alrs = mutableListOf<Allergy>()
  val conds = mutableListOf<Condition>()
  val obs = mutableListOf<Observation>()
  val imms = mutableListOf<Immunization>()

  // Pre-timestamp medications (M3-x)
  if (lines.getOrNull(idx)?.startsWith("M") == true) {
    val (countStr) = Regex("^M\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
    val uniqueCount = countStr.toInt()
    repeat(uniqueCount) {
      val name = lines[idx++]
      val datesRaw = lines[idx++] // e.g. "20240501, 20240501"
      val dosage = lines[idx++]

      // split on comma, trim, convert each to ISO-string and add a separate entry
      datesRaw
          .split(',')
          .map { it.trim() }
          .forEach { dateRaw ->
            val dateStr = parseDateYmd(dateRaw) // now returns e.g. "2024-05-01T00:00:00Z"
            meds.add(
                Medication(
                    id = 0,
                    name = name,
                    date = dateStr,
                    dosage = dosage,
                    system = null,
                    code = null,
                    status = null,
                    ipsModelId = null))
          }
    }
  }

  // Allergies (A3-x)
  if (lines.getOrNull(idx)?.startsWith("A") == true) {
    val critMap = mapOf("h" to "high", "m" to "medium", "l" to "low")
    val (countStr) = Regex("^A\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
    repeat(countStr.toInt()) {
      val name = lines[idx++]
      val crit = critMap[lines[idx++].lowercase()] ?: "unknown"
      val date = parseDateYmd(lines[idx++]).toString()
      alrs.add(Allergy(0, name, crit, date, null, null, null))
    }
  }

  // Conditions (C2-x)
  if (lines.getOrNull(idx)?.startsWith("C") == true) {
    val (countStr) = Regex("^C\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
    repeat(countStr.toInt()) {
      val name = lines[idx++]
      val date = parseDateYmd(lines[idx++]).toString()
      conds.add(Condition(0, name, date, null, null, null))
    }
  }

  // Observations before timestamp (O3-x)
  if (lines.getOrNull(idx)?.startsWith("O") == true) {
    val (countStr) = Regex("^O\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
    repeat(countStr.toInt()) {
      val name = lines[idx++]
      val dates = lines[idx++].split(',').map { parseDateYmd(it) }
      val values = lines[idx++].split(',')
      dates.forEachIndexed { i, dt ->
        obs.add(
            Observation(
                0, name, dt.toString(), values.getOrNull(i), null, null, null, null, null, null))
      }
    }
  }

  // Immunizations (I3-x)
  if (lines.getOrNull(idx)?.startsWith("I") == true) {
    val (countStr) = Regex("^I\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
    repeat(countStr.toInt()) {
      val name = lines[idx++]
      val system = lines[idx++]
      val date = parseDateYmd(lines[idx++]).toString()
      imms.add(Immunization(0, name, system, date, null, null))
    }
  }

  // Future meds/obs sections
  if (lines.getOrNull(idx)?.matches(Regex("\\d{12}")) == true) {
    val earliestMed = parseDateTimeYmdHm(lines[idx++])
    // future medications (m3-x)
    if (lines.getOrNull(idx)?.startsWith("m") == true) {
      val (countStr) = Regex("^m\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
      repeat(countStr.toInt()) {
        val name = lines[idx++]
        val minutes = lines[idx++].split(',').map(String::toInt)
        idx++ // skip route
        minutes.forEach { m ->
          val dt = earliestMed.plusSeconds(m * 60L)
          meds.add(Medication(0, name, dt.toString(), "Stat", null, null, null, null))
        }
      }
    }
    // vital-signs (vCount)
    if (lines.getOrNull(idx)?.startsWith("v") == true) {
      val (vCountStr) = Regex("^v(\\d+)\$").find(lines[idx++])!!.destructured
      val obsTypeMap =
          mapOf(
              'B' to "Blood Pressure",
              'P' to "Pulse",
              'R' to "Resp Rate",
              'T' to "Temperature",
              'O' to "Oxygen Sats",
              'A' to "AVPU")
      val obsUnits =
          mapOf('B' to "mmHg", 'P' to "bpm", 'R' to "bpm", 'T' to "cel", 'O' to "%", 'A' to "")
      repeat(vCountStr.toInt()) {
        val line = lines[idx++]
        val type = line[0]
        val entries = line.substring(1).split(',')
        entries.forEach {
          val (time, value) = it.split('+')
          val dt = earliestMed.plusSeconds(time.toLong() * 60)
          obs.add(
              Observation(
                  0,
                  obsTypeMap[type] ?: "",
                  dt.toString(),
                  "${value} ${obsUnits[type]}".trim(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  null))
        }
      }
    }
    // other future obs (oCount)
    if (lines.getOrNull(idx)?.startsWith("o") == true) {
      val (oCountStr) = Regex("^o\\d+-(\\d+)\$").find(lines[idx++])!!.destructured
      repeat(oCountStr.toInt()) {
        val name = lines[idx++]
        val minutes = lines[idx++].split(',').map(String::toInt)
        val vals = lines[idx++].split(',')
        minutes.forEachIndexed { i, m ->
          val dt = earliestMed.plusSeconds(m * 60L)
          obs.add(
              Observation(
                  0, name, dt.toString(), vals.getOrNull(i), null, null, null, null, null, null))
        }
      }
    }
  }

  //  Finally, build and return
  return IPSModel(
      id = null,
      packageUUID = pkgId,
      timeStamp = ts,
      patientName = patientName,
      patientGiven = patientGiven,
      patientDob = patientDob,
      patientGender = patientGender,
      patientNation = patientNation,
      patientOrganization = patientOrg,
      patientPractitioner = patientPract,
      patientIdentifier = null,
      patientIdentifier2 = null,
      medications = meds,
      allergies = alrs,
      conditions = conds,
      observations = obs,
      immunizations = imms)
}
