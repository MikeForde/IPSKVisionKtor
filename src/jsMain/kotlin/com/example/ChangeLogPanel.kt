package com.example

import io.kvision.html.*
import io.kvision.panel.SimplePanel
import io.kvision.utils.px

object ChangeLogPanel : SimplePanel() {
  init {
    padding = 5.px
    // Outer container with top margin
    div(className = "container") {
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
            li(className = "list-unstyled")
            h5("Version 0_4 - 6th May 2025")
            ul {
              li { strong("NFC Reader Panel") }
              li("Added NFC Page for reading NFC cards in both plain text and agreed MIME formats.")
              li("Supports all x.ips binary NDEF formats.")
              li(
                  "Allows simple viewing, conversion to internal schema format (display only) and full import to IPS database.")
            }
            li(className = "list-unstyled") {
              h5("Version 0_5 - 7th May 2025")
              ul {
                li { strong("Data Format Panel Enhancements") }
                li("Patient selector dropdown bound to Model.ipsRecords and Model.selectedIps.")
                li(
                    "Mode selection, compression & encryption checkboxes integrated with dynamic reload on toggle.")
                li(
                    "‘Gzip + Encrypt’ option implemented via encryptTextGzip RPC, with optional key visibility.")
                li(
                    "Download button now builds timestamped filenames with proper extension, MIME type, and _ce_ik suffix.")
                li(
                    "Write-to-NFC button uses Web NFC API for plain-text or binary (compressed+encrypted) payloads.")
                li("Header now displays live response-size count.")
                li(
                    "Textarea sizing switched to CSS-driven responsive layout instead of fixed height.")
              }
            }
            li(className = "list-unstyled") {
              h5("Version 0_6 - 8th May 2025")
              ul {
                li { strong("New QR Code Panel") }
                li("Added `QRPanel` for generating scannable QR codes from IPS data.")
                li(
                    "Supports both raw bundle content and encoded `ipsurl` links to backend API endpoints.")
                li(
                    "Compression + encryption toggle (gzip + AES256 base64) integrated with backend RPC.")
                li(
                    "Payload size limit enforced (max 3000 bytes); oversized payloads trigger toast warning.")
                li("QR code is displayed as a PNG image with built-in download button.")
                li(
                    "Downloaded QR filenames include timestamp, patient info, UUID suffix, and mode.")
              }
            }
            li(className = "list-unstyled") {
              h5("Version 0_7 - 11th May 2025")
              ul {
                li { strong("Unified Data Import") }
                li("Backend upsertIPSData determines matches incoming data by packageUUID")
                li("If packageUUID not found then new record created.")
                li(
                    "If found then new data merged with old. This avoids duplicates and allows partial updates.")
                li(
                    "Medication, Allergies etc - are considered updates to existing items if matched on name+datetime ")
                li(
                    "The system sending does not need to know if the record [+/- data] it is sending is new or not.")
              }
            }
          }
        }
      }
    }
  }
}
