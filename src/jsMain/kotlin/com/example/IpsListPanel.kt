package com.example

import io.kvision.core.AlignItems
import io.kvision.core.FontStyle
import io.kvision.core.onEvent
import io.kvision.form.check.RadioGroup
import io.kvision.form.check.radioGroup
import io.kvision.form.text.TextInput
import io.kvision.form.text.text
import io.kvision.html.InputType
import io.kvision.html.icon
import io.kvision.html.link
import io.kvision.i18n.I18n.tr
import io.kvision.modal.Confirm
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.state.bind
import io.kvision.table.HeaderCell
import io.kvision.table.TableType
import io.kvision.table.cell
import io.kvision.table.row
import io.kvision.table.table
import io.kvision.utils.px

object IpsListPanel : SimplePanel() {

    init {
        padding = 5.px

        // you probably want search/filter later — for now just the table
        table(types = setOf(TableType.STRIPED, TableType.HOVER)) {
            addHeaderCell(HeaderCell("Package UUID"))
            addHeaderCell(HeaderCell("Patient Name"))
            addHeaderCell(HeaderCell("Date of Birth"))
            // bind to the new IPS list in your Model
            bind(Model.ipsRecords) { list ->
                list.forEachIndexed { index, ips ->
                    row {
                        cell(ips.packageUUID)
                        cell(ips.patientName)
                        cell(ips.patientDob)  // now a String
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