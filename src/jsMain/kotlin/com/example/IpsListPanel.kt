package com.example

// import io.kvision.core.FontStyle

import io.kvision.core.AlignItems
import io.kvision.core.onEvent
import io.kvision.form.text.text
import io.kvision.html.InputType
import io.kvision.html.button
import io.kvision.i18n.I18n.tr
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.state.bind
import io.kvision.table.HeaderCell
import io.kvision.table.TableType
import io.kvision.table.cell
import io.kvision.table.row
import io.kvision.table.table
import kotlinx.coroutines.launch

object IpsListPanel : SimplePanel() {

  init {

    // search with text input
    hPanel(alignItems = AlignItems.CENTER, spacing = 10) {
      val searchInput = text(InputType.SEARCH) { placeholder = tr("Surname") }
      button(tr("Find")) {
        onEvent {
          click = {
            // clear any selection
            Model.selectedIps.value = null
            // run the surname search
            AppScope.launch { Model.findByLastName(searchInput.value ?: "") }
          }
        }
      }
    }

    // results table
    table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
      addHeaderCell(HeaderCell("PackageUUID"))
      addHeaderCell(HeaderCell("Patient Name"))
      addHeaderCell(HeaderCell("Date of Birth"))
      // bind to the new IPS list in your Model
      bind(Model.ipsRecords) { list ->
        list.forEachIndexed { index, ips ->
          row {
            cell(ips.packageUUID)
            cell(ips.patientName)
            cell((ips.patientDob).split("T")[0]) // now a String
            onEvent {
              click = {
                // update the selected record
                Model.selectedIps.value = ips
              }
            }
          }
        }
      }
    }
  }
}
