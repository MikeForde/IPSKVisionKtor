package com.example

import io.kvision.html.*
import io.kvision.panel.SimplePanel

object ChangeLogPanel : SimplePanel() {
  init {
    // Outer container with top margin
    div(className = "container mt-5") {
      // First row: header + paragraphs
      div(className = "row") {
        div(className = "col") {
          h3("ChangeLog - Versions refer to Cloud Deployments")
          ul() {
            // Next li should have no bullet
            li(className = "list-unstyled")
            h5("Version 0_1 (Initial Release) - 30th April 2025")
            ul() {
              // First item in the list should be in bold
              li { strong("Initial release of the application.") }
              li("Basic functionality implemented.")
              li(
                  "Single page with split panel, list (all records) + detail when record on list clicked.")
            }
            li(className = "list-unstyled")
            h5("Version 0_2 - 1st May 2025")
            ul() {
              li { strong("Improved Navigation and About Pages") }
              li("Added navigation bar with links to different sections.")
              li("Implemented an About page with application information.")
            }
            li(className = "list-unstyled")
            h5("Version 0_3 - 2nd May 2025")
            ul {
              li { strong("DataFormat Panel and IPS Converters") }
              li("Added API Page for viewing unified FHIR JSON bundle in a scrollable textarea.")
              li(
                  "Implemented /api/convertIPS POST endpoint - will parse IPS FHiR JSON in variety of formats.")
              li(
                  "Implemented /api/convertSchemaToUnified POST endpoint - outputs FHiR JSON in unified format.")
            }
          }
        }
      }
    }
  }
}
