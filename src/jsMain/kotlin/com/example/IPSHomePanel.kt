package com.example

import io.kvision.panel.Direction
import io.kvision.panel.SimplePanel
import io.kvision.panel.splitPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.utils.vh
import kotlinx.browser.window

/**
 * Combines the list and detail panels into one split view, on mobile shows a horizontal split
 * (top/bottom).
 */
object IPSHomePanel : SimplePanel() {
  init {
    padding = 25.px

    var lastWidth = window.innerWidth.toInt()

    // (re)build splitPanel
    fun buildSplit() {
      removeAll() // clear children

      val dir =
          if (window.innerWidth < 1000) {
            Direction.HORIZONTAL
          } else {
            Direction.VERTICAL
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
    window.addEventListener(
        "resize",
        {
          val newWidth = window.innerWidth.toInt()
          if (newWidth != lastWidth) {
            lastWidth = newWidth
            buildSplit()
          }
        })
  }
}
