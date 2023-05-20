import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.compose")
  id("dev.hydraulic.conveyor") version "1.4"
}

group = "com.trevorsstone"

version = "1.0"

repositories {
  mavenCentral()
  google()
  mavenLocal()
  maven { url = uri("https://jitpack.io") }
  maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

kotlin {
  jvm {
    compilations.all { kotlinOptions.jvmTarget = "17" }
    withJava()
  }

  sourceSets {
    val jvmMain: KotlinSourceSet by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(compose.desktop.currentOs)
        implementation("com.github.sarxos:webcam-capture:0.3.13-eduramiba-SNAPSHOT")
        implementation("com.github.eduramiba:webcam-capture-driver-native:1.0.0-SNAPSHOT")
      }
    }
  }
}

dependencies {
  // Use the configurations created by the Conveyor plugin to tell Gradle/Conveyor where to find the
  // artifacts for each platform.
  linuxAmd64(compose.desktop.linux_x64)
  macAmd64(compose.desktop.macos_x64)
  macAarch64(compose.desktop.macos_arm64)
  windowsAmd64(compose.desktop.windows_x64)
}

compose.desktop { application { mainClass = "MainKt" } }

// region Work around temporary Compose bugs.
configurations.all {
  attributes {
    // https://github.com/JetBrains/compose-jb/issues/1404#issuecomment-1146894731
    attribute(Attribute.of("ui", String::class.java), "awt")
  }
}

dependencies {
  // Force override the Kotlin stdlib version used by Compose to 1.7 in the machine specific
  // configurations, as otherwise we can end up
  // with a mix of 1.6 and 1.7 on our classpath. This is the same logic as is applied to the regular
  // Compose configurations normally.
  val v = "1.7.10"
  for (m in setOf("linuxAmd64", "macAmd64", "macAarch64", "windowsAmd64")) {
    m("org.jetbrains.kotlin:kotlin-stdlib:$v")
    m("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$v")
    m("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$v")
  }
}// endregion
