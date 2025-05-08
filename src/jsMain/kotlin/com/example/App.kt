package com.example

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.cbor.*
import io.kvision.Application
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.FontAwesomeModule
import io.kvision.Hot
import io.kvision.ToastifyModule
import io.kvision.core.AlignItems
import io.kvision.core.BsBgColor
import io.kvision.core.onEvent
import io.kvision.form.text.text
import io.kvision.html.InputType
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.i18n.DefaultI18nManager
import io.kvision.i18n.I18n
import io.kvision.i18n.I18n.tr
import io.kvision.navbar.NavbarColor
import io.kvision.navbar.NavbarExpand
import io.kvision.navbar.NavbarType
import io.kvision.navbar.nav
import io.kvision.navbar.navLink
import io.kvision.navbar.navbar
import io.kvision.panel.SimplePanel
import io.kvision.panel.hPanel
import io.kvision.panel.root
import io.kvision.remote.registerRemoteTypes
import io.kvision.startApplication
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.utils.useModule
import io.kvision.utils.vh
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

val AppScope = CoroutineScope(window.asCoroutineDispatcher())

// val cborClient = HttpClient { install(ContentNegotiation) { cbor() } }

val restClient = HttpClient { expectSuccess = true }

// ServiceManager.register(IIPSService::class, client)

@JsModule("/kotlin/modules/css/kvapp.css") external val kvappCss: dynamic

@JsModule("/kotlin/modules/i18n/messages-en.json") external val messagesEn: dynamic

@JsModule("/kotlin/modules/i18n/messages-pl.json") external val messagesPl: dynamic

private fun collapseNavbar() {
  // Find the first element with class "navbar-collapse"
  document.querySelector(".navbar-collapse")?.let { it.asDynamic().classList.remove("show") }
}

class App : Application() {
  init {
    useModule(kvappCss)
  }

  override fun start(state: Map<String, Any>) {
    // i18n + RPC setup
    I18n.manager = DefaultI18nManager(mapOf("en" to messagesEn, "pl" to messagesPl))

    val content =
        SimplePanel().apply {
          width = 100.perc
          height = 100.vh
          marginTop = 76.px // push below the fixed navbar
        }

    root("kvapp") {
      // Icon?
      // 1) Fixed‐top Navbar
      navbar(
          label = "IPS SK3",
          type = NavbarType.FIXEDTOP,
          bgColor = BsBgColor.PRIMARY,
          nColor = NavbarColor.LIGHT,
          expand = NavbarExpand.SM,
      ) {
        nav {
          navLink("0_6", className = "app_version")
          navLink("Home", icon = "fas fa-home") {
            onEvent {
              click = {
                content.removeAll()
                content.add(IPSHomePanel)
                collapseNavbar()
              }
            }
          }
          navLink("API", icon = "fas fa-file") {
            onEvent {
              click = {
                content.removeAll()
                content.add(DataFormatPanel)
                collapseNavbar()
              }
            }
          }
          navLink("QR", icon = "fas fa-qrcode") {
            onEvent {
              click = {
                content.removeAll()
                content.add(QRPanel)
                collapseNavbar()
              }
            }
          }
          navLink("NFC", icon = "fas fa-tag") {
            onEvent {
              click = {
                content.removeAll()
                content.add(NfcReaderPanel)
                collapseNavbar()
              }
            }
          }
          navLink("About", icon = "fas fa-info-circle") {
            onEvent {
              click = {
                content.removeAll()
                content.add(InfoPanel)
                collapseNavbar()
              }
            }
          }
        }
        nav(rightAlign = true) {
          // search input + button
          hPanel(alignItems = AlignItems.CENTER, spacing = 10) {
            val searchInput = text(InputType.SEARCH) { placeholder = tr("Surname") }
            button(tr("Find")) {
              onEvent {
                click = {
                  Model.selectedIps.value = null
                  AppScope.launch { Model.findByLastName(searchInput.value ?: "") }
                }
              }
            }
          }
        }
      }

      // 2) Content slot, below the navbar
      add(content)

      // 3) Start on Home
      content.add(IPSHomePanel)
    }
  }

  override fun dispose(): Map<String, Any> {
    return mapOf()
  }
}

fun main() {
  registerRemoteTypes()
  startApplication(
      ::App,
      js("import.meta.webpackHot").unsafeCast<Hot?>(),
      BootstrapModule,
      FontAwesomeModule,
      CoreModule,
      ToastifyModule,
  )
}
