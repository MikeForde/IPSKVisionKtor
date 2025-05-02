package com.example

import io.kvision.html.*
import io.kvision.html.link
import io.kvision.panel.SimplePanel

object AboutPanel : SimplePanel() {
  init {
    // Outer container with top margin
    div(className = "container mt-5") {
      // First row: header + paragraphs
      div(className = "row") {
        div(className = "col") {
          h3("About International Patient Summary (IPS)")
          p(
              """
                        The International Patient Summary (IPS) is a standardized health data format
                        designed to facilitate the exchange of patient health information across
                        different healthcare systems and countries. It aims to improve interoperability
                        and enable seamless sharing of medical records, ensuring better patient care
                        and safety.
                    """
                  .trimIndent())
          p(
              """
                        IPS contains essential health information such as patient demographics, medical
                        history, medications, allergies, and more, in a structured and standardized
                        format.
                    """
                  .trimIndent())
        }
      }
      // Second row: useful links cards
      div(className = "row mt-4") {
        div(className = "col") {
          h3("Useful Links")
          // Card 1
          div(className = "card mb-3") {
            div(className = "card-body") {
              h5("IPS Implementation Guide", className = "card-title")
              p(
                  """
                                Explore the IPS implementation guide for detailed information on data
                                elements, standards, and implementation best practices.
                            """
                      .trimIndent(),
                  className = "card-text")
              link(
                  "IPS Implementation Guide",
                  "https://www.hl7.org/fhir/uv/ips/",
                  className = "card-link")
            }
          }
          // Card 2
          div(className = "card") {
            div(className = "card-body") {
              h5("IPS Website", className = "card-title")
              p(
                  """
                                Visit the official website of the International Patient Summary (IPS) to 
                                learn more about the standard and its implementation.
                            """
                      .trimIndent(),
                  className = "card-text")
              link(
                  "IPS Website",
                  "https://international-patient-summary.net",
                  className = "card-link")
            }
          }
        }
      }
    }
  }
}
