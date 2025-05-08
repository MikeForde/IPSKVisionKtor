package com.example

import io.kvision.core.onEvent
import io.kvision.form.check.CheckBox
import io.kvision.form.select.SelectInput
import io.kvision.html.*
import io.kvision.panel.SimplePanel
import io.kvision.panel.VPanel
import io.kvision.state.bind
import io.kvision.toast.Toast
import io.kvision.utils.px
import kotlin.js.Date
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

@JsName("atob") external fun atob(encoded: String): String

// external class Uint8Array(length: Int) {
//   constructor(array: Array<Byte>)

//   val buffer: dynamic

//   operator fun set(index: Int, value: Byte)

//   val length: Int
// }

// @JsName("Uint8Array")
// external class Uint8Array(length: Int) {
//     constructor(array: Array<Short>)
//     constructor(array: ByteArray)
//     val buffer: dynamic
// }

object QRPanel : SimplePanel() {
  private val scope = MainScope()
  private val header = h3("QR Code - IPS Data: 0")
  private val qrImage = Image(src = "", alt = "QR Code", className = "img-thumbnail")
  private val compressionCheck = CheckBox(label = "Gzip + Encrypt (AES256 base64)")
  private val modeSelect =
      SelectInput(
          options = listOf("ipsurl" to "IPS URL", "ipsunified" to "IPS Unified JSON Bundle"),
          value = "ipsurl")
  private val patientSelect =
      SelectInput(options = listOf(), value = null).apply { disabled = true }
  private val downloadButton =
      button("Download QR PNG") {
        disabled = true
        onClick {
          val base64 = qrImage.src?.removePrefix("data:image/png;base64,") ?: return@onClick
          // val binary = atob(base64)
          // val u8arr = Uint8Array(binary.length)
          // for (i in binary.indices) u8arr[i] = binary[i].code.toByte()
          // val binary = window.atob(base64)
          // val byteArray = binary.encodeToByteArray()
          // val u8arr = Uint8Array(byteArray.size)
          // for (i in byteArray.indices) {
          //     u8arr[i] = byteArray[i].toInt()
          // }
          // val binary = atob(base64)
          // val byteArray = binary.encodeToByteArray()
          // val u8arr = Uint8Array(byteArray)
          val binaryStr = atob(base64)

          val byteArray = ByteArray(binaryStr.length) { i -> binaryStr[i].code.toByte() }

          // Uint8Array accepts ByteArray directly
          val u8arr = Uint8Array(byteArray.toTypedArray())

          val now = Date()
          val pad: (Int) -> String = { it.toString().padStart(2, '0') }
          val yyyymmdd = "${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}"

          Model.selectedIps.value?.let { selected ->
            val fam = selected.patientName.uppercase().replace(Regex("[^A-Z0-9]"), "_").trim('_')
            val giv = selected.patientGiven.uppercase().replace(Regex("[^A-Z0-9]"), "_").trim('_')
            val last6 = selected.packageUUID.takeLast(6)
            val fname = "$yyyymmdd-${fam}_${giv}_${last6}_${modeSelect.value}.png"
            val blob = Blob(arrayOf(u8arr.buffer), BlobPropertyBag("image/png"))
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
                div(className = "text-area") { add(qrImage) }
                add(downloadButton)
              }
            }
          }
        })

    bind(Model.selectedIps, removeChildren = false, runImmediately = true) { selected ->
      val has = selected != null
      downloadButton.disabled = !has
      modeSelect.disabled = !has
      if (has) {
        patientSelect.value = selected!!.id.toString()
        fetchQr(selected.id)
      } else {
        qrImage.src = ""
        header.content = "QR Code - IPS Data: 0"
      }
    }

    bind(Model.ipsRecords, removeChildren = false, runImmediately = true) { records ->
      val opts = records.map { it.id.toString() to "${it.patientGiven} ${it.patientName}" }
      patientSelect.options = opts
      patientSelect.disabled = opts.isEmpty()
    }

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

    modeSelect.onEvent {
      change = {
        val selectedId = Model.selectedIps.value?.id
        if (selectedId != null) {
          fetchQr(selectedId)
        }
      }
    }

    compressionCheck.onEvent { change = { Model.selectedIps.value?.id?.let { fetchQr(it) } } }
  }

  private fun fetchQr(selectedId: Int?) {
    scope.launch {
      val selected = Model.ipsRecords.find { it.id == selectedId } ?: return@launch
      val mode = modeSelect.value

      val qrPayload =
          when (mode) {
            "ipsurl" -> {
              val baseUrl = window.location.origin
              "$baseUrl/api/ipsRecord?id=${selected.packageUUID}"
            }
            else -> {
              var rawJson = Model.generateUnifiedBundle(selectedId)
              if (compressionCheck.value) {
                val encrypted = Model.encryptTextGzip(rawJson)
                val json = kotlinx.serialization.json.Json { prettyPrint = true }
                rawJson = json.encodeToString(EncryptedPayloadDTO.serializer(), encrypted)
              }
              val byteSize = rawJson.encodeToByteArray().size
              if (byteSize > 3000) {
                Toast.danger("QR code too large: $byteSize bytes (max 3000)")
                qrImage.src = ""
                header.content = "QR Code - IPS Data: $byteSize (TOO LARGE)"
                return@launch
              } else {
                rawJson
              }
            }
          } ?: return@launch

      val qrPng = Model.generateQrCode(qrPayload)
      qrImage.src = qrPng
      header.content = "QR Code - IPS Data: ${qrPayload.encodeToByteArray().size}"
    }
  }
}
