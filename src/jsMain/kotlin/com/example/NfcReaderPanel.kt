package com.example

import io.kvision.form.text.TextArea
import io.kvision.html.Button
import io.kvision.html.ButtonSize
import io.kvision.panel.SimplePanel
import io.kvision.toast.Toast
import io.kvision.utils.px
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

fun fnPrettyJson(raw: String): String {
  return try {
    val json = Json { prettyPrint = true }
    val element: JsonElement = json.decodeFromString(raw)
    json.encodeToString(element)
  } catch (_: Exception) {
    raw
  }
}

fun arrayBufferToByteArray(buffer: dynamic): ByteArray {
  val uint8Array = js("new Uint8Array(buffer)")
  val byteArray = ByteArray(uint8Array.length as Int)
  for (i in 0 until byteArray.size) {
    byteArray[i] = uint8Array[i] as Byte
  }
  return byteArray
}

object NfcReaderPanel : SimplePanel() {
  private val scope = MainScope()
  private val cardInfoArea = TextArea(rows = 3).apply { this.readonly = true }
  private val payloadArea = TextArea(rows = 15).apply { this.readonly = true }

  private var rawPayload: String = ""

  init {
    padding = 30.px
    add(
        Button("Read from NFC").apply {
          size = ButtonSize.SMALL
          onClick { scope.launch { readFromNfc() } }
        })
    add(
        Button("Import").apply {
          size = ButtonSize.SMALL
          onClick { scope.launch { importPayload() } }
        })

    add(
        Button("Convert to Schema").apply {
          size = ButtonSize.SMALL
          onClick { scope.launch { convertOnly() } }
        })
    add(
        Button("Debug").apply {
          size = ButtonSize.SMALL
          onClick { scope.launch { debugTest() } }
        })

    add(cardInfoArea)
    add(payloadArea)
  }

  private suspend fun readFromNfc() {
    if (js("typeof NDEFReader === 'undefined'") as Boolean) {
      Toast.warning("Web NFC is not supported")
      return
    }

    try {
      val reader = NDEFReader()

      reader.onreading = { event ->
        val record = event.message.records.getOrNull(0)

        if (record == null) {
          Toast.danger("No records found on the NFC tag.")
        } else {
          val info =
              "UID: ${event.serialNumber}\nRecords: ${event.message.records.size}" +
                  (record?.mediaType?.let { "\nMIME: $it" } ?: "")
          cardInfoArea.value = info

          val extracted =
              when {
                record?.recordType == "text" -> {
                  val decoder = js("new TextDecoder(record.encoding || 'utf-8')")
                  decoder.decode(record.data)
                }
                record?.recordType == "url" -> {
                  val decoder = js("new TextDecoder()")
                  "URL: " + decoder.decode(record.data)
                }
                record?.recordType == "mime" -> {
                  var result = ""
                  scope.launch {
                    result = processBinaryRecord(record)
                    payloadArea.value = result
                    rawPayload = result
                  }
                  ""
                }
                else -> {
                  val bytes = js("Array.from(new Uint8Array(record.data))")
                  (bytes as Array<dynamic>).joinToString(" ") { byte ->
                    (byte as Int).toString(16).padStart(2, '0')
                  }
                }
              }

          payloadArea.value = extracted
          rawPayload = extracted
          Toast.success("NFC tag read successfully!")
        }
      }

      reader.onreadingerror = {
        Toast.danger("Cannot read data from the NFC tag (onreadingerror triggered)")
      }

      // Important: wrap scan in try-catch
      reader.scan().await()
    } catch (e: Throwable) {
      Toast.danger("Failed to start NFC scan: ${e.message}")
    }
  }

  private suspend fun processBinaryRecord(record: NDEFRecord): String {
    val buffer: dynamic =
        if (js("record.data instanceof ArrayBuffer") as Boolean) {
          record.data
        } else {
          record.data.buffer
        }

    val mimeType = record.mediaType ?: "application/octet-stream"

    return try {
      // Convert ArrayBuffer to ByteArray
      val uint8Array = js("new Uint8Array(buffer)")
      val byteArray = ByteArray(uint8Array.length as Int)
      for (i in byteArray.indices) {
        byteArray[i] = uint8Array[i] as Byte
      }

      val decodedText =
          when (mimeType) {
            "application/x.ips.v1-0" -> {
              val decoder = js("new TextDecoder('utf-8')")
              decoder.decode(buffer)
            }

            "application/x.ips.aes256.v1-0" -> {
              val decryptedBytes = Model.decryptBinaryViaHttp(byteArray)
              val uint8Decrypted = js("new Uint8Array(decryptedBytes)")
              val decoder = js("new TextDecoder('utf-8')")
              decoder.decode(decryptedBytes)
            }

            else -> {
              Toast.warning("Unknown MIME type: $mimeType — showing as UTF-8 text.")
              val decoder = js("new TextDecoder('utf-8')")
              decoder.decode(buffer)
            }
          }

      fnPrettyJson(decodedText)
    } catch (e: Throwable) {
      Toast.danger("Error processing binary: ${e.message}")
      "Error decoding binary: ${e.message}"
    }
  }

  private suspend fun importPayload() {
    if (rawPayload.isBlank()) return

    val (endpoint, payloadToSend, contentType) =
        when {
          rawPayload.trim().startsWith("{") &&
              "resourceType" in rawPayload &&
              "Bundle" in rawPayload ->
              Triple("/ipsbundle", Json.parseToJsonElement(rawPayload), "application/json")
          rawPayload.startsWith("MSH") -> Triple("/ipsfromhl72x", rawPayload, "text/plain")
          rawPayload.startsWith("H9") -> Triple("/ipsfrombeer", rawPayload, "text/plain")
          else -> {
            Toast.danger("Unrecognized IPS format")
            return
          }
        }

    try {
      val headers = js("({ 'Content-Type': contentType })")
      headers["Content-Type"] = contentType

      val body =
          if (contentType == "application/json") JSON.stringify(payloadToSend) else payloadToSend

      val response =
          window.fetch(endpoint, js("({ method: 'POST', headers: headers, body: body })")).await()

      Toast.success("Import successful")
    } catch (e: Throwable) {
      Toast.danger("Import failed: ${e.message}")
    }
  }

  private suspend fun convertOnly() {
    val trimmed = rawPayload.trim()
    if (!trimmed.startsWith("{") ||
        !trimmed.contains("resourceType") ||
        !trimmed.contains("Bundle")) {
      Toast.danger("Only JSON FHIR Bundles can be converted via this method.")
      return
    }

    try {
      val rawJson = Model.convertBundleToSchema(trimmed)
      val prettyJson = fnPrettyJson(rawJson)
      payloadArea.value = prettyJson
      Toast.success("Conversion successful")
    } catch (e: Throwable) {
      Toast.danger("Conversion failed: ${e.message}")
    }
  }

  private suspend fun debugTest() {
    try {
      // Example UTF-8 test string
      val testString = "Hello from NFC test"
      val byteArray = testString.encodeToByteArray()

      // Send to backend and receive echo
      val responseBytes = Model.testBinary(byteArray)

      // Decode response back to string for display
      val decoder = js("new TextDecoder('utf-8')")
      val text = decoder.decode(js("new Uint8Array(responseBytes)"))

      payloadArea.value = text
      Toast.success("CBOR round-trip success!")
    } catch (e: Throwable) {
      Toast.danger("Debug test failed: ${e.message}")
    }
  }
}
