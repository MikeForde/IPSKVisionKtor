package com.example

import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.html.link
import io.kvision.html.p
import io.kvision.panel.SimplePanel
import io.kvision.utils.px
import kotlinx.browser.window

object AboutAppPanel : SimplePanel() {

  init {
    padding = 10.px

    // About IPS
    div(className = "container mt-5") {
      div(className = "row") {
        div(className = "col") {
          h3("About This Web Application")
          p(
              """
                        Built using the S3K Stack. S3K stands for SQL, Kotlin, KVision, and Ktor.
                    """
                  .trimIndent())
        }
      }

      // Development Pipeline & Version Control & Deployment
      div(className = "row mt-4") {
        div(className = "col") {
          h3("Development Pipeline")
          div(className = "card") {
            div(className = "card-body") {
              // Dev environment
              p(
                  "• Dev environment: VSCode plus Docker devcontainer (Java 21 Bullseye)",
                  className = "card-text")
              // Version control with clickable link
              p(className = "card-text") {
                +"• Version control: GitHub ("
                link(
                    "IPSKVisionKtor",
                    "https://github.com/MikeForde/IPSKVisionKtor",
                    className = "card-link",
                    target = "_blank")
                +")"
              }
              // Deployment
              p(
                  "• Deployment: Combined frontend-backend jar file to OpenShift using a Dockerfile strategy",
                  className = "card-text")
            }
          }
        }
      }

      // Hosting Options
      div(className = "row mt-4") {
        div(className = "col") {
          h3("Hosting Options")
          div(className = "card") {
            div(className = "card-body") {
              val isLocal = window.location.hostname == "localhost"
              val hostingText =
                  if (isLocal) {
                    """
                                This web application is running locally on your development machine.
                                In production it can be deployed via fat‐jar Docker images to OpenShift
                                or any container platform.
                                """
                        .trimIndent()
                  } else {
                    """
                                This web application is deployed on a remote server (e.g. OpenShift).
                                It can equally run in local Docker‐based devcontainers or other clouds.
                                """
                        .trimIndent()
                  }
              p(hostingText, className = "card-text")
            }
          }
        }
      }

      // Future Development
      div(className = "row mt-4") {
        div(className = "col") {
          h3("Future Development and Experimentation")
          div(className = "card") {
            div(className = "card-body") {
              p(
                  """
                                Many features of this prototype are designed for demonstration and
                                experimentation. In a production-ready version, some aspects would be
                                implemented differently or omitted. Feedback and ideas are welcome!
                            """
                      .trimIndent(),
                  className = "card-text")
            }
          }
        }
      }
    }
  }
}
