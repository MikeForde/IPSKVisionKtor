@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.example

import kotlin.js.Promise
import org.w3c.dom.events.Event

external class NDEFReader {
  fun scan(): Promise<Unit>

  fun write(message: dynamic): Promise<Unit>

  var onreading: ((event: NDEFReadingEvent) -> Unit)?
  var onreadingerror: ((event: Event) -> Unit)?
}

external interface NDEFMessage {
  val records: Array<NDEFRecord>
}

external interface NDEFReadingEvent {
  val serialNumber: String
  val message: NDEFMessage
}

external interface NDEFRecord {
  val recordType: String
  val mediaType: String?
  val data: dynamic
  val encoding: String?
}
