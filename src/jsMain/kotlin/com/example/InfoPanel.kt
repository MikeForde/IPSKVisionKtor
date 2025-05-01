package com.example

import io.kvision.panel.SimplePanel
import io.kvision.panel.tabPanel
import io.kvision.panel.TabPanel
import io.kvision.utils.px

/** Combines the list and detail panels into one split view */
object InfoPanel : SimplePanel() {
    init {
        padding = 25.px
        tabPanel {
                 addTab("About IPS", AboutPanel, icon = "fas fa-medkit")
                 addTab("About App", AboutAppPanel, icon = "fas fa-info-circle")
                 addTab("Change Log", ChangeLogPanel, icon = "fas fa-history")
             } 
    }
}