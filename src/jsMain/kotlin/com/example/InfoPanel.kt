package com.example

import io.kvision.panel.SimplePanel
import io.kvision.panel.tabPanel
import io.kvision.panel.TabPanel

/** Combines the list and detail panels into one split view */
object InfoPanel : SimplePanel() {
    init {
        tabPanel {
                 addTab("About IPS", AboutPanel, icon = "fas fa-info-circle")
                 addTab("About App", AboutAppPanel, icon = "fas fa-info-circle")
             } 
    }
}