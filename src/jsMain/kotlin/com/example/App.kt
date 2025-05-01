package com.example

import io.kvision.Application
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.FontAwesomeModule
import io.kvision.Hot
import io.kvision.i18n.DefaultI18nManager
import io.kvision.i18n.I18n
import io.kvision.navbar.navbar
import io.kvision.navbar.NavbarExpand
import io.kvision.navbar.nav
import io.kvision.navbar.navLink
import io.kvision.panel.root
import io.kvision.panel.tabPanel
import io.kvision.panel.TabPanel
import io.kvision.remote.registerRemoteTypes
import io.kvision.startApplication
import io.kvision.routing.Routing
import io.kvision.state.bind
import io.kvision.utils.perc
import io.kvision.utils.useModule
import io.kvision.utils.vh
import io.kvision.core.onEvent
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

val AppScope = CoroutineScope(window.asCoroutineDispatcher())

@JsModule("/kotlin/modules/css/kvapp.css")
external val kvappCss: dynamic

@JsModule("/kotlin/modules/i18n/messages-en.json")
external val messagesEn: dynamic

@JsModule("/kotlin/modules/i18n/messages-pl.json")
external val messagesPl: dynamic

class App : Application() {
    init {
        useModule(kvappCss)
    }

    override fun start() {
        // i18n + RPC setup
        I18n.manager = DefaultI18nManager(mapOf("en" to messagesEn, "pl" to messagesPl))

        root("kvapp") {
             // TabPanel creates a tabbed layout and handles view‐swapping automatically
             tabPanel {
                 // First tab: your master/detail IPS list
                 addTab("IPS SKK", IPSHomePanel, icon = "fas fa-home")
                 // Second tab: the static About page
                 addTab("About", AboutPanel, icon = "fas fa-info-circle")
             } 
             // Load data immediately for the List tab
             AppScope.launch { Model.getIPSList() }
         }
    }
}

fun main() {
    registerRemoteTypes()
    startApplication(
        ::App,
        js("import.meta.webpackHot").unsafeCast<Hot?>(),
        BootstrapModule,
        FontAwesomeModule,
        CoreModule
    )
}
