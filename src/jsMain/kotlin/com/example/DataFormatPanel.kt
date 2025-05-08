package com.example

import io.kvision.core.onEvent
import io.kvision.form.check.CheckBox
import io.kvision.form.check.switch
import io.kvision.form.select.SelectInput
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.h3
import io.kvision.panel.SimplePanel
import io.kvision.panel.VPanel
import io.kvision.state.bind
import io.kvision.toast.Toast
import io.kvision.utils.px
import kotlin.js.Date
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

object DataFormatPanel : SimplePanel() {
  private val scope = MainScope()
  private val textArea = textArea()
  private val header = h3("API GET - IPS Data: 0")

  private fun sanitize(input: String): String =
      input.uppercase().replace(Regex("[^A-Z0-9]"), "_").replace(Regex("_+"), "_").trim('_')

  // Patient selector (disabled until patients are loaded)
  private val patientSelect =
      SelectInput(options = listOf(), value = null).apply { disabled = true }

  // Format selector (disabled until a patient is selected)
  private val modeSelect =
      SelectInput(
              options =
                  listOf(
                      "ipsunified" to "IPS Unified JSON Bundle"
                      // TODO: add more format options when generators are available
                      ),
              value = "ipsunified")
          .apply { disabled = true }

  private val compressionCheck = CheckBox(label = "Gzip + Encrypt (AES256 base64)")
  private val binarySwitch = switch(label = "Write to NFC using binary (AES256+gzip)")

  private val downloadButton =
      button("Download Data") {
        disabled = true
        onClick {
          Model.selectedIps.value?.let { selected ->
            // 1) Build YYYYMMDD
            val now = Date()
            val pad: (Int) -> String = { it.toString().padStart(2, '0') }
            val yyyymmdd = "${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}"

            // 2) Patient info
            val fam = sanitize(selected.patientName)
            val giv = sanitize(selected.patientGiven)
            val last6 = selected.packageUUID.takeLast(6)
            val mode = modeSelect.value ?: "ipsunified"

            // 3) Determine extension & MIME
            val (ext, mime) =
                when {
                  compressionCheck.value -> "json" to "application/json"
                  mode == "ipsxml" -> "xml" to "application/xml"
                  mode in listOf("ipsbasic", "ipsbeer", "ipsbeerwithdelim", "ipshl72x") ->
                      "txt" to "text/plain"
                  else -> "json" to "application/json"
                }

            // 4) Suffixes
            val ceIkSuffix = if (compressionCheck.value) "_ce_ik" else ""
            val fname = "$yyyymmdd-${fam}_${giv}_${last6}_${mode}${ceIkSuffix}.$ext"

            // 5) Create Blob & trigger download
            val blob = Blob(arrayOf(textArea.value), BlobPropertyBag(type = mime))
            val url = URL.createObjectURL(blob)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = url
            link.download = fname
            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)
            URL.revokeObjectURL(url)
          }
        }
      }
  private val nfcButton =
      button("Write to NFC") {
        disabled = true
        onClick {
          if (js("typeof NDEFReader") != "function") {
            Toast.warning("Web NFC not supported on this device/browser.")
          } else {
            // 2) Launch a coroutine to call the JS API
            scope.launch {
              try {
                val reader = NDEFReader()
                // 3) Write either plain text or binary, per the switch
                if (binarySwitch.value) {
                  // 1) Get gzip-encrypted bytes via your RPC
                  val rawJson = Model.generateUnifiedBundle(patientSelect.value?.toInt())
                  val compressed: ByteArray = Model.encryptAndCompress(rawJson)
                  // 2) Build a dynamic NDEF message with a MIME record
                  val msg = js("{}")
                  msg.records =
                      arrayOf(
                          js(
                              "({ recordType: 'mime', mediaType: 'application/x.ips.gzip.aes256.v1-0', data: compressed })"))
                  // 3) Write the binary payload
                  reader.write(msg).await()
                } else {
                  reader.write(textArea.value).await()
                }
                Toast.success("Data written to NFC tag!")
              } catch (e: dynamic) {
                Toast.danger("Failed to write to NFC: ${e.message}")
              }
            }
          }
        }
      }

  init {
    padding = 5.px

    add(
        VPanel(spacing = 5) {
          div(className = "container mt-5") {
            div(className = "row") {
              div(className = "col") {
                add(header)
                div(className = "row") {
                  add(patientSelect)
                  add(modeSelect)
                  add(compressionCheck)
                }
                div(className = "text-area") { add(textArea) }
                div(className = "button-container mt-3") {
                  add(downloadButton)
                  add(nfcButton)
                  add(binarySwitch)
                }
              }
            }
          }
        })

    // Bindings i.e. so the UI updates when the model changes
    bind(observableState = Model.selectedIps, removeChildren = false, runImmediately = true) {
        selected ->
      val has = selected != null
      modeSelect.disabled = !has
      downloadButton.disabled = !has
      nfcButton.disabled = !has
      if (has) {
        patientSelect.value = selected!!.id.toString()
        fetchData(selected.id)
      } else {
        patientSelect.value = null
        textArea.value = ""
      }
    }

    bind(observableState = Model.ipsRecords, removeChildren = false, runImmediately = true) {
        records ->
      // Map each IPSModel to value/label pairs for the dropdown
      val opts = records.map { it.id.toString() to "${it.patientGiven} ${it.patientName}" }
      patientSelect.options = opts
      patientSelect.disabled = opts.isEmpty()
    }
    // Update the active patient when the dropdown changes - note this is pan-App
    patientSelect.onEvent {
      change = {
        patientSelect.value?.let { idStr ->
          val record = Model.ipsRecords.find { it.id.toString() == idStr }
          if (Model.selectedIps.value?.id != record?.id) {
            Model.selectedIps.value = record
          }
        }
      }
    }
    compressionCheck.onEvent {
      change = {
        // only refresh if there’s a current patient
        Model.selectedIps.value?.id?.let { fetchData(it) }
      }
    }
  }

  private fun fetchData(selectedId: Int?) {
    if (selectedId == null) {
      textArea.value = ""
      return
    }
    scope.launch {
      var rawJson = Model.generateUnifiedBundle(selectedId)
      if (compressionCheck.value) {
        val encrypted = Model.encryptTextGzip(rawJson)
        /*  encrypted is a bespoke dataclass:

        data class EncryptedPayloadDTO(
            val encryptedData: String,
            val iv: String,
            val mac: String,
            val key: String
        )
        */

        // convert to JSON
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        rawJson = json.encodeToString(EncryptedPayloadDTO.serializer(), encrypted)
      }
      val pretty =
          try {
            val json = kotlinx.serialization.json.Json { prettyPrint = true }
            val elem = json.decodeFromString<kotlinx.serialization.json.JsonElement>(rawJson)
            json.encodeToString(elem)
          } catch (e: Exception) {
            rawJson
          }
      // Fallback to just showing the JSON
      textArea.value = pretty
      header.content = "API GET - IPS Data: ${textArea.value?.length}"
    }
  }
}
