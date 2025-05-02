package com.example

import io.kvision.panel.Direction
import io.kvision.panel.SimplePanel
import io.kvision.panel.splitPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.utils.vh
import kotlinx.browser.window

/**
 * Combines the list and detail panels into one split view, but on narrow screens (<768px) shows a
 * horizontal split (top/bottom).
 */
object IPSHomePanel : SimplePanel() {
  init {
    padding = 25.px

    // a single function to (re)build our splitPanel
    fun buildSplit() {
      removeAll() // clear out any existing children

      val dir =
          if (window.innerWidth < 1000) {
            Direction.HORIZONTAL // stacked: list on top, details below
          } else {
            Direction.VERTICAL // side-by-side
          }

      splitPanel(dir) {
        width = 100.perc
        height = 100.vh
        add(IpsListPanel)
        add(IpsDetailPanel)
      }
    }

    // initial build
    buildSplit()

    // rebuild on every resize
    window.addEventListener("resize", { buildSplit() })
  }
}
