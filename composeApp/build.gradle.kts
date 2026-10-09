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
            implementation(libs.compose.components.resources)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kermit)
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
            implementation(libs.ktor.client.mock)
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
        // Id definitivo sul Play Store (DR1): non si può più cambiare dopo il primo caricamento.
        // Il namespace e il package Kotlin restano com.hooloovoochimico.kmp.hbible.
        applicationId = "com.hooloovoochimico.hbible"
        minSdk = 24
        targetSdk = 36
        // Dal workflow di rilascio (R09): -PversionCode=10203 -PversionName=1.2.3 (tag v1.2.3).
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "1.0.0"
        manifestPlaceholders["appLabel"] = "HBible"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Firma di release con la chiave di caricamento (upload key, Play App Signing):
    // - in locale da keystore.properties (root, gitignored: storeFile/storePassword/keyAlias/keyPassword);
    // - in CI da variabili d'ambiente (HBIBLE_UPLOAD_KEYSTORE = percorso del .jks,
    //   HBIBLE_UPLOAD_KEYSTORE_PASSWORD, HBIBLE_UPLOAD_KEY_ALIAS, HBIBLE_UPLOAD_KEY_PASSWORD).
    // Senza nessuna delle due la release è firmata con la chiave di debug: installabile per
    // provarla in locale, NON pubblicabile. Con -PrequireReleaseSigning=true (workflow di
    // rilascio) il build si ferma invece di firmare in debug.
    val keystoreFile = rootProject.file("keystore.properties")
    val envKeystore = System.getenv("HBIBLE_UPLOAD_KEYSTORE")?.takeIf { it.isNotBlank() }
    val releaseSigning =
        when {
            keystoreFile.exists() -> {
                val props = Properties().apply { keystoreFile.inputStream().use { load(it) } }
                signingConfigs.create("release") {
                    storeFile = rootProject.file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            }
            envKeystore != null ->
                signingConfigs.create("release") {
                    storeFile = file(envKeystore)
                    storePassword = System.getenv("HBIBLE_UPLOAD_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("HBIBLE_UPLOAD_KEY_ALIAS")
                    keyPassword = System.getenv("HBIBLE_UPLOAD_KEY_PASSWORD")
                }
            findProperty("requireReleaseSigning") == "true" ->
                throw GradleException(
                    "Firma di release mancante: serve keystore.properties o HBIBLE_UPLOAD_KEYSTORE (vedi R06).",
                )
            else -> signingConfigs.getByName("debug")
        }

    buildTypes {
        getByName("debug") {
            // Id diverso: la build di sviluppo si installa accanto a quella del Play Store.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "HBible debug"
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
