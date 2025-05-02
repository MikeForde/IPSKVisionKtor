package com.example

import io.kvision.form.text.textAreaInput
import io.kvision.html.*
import io.kvision.panel.SimplePanel
import io.kvision.state.bind
import io.kvision.utils.px
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object DataFormatPanel : SimplePanel() {
  private val scope = MainScope()
  private val textArea = textAreaInput(rows = 20) { height = 800.px }

  init {
    padding = 25.px

    div(className = "container mt-5") {
      div(className = "row") {
        div(className = "col") {
          // This header is static—it won't get cleared
          h3("API GET - IPS Data")

          // Create a dedicated panel for the dynamic content
          val resultPanel = SimplePanel()
          add(resultPanel)

          // Only the resultPanel gets cleared & rebuilt on each change
          resultPanel.bind(Model.selectedIps) { selected ->
            resultPanel.removeAll() // ensure it's empty
            if (selected != null) {
              resultPanel.add(textArea)
              scope.launch {
                val rawJson = Model.generateUnifiedBundle(selected.id)
                val prettyJson =
                    try {
                      val json = Json { prettyPrint = true }
                      val element = json.decodeFromString<JsonElement>(rawJson)
                      json.encodeToString(element)
                    } catch (e: Exception) {
                      rawJson
                    }
                textArea.value = prettyJson
              }
            }
          }
        }
      }
    }
  }
}
