import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  val kotlinVersion: String by System.getProperties()
  kotlin("plugin.serialization") version kotlinVersion
  kotlin("multiplatform") version kotlinVersion
  val kspVersion: String by System.getProperties()
  id("com.google.devtools.ksp") version kspVersion
  val kiluaRpcVersion: String by System.getProperties()
  id("dev.kilua.rpc") version kiluaRpcVersion
  val kvisionVersion: String by System.getProperties()
  id("io.kvision") version kvisionVersion
  id("com.ncorti.ktfmt.gradle") version "0.22.0"
}

version = "1.0.0-SNAPSHOT"

group = "com.example"

repositories {
  mavenCentral()
  mavenLocal()
}

// Versions
val kvisionVersion: String by System.getProperties()
val kiluaRpcVersion: String by System.getProperties()
val ktorVersion: String by project
val exposedVersion: String by project
val hikariVersion: String by project
val h2Version: String by project
val pgsqlVersion: String by project
val kweryVersion: String by project
val logbackVersion: String by project
val commonsCodecVersion: String by project
val jdbcNamedParametersVersion: String by project

val mainClassName = "io.ktor.server.netty.EngineMain"

kotlin {
  jvmToolchain(21)
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions { freeCompilerArgs = listOf("-Xjsr305=strict") }
    @OptIn(ExperimentalKotlinGradlePluginApi::class) mainRun { mainClass.set(mainClassName) }
  }
  js(IR) {
    browser {
      // useCommonJs()
      commonWebpackConfig {
        outputFileName = "main.bundle.js"
        sourceMaps = false
      }
      testTask { useKarma { useChromeHeadless() } }
    }
    binaries.executable()
    compilerOptions { target.set("es2015") }
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("dev.kilua:kilua-rpc-ktor:$kiluaRpcVersion")
        implementation("io.kvision:kvision-common-remote:$kvisionVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("reflect"))
        implementation("io.ktor:ktor-server-netty:$ktorVersion")
        implementation("io.ktor:ktor-server-auth:$ktorVersion")
        implementation("io.ktor:ktor-server-compression:$ktorVersion")
        implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
        implementation("io.ktor:ktor-server-compression:$ktorVersion")
        implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
        implementation("ch.qos.logback:logback-classic:$logbackVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("com.h2database:h2:$h2Version")
        implementation("org.jetbrains.exposed:exposed:$exposedVersion")
        implementation("org.postgresql:postgresql:$pgsqlVersion")
        implementation("mysql:mysql-connector-java:8.0.33")
        implementation("com.zaxxer:HikariCP:$hikariVersion")
        implementation("commons-codec:commons-codec:$commonsCodecVersion")
        implementation("com.axiomalaska:jdbc-named-parameters:$jdbcNamedParametersVersion")
        implementation("com.github.andrewoma.kwery:core:$kweryVersion")
        implementation(kotlin("stdlib"))
        implementation("io.ktor:ktor-server-cors:$ktorVersion")
        implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktorVersion")
        implementation("com.google.zxing:core:3.5.2")
        implementation("com.google.zxing:javase:3.5.2")
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(kotlin("test-junit"))
      }
    }
    val jsMain by getting {
      dependencies {
        implementation("io.kvision:kvision:$kvisionVersion")
        implementation("io.kvision:kvision-bootstrap:$kvisionVersion")
        implementation("io.kvision:kvision-state:$kvisionVersion")
        implementation("io.kvision:kvision-fontawesome:$kvisionVersion")
        implementation("io.kvision:kvision-i18n:$kvisionVersion")
        implementation("io.kvision:kvision-rest:$kvisionVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
        implementation("io.kvision:kvision-routing-navigo:$kvisionVersion")
        implementation("io.kvision:kvision-toastify:$kvisionVersion")
        implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktorVersion")
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
        implementation("io.kvision:kvision-testutils:$kvisionVersion")
      }
    }
  }
}
