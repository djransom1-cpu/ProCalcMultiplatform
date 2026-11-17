import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    // Desktop (Windows, Mac, Linux): same app in a window, with the split home/page layout
    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.2")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.2")
            implementation("org.jetbrains.compose.material:material:1.10.2")
            implementation("org.jetbrains.compose.ui:ui:1.10.2")
            implementation("org.jetbrains.compose.components:components-resources:1.10.2")
            implementation("org.jetbrains.compose.components:components-ui-tooling-preview:1.10.2")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.kotlinx.serialization.json)
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation("org.jetbrains.compose.ui:ui-tooling:1.10.2")
            implementation(libs.firebase.analytics)
        }
    }
}

android {
    namespace = "com.djran.constructioncalculator"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.djran.constructioncalculator.multi"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

firebaseAppDistribution {
    appId = "1:622603052731:android:c036b1e87656e0b784eb9e"
    groups = "testers"
}

compose.desktop {
    application {
        mainClass = "com.djran.constructioncalculator.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "ProCalc"
            // Windows only installs over the old copy when the version goes up, so every
            // GitHub build gets a new one: 1.1.<build number> (1.1.0 for local builds)
            packageVersion = "1.1.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
            description = "ProCalc construction calculator"
            vendor = "Construction App Builders LLC"
            // The app's icon (the phone app's purple tile with the white roof and calculator marks) for the
            // installer, Start menu, desktop shortcut and the .exe; the window loads icon.png itself.
            linux { iconFile.set(project.file("icon.png")) }
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "ProCalc"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "6f1d2c1e-6a43-4f0e-9b7a-2f5d8a8e4c31"
            }
        }
    }
}
