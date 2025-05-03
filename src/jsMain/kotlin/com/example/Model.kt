package com.example

import dev.kilua.rpc.getService
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.syncWithList
import kotlinx.browser.window

enum class Page {
  LIST,
  ABOUT
}

fun ByteArray.encodeBase64(): String {
    return window.btoa(this.joinToString("") { it.toInt().toChar().toString() })
}

fun ByteArray.toBase64(): String {
    val binary = this.joinToString("") { it.toInt().toChar().toString() }
    return js("btoa(binary)") as String
}

fun ByteArray.base64EncodeSafe(): String {
    val binary = this.joinToString("") { it.toInt().toChar().toString() }
    return js("btoa(binary)") as String
}

fun String.decodeBase64(): ByteArray {
    val decoded = window.atob(this)
    return decoded.encodeToByteArray()
}

object Model {
  private val ipsService = getService<IIPSService>()

  // suspend fun getIPSList(): List<IPSModel> = ipsService.getIPSList()
  // new observable list for IPS
  val ipsRecords: ObservableList<IPSModel> = observableListOf()
  val selectedIps: ObservableValue<IPSModel?> = ObservableValue(null)

  val currentPage = ObservableValue(Page.ABOUT)

  suspend fun getIPSList() {
    val list = ipsService.getIPSList()
    // console.log("IPS list: $list")
    ipsRecords.syncWithList(list)
    selectedIps.value = null
  }

  suspend fun findByLastName(surname: String) {
    val list = ipsService.findByLastName(surname)
    ipsRecords.syncWithList(list)
    // Make the selected item the first one in the list if not empty
    if (list.isNotEmpty()) {
      selectedIps.value = list[0]
    } else {
      selectedIps.value = null
    }
  }

  suspend fun generateUnifiedBundle(id: Int?): String {
    return ipsService.generateUnifiedBundle(id)
  }

  suspend fun convertBundleToSchema(bundleJson: String): String {
    return ipsService.convertBundleToSchema(bundleJson)
  }

  suspend fun encryptText(data: String, useBase64: Boolean = false): EncryptedPayloadDTO {
    return ipsService.encryptText(data, useBase64)
}

suspend fun decryptText(payload: EncryptedPayloadDTO, useBase64: Boolean = false): String {
    return ipsService.decryptText(
        encryptedData = payload.encryptedData,
        iv = payload.iv,
        mac = payload.mac,
        useBase64 = useBase64
    )
}

suspend fun encryptBinary(data: ByteArray): ByteArray {
    val encoded = data.encodeBase64()
    val response = ipsService.encryptBinary(encoded)
    return response.data.decodeBase64()
}

suspend fun decryptBinary(encrypted: ByteArray): ByteArray {
    val encoded = encrypted.toBase64()
    val response = ipsService.decryptBinary(encoded)
    return response.data.decodeBase64()
}
}
