package com.example

import io.kvision.panel.SimplePanel
import io.kvision.panel.splitPanel
import io.kvision.utils.perc
import io.kvision.utils.vh

/** Combines the list and detail panels into one split view */
object IPSHomePanel : SimplePanel() {
    init {
        splitPanel {
            width = 100.perc
            height = 100.vh
            add(IpsListPanel)
            add(IpsDetailPanel)
        }
    }
}
