package com.example

import dev.kilua.rpc.getService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.syncWithList

enum class Page {
  LIST,
  ABOUT
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
        useBase64 = useBase64)
  }

  // suspend fun decryptBinaryViaHttp(encrypted: ByteArray): ByteArray {
  //   val response: HttpResponse =
  //       cborClient.post("http://localhost:8081/api/decryptBinaryCbor") {
  //         contentType(ContentType.Application.Cbor)
  //         setBody(encrypted)
  //       }
  //   return response.body()
  // }

  suspend fun decryptBinaryViaHttp(encrypted: ByteArray): ByteArray {
    val response: HttpResponse =
        restClient.post("http://localhost:8080/api/decryptBinary") {
          contentType(ContentType.Application.OctetStream)
          setBody(encrypted)
        }
    return response.body()
  }

  // suspend fun testBinary(data: ByteArray): ByteArray {
  //   val response: HttpResponse =
  //       cborClient.post("http://localhost:8081/api/TestCBor") {
  //         contentType(ContentType.Application.Cbor)
  //         setBody(data)
  //       }
  //   return response.body()
  // }

  suspend fun testBinary(data: ByteArray): ByteArray {
    val response: HttpResponse =
        restClient.post("http://localhost:8080/api/TestCBor") {
          contentType(ContentType.Application.OctetStream)
          setBody(data)
        }
    return response.body()
  }
}
