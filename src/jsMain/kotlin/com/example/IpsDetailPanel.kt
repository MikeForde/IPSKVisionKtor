package com.example

import io.kvision.html.*
import io.kvision.panel.SimplePanel
import io.kvision.state.bind
import io.kvision.table.HeaderCell
import io.kvision.table.TableType
import io.kvision.table.cell
import io.kvision.table.row
import io.kvision.table.table
import io.kvision.utils.px

object IpsDetailPanel : SimplePanel() {

  init {
    padding = 5.px

    bind(Model.selectedIps) { ips ->
      removeAll()
      if (ips != null) {
        // --- Demographics ---
        h4("Demographics")
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
          addHeaderCell(HeaderCell("Field"))
          addHeaderCell(HeaderCell("Value"))
          row {
            cell("PackageUUID")
            cell(ips.packageUUID)
          }
          row {
            cell("Timestamp")
            cell(ips.timeStamp)
          }
          row {
            cell("Patient Name")
            cell(ips.patientName)
          }
          row {
            cell("Given Name")
            cell(ips.patientGiven)
          }
          row {
            cell("Date of Birth")
            cell((ips.patientDob).split("T")[0])
          }
          row {
            cell("Gender")
            cell(ips.patientGender ?: "")
          }
          row {
            cell("Nation")
            cell(ips.patientNation)
          }
          row {
            cell("Practitioner")
            cell(ips.patientPractitioner)
          }
          row {
            cell("Organization")
            cell(ips.patientOrganization ?: "")
          }
          row {
            cell("Identifier")
            cell(ips.patientIdentifier ?: "")
          }
          row {
            cell("Identifier2")
            cell(ips.patientIdentifier2 ?: "")
          }
        }

        // --- Medications ---
        h4("Medications")
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
          addHeaderCell(HeaderCell("Name"))
          addHeaderCell(HeaderCell("Date"))
          addHeaderCell(HeaderCell("Dosage"))
          addHeaderCell(HeaderCell("Status"))
          ips.medications?.forEach { m ->
            row {
              cell(m.name)
              cell(m.date?.substringBefore(".")?.replace("T", " ") ?: "")
              cell(m.dosage)
              cell(m.status)
            }
          }
        }

        // --- Allergies ---
        h4("Allergies")
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
          addHeaderCell(HeaderCell("Name"))
          addHeaderCell(HeaderCell("Date"))
          addHeaderCell(HeaderCell("Criticality"))
          ips.allergies?.forEach { a ->
            row {
              cell(a.name)
              cell(a.date?.substringBefore("T") ?: "")
              cell(a.criticality)
            }
          }
        }

        // --- Conditions ---
        h4("Conditions")
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
          addHeaderCell(HeaderCell("Name"))
          addHeaderCell(HeaderCell("Date"))
          ips.conditions?.forEach { c ->
            row {
              cell(c.name)
              cell(c.date?.substringBefore("T") ?: "")
            }
          }
        }

        // --- Observations ---
        h4("Observations")
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
          addHeaderCell(HeaderCell("Name"))
          addHeaderCell(HeaderCell("Date"))
          addHeaderCell(HeaderCell("Value"))
          addHeaderCell(HeaderCell("Status"))
          ips.observations?.forEach { o ->
            row {
              cell(o.name)
              cell(o.date?.substringBefore(".")?.replace("T", " ") ?: "")
              cell(o.value)
              cell(o.status)
            }
          }
        }

        // --- Immunizations ---
        h4("Immunizations")
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
          addHeaderCell(HeaderCell("Name"))
          addHeaderCell(HeaderCell("Date"))
          addHeaderCell(HeaderCell("Status"))
          ips.immunizations?.forEach { i ->
            row {
              cell(i.name)
              cell(i.date?.substringBefore(".")?.replace("T", " ") ?: "")
              cell(i.status)
            }
          }
        }
      }
    }
  }
}
