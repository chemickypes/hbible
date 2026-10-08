import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// DB pre-costruito incluso nell'app (files/bible.db): generato dai JSON della
// pipeline (src/commonMain/composeResources/files, sorgente anche per il CMS e
// learn-hebrew) e dall'ultimo schema Room esportato. Rigenerato solo quando
// cambiano JSON, schema o script; non versionato (~90 MB).
val bundledDatabaseDir = layout.buildDirectory.dir("generated/bundledDatabase")
val buildBundledDatabase by tasks.registering(Exec::class) {
    description = "Genera files/bible.db con tools/build_bible_db.py"
    val script = rootProject.file("tools/build_bible_db.py")
    val output = bundledDatabaseDir.map { it.file("files/bible.db") }
    inputs.files(fileTree("src/commonMain/composeResources/files") { include("*.json") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("schemas").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(script).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(output)
    commandLine("python3", "-I", script.absolutePath, "--out", output.get().asFile.absolutePath)
}

compose.resources {
    generateResClass = always
    packageOfResClass = "com.hooloovoochimico.kmp.hbible.resources"
    // Le risorse dell'app vengono SOLO dalla cartella generata (bible.db): i JSON
    // in src/commonMain/composeResources restano sorgente della pipeline e non
    // finiscono più nell'APK/app iOS.
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = buildBundledDatabase.map { bundledDatabaseDir.get() },
    )
}

// Schema Room esportato per versione (composeApp/schemas/<db>/<versione>.json):
// base dei test di migrazione. Le versioni 8–13 sono state rigenerate dai commit
// storici (PLAN §12, 2026-10-07); 10 e 14 non sono mai state committate.
room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iosX64 (simulatore Intel) rimosso: CM 1.12 non lo pubblica più e l'utente
    // usa solo Apple Silicon (NEXT_STEPS punto 3a, 2026-10-08).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.ui)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.androidx.room.runtime)
            api(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        // Test strumentali (device/emulatore): migrazioni Room con MigrationTestHelper.
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.room.testing)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.junit)
        }
    }
}

android {
    namespace = "com.hooloovoochimico.kmp.hbible"
    // 37 (SDK "37.0"): richiesto da CM 1.12.1/androidx 1.12.1, navigation 2.10.0,
    // lifecycle 2.11.0, okhttp 5.5.0 (vedi libs.versions.toml).
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hooloovoochimico.kmp.hbible"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Firma di release da keystore.properties (root, gitignored) con chiavi
    // storeFile/storePassword/keyAlias/keyPassword. Senza file la release è
    // firmata con la chiave di debug: installabile per provarla in locale,
    // NON pubblicabile (il Play Store rifiuta APK/AAB firmati in debug).
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseSigning =
        if (keystoreFile.exists()) {
            val props = Properties().apply { keystoreFile.inputStream().use { load(it) } }
            signingConfigs.create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        } else {
            signingConfigs.getByName("debug")
        }

    buildTypes {
        getByName("debug") {
            // HTTP in chiaro solo in debug: CMS di sviluppo su LAN/emulatore (10.0.2.2).
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
            // In release CMS e provider AI solo via HTTPS.
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// KSP per-target (la configurazione bare `ksp` è deprecata nei progetti KMP).
// Room compiler processa commonMain su ogni target.
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
