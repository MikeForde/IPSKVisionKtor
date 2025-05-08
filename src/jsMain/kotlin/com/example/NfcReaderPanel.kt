package com.example

import io.kvision.form.text.TextArea
import io.kvision.html.Button
import io.kvision.panel.SimplePanel
import io.kvision.toast.Toast
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
  private var nfcReader: NDEFReader? = null
  private var scanAbortController: dynamic = null
  private val scope = MainScope()
  private val cardInfoArea =
      TextArea(rows = 3).apply {
        this.readonly = true
        input.addCssClass("card-info-area")
      }
  private val payloadArea = TextArea(rows = 15).apply { this.readonly = true }
  private val readButton =
      Button("Read from NFC").apply { onClick { scope.launch { readFromNfc() } } }

  private var rawPayload: String = ""

  init {
    add(readButton)
    add(Button("Import").apply { onClick { scope.launch { importPayload() } } })

    add(Button("Convert to Schema").apply { onClick { scope.launch { convertOnly() } } })
    // add(
    //     Button("Debug").apply {
    //       size = ButtonSize.SMALL
    //       onClick { scope.launch { debugTest() } }
    //     })

    add(cardInfoArea)
    add(payloadArea)
  }

  private suspend fun readFromNfc() {
    if (js("typeof NDEFReader === 'undefined'") as Boolean) {
      Toast.warning("Web NFC is not supported")
      return
    }

    readButton.text = "Waiting..."
    readButton.disabled = true

    scanAbortController?.abort()

    try {
      val reader = nfcReader ?: NDEFReader().also { nfcReader = it }

      // Clear previous handlers to avoid duplicates
      reader.onreading = null
      reader.onreadingerror = null

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

        readButton.text = "Read from NFC"
        readButton.disabled = false
        scanAbortController?.abort()
      }

      reader.onreadingerror = {
        Toast.danger("Cannot read data from the NFC tag (onreadingerror triggered)")

        reader.onreading = null
        reader.onreadingerror = null

        readButton.text = "Read from NFC"
        readButton.disabled = false
        scanAbortController?.abort()
      }

      // Important: wrap scan in try-catch
      val controller = js("new AbortController()")
      scanAbortController = controller
      val options = js("({ signal: controller.signal })")
      reader.scan(options).await()
    } catch (e: Throwable) {
      Toast.danger("Failed to start NFC scan: ${e.message}")
      readButton.text = "Read from NFC"
      readButton.disabled = false
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
              // val uint8Decrypted = js("new Uint8Array(decryptedBytes)")
              val decoder = js("new TextDecoder('utf-8')")
              decoder.decode(decryptedBytes)
            }
            // decodeGzip will return a UTF-8 string
            "application/x.ips.gzip.v1-0" -> {
              // Model.decodeGzip takes ByteArray and returns the decompressed String
              Model.decodeGzip(byteArray)
            }

            // decryptAndDecompress
            "application/x.ips.gzip.aes256.v1-0" -> {
              Model.decryptAndDecompress(byteArray)
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
    val trimmed = rawPayload.trim()
    if (!trimmed.startsWith("{") ||
        !trimmed.contains("resourceType") ||
        !trimmed.contains("Bundle")) {
      Toast.danger("Only JSON FHIR Bundles can be currently imported via this method.")
      return
    }

    try {
      val rawJson = Model.addBundleToDatabase(trimmed)
      val prettyJson = fnPrettyJson(rawJson)
      payloadArea.value = prettyJson
      Toast.success("Conversion successful")
    } catch (e: Throwable) {
      Toast.danger("Conversion failed: ${e.message}")
    }
  }

  private suspend fun convertOnly() {
    val trimmed = rawPayload.trim()
    if (!trimmed.startsWith("{") ||
        !trimmed.contains("resourceType") ||
        !trimmed.contains("Bundle")) {
      Toast.danger("Only JSON FHIR Bundles can be currently converted via this method.")
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

  // private suspend fun debugTest() {
  //   try {
  //     // Example UTF-8 test string
  //     val testString = "Hello from NFC test"
  //     val byteArray = testString.encodeToByteArray()

  //     // Send to backend and receive echo
  //     val responseBytes = Model.testBinary(byteArray)

  //     // Decode response back to string for display
  //     val decoder = js("new TextDecoder('utf-8')")
  //     val text = decoder.decode(js("new Uint8Array(responseBytes)"))

  //     payloadArea.value = text
  //     Toast.success("CBOR round-trip success!")
  //   } catch (e: Throwable) {
  //     Toast.danger("Debug test failed: ${e.message}")
  //   }
  // }
}
