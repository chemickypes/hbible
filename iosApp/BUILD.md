# iosApp — build su Mac

> **Stato (Fase 6)** — Il codice Kotlin iOS è **verificato su Linux**:
> `compileKotlinIosSimulatorArm64`, `compileKotlinIosArm64` e
> `linkDebugFrameworkIosSimulatorArm64` completano con successo (vedi
> `docs/kmp-migration/PLAN.md` §12).
> Il progetto Xcode (`project.pbxproj`, Swift, plist, asset catalog) è **UNVERIFIED**:
> su questa macchina Linux non c'è Xcode, quindi non è mai stato compilato/aperto.
> Se qualcosa non torna all'apertura, i punti fragili sono elencati in fondo.

## Prerequisiti (Mac)

1. **Xcode 16+** con i simulatori iOS (progetto: `compatibilityVersion Xcode 15.0`,
   deployment target 16.0). Su Apple Silicon serve anche **Rosetta** solo se si usa un
   simulatore Intel — non necessario per i simulatori arm64 consigliati qui.
2. **JDK 17+** (consigliato lo stesso del progetto, JBR 25). Due opzioni:
   - installare un JDK in `/Library/Java/JavaVirtualMachines` (lo script di build fa
     fallback su `/usr/libexec/java_home` se `JAVA_HOME` non è impostato), oppure
   - `export JAVA_HOME=<percorso JDK>` prima di invocare Gradle da terminale.
   Gradle scarica automaticamente il toolchain richiesto (JBR 25, via foojay resolver
   già configurato in `settings.gradle.kts`).
3. **Android SDK — necessario anche per la build iOS.** Il modulo `composeApp` applica
   il plugin `com.android.application` (AGP): alla configurazione Gradle serve un SDK,
   anche se si compila solo il framework iOS. Nessuna dipendenza extra serve a runtime:
   - la via più semplice è installare **Android Studio** (che porta l'SDK), oppure
   - solo cmdline-tools: `sdkmanager "platform-tools" "platforms;android-37" "build-tools;<ultima>"`.
4. **`local.properties`**: sul repo Linux contiene `sdk.dir=/home/hooloovoo/android-sdk`,
   non valido sul Mac. Il file non è versionato: su una nuova copia va creato con
   `sdk.dir=/Users/<utente>/Library/Android/sdk` (o impostare `ANDROID_HOME`).

## Struttura

```
iosApp/
├── iosApp.xcodeproj/     progetto Xcode (target "iosApp", bundle id
│                         com.hooloovoochimico.kmp.hbible)
└── iosApp/
    ├── iOSApp.swift      entry @main → ContentView
    ├── ContentView.swift → MainViewControllerKt.MainViewController() (framework ComposeApp)
    ├── Assets.xcassets   AppIcon (set vuoto: aggiungere l'immagine 1024×1024 se serve)
    └── Info.plist        nome "HBible", orientamenti portrait/landscape
```

## Come funziona l'integrazione Gradle↔Xcode

Il target Xcode ha una build phase script **`embedAndSignAppleFrameworkForXcode`**
(posizionata prima di Sources) che esegue:

```
cd "$SRCROOT/.."   # root del repo, dove c'è ./gradlew
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Il task Gradle (creato dal plugin Kotlin Multiplatform):
- legge le variabili d'ambiente di Xcode (`PLATFORM_NAME`, `ARCHS`, `CONFIGURATION`,
  `CONTENTS_FOLDER_PATH`, …) e sceglie il framework giusto:
  - simulatore → `linkDebugFrameworkIosSimulatorArm64` (release → `linkReleaseFrameworkIosSimulatorArm64`)
  - device → `linkDebugFrameworkIosArm64` / `linkReleaseFrameworkIosArm64`
- copia il framework **ComposeApp** (`baseName = "ComposeApp"`, `isStatic = true` in
  `composeApp/build.gradle.kts`) in `composeApp/build/xcode-frameworks/<CONFIG>/<SDK>/`
  e lo embedda/firma dentro l'app;
- Xcode lo trova per compilare `import ComposeApp` grazie a
  `FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.

Pre-compilare il framework da terminale (opzionale, comodo per isolare errori Gradle da
errori Xcode — **su Mac**):

```
JAVA_HOME=<percorso JDK> ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

> Nota Linux: `linkDebugFrameworkIos*` è **disabilitato by design** su host non-Apple
> (KGP 2.3.20: `enabledOnCurrentHostForBinariesCompilation` controlla solo
> `HostManager.isEnabled` → task SKIPPED, nessun flag lo abilita; testato con
> `enableKlibsCrossCompilation` e `ignoreDisabledTargets`). Le task
> `compileKotlinIos*` funzionano invece su Linux. Il framework lo produce Xcode (o
> Gradle su Mac).

## Passi per build & run

1. Copiare/cloneare il repo sul Mac e sistemare `local.properties` (vedi prerequisiti).
2. Aprire `iosApp/iosApp.xcodeproj` in Xcode.
3. Target `iosApp` → **Signing & Capabilities** → selezionare il **Team** (l'id team nel
   pbxproj è vuoto: si imposta dall'UI).
4. Scegliere un simulatore iPhone (arm64, es. "iPhone 16") e premere **Run (⌘R)**.
5. Primo avvio: l'app parte subito (tutte le definizioni Koin sono lazy) e importa gli
   asset in background — l'UI resta reattiva, i versetti compaiono dopo qualche secondo
   (~30 MB di JSON). A build successive il framework Kotlin viene ricompilato solo se
   i sorgenti sono cambiati.

## Asset JSON (~30 MB)

I 5 asset (`nuova_riveduta`, `riveduta_2020`, `originals`, `crossrefs`, `lexicon`) vivono
in `composeApp/src/commonMain/composeResources/files/` (Compose Multiplatform resources,
package `com.hooloovoochimico.kmp.hbible.resources`). Su iOS Compose Multiplatform li
include **dentro il framework** automaticamente al link: **nessun passo extra in Xcode**
(niente copie nel bundle, niente "Copy Bundle Resources"). L'import usa
`Res.readBytes("files/<nome>.json")` ed è asincrono/lazy (`ensureImported()`).

## iOS x64 (simulatore Intel): warning noto e non bloccante

JB navigation `org.jetbrains.androidx.navigation:navigation-compose` **2.10.0-beta01** e
JB lifecycle **2.11.0** non pubblicano varianti `iosX64`: sui target Intel-simulator
Gradle stampa "KMP Dependencies Resolution Failure". È un warning pre-esistente e **non
blocca** simulator arm64 e device arm64. Se serve davvero il simulatore Intel, l'unica
via è degradare navigation/lifecycle (es. 2.9.x) — non fatto di proposito.

## Icona app

`Assets.xcassets/AppIcon.appiconset/` è creato ma vuoto: trascinare in Xcode una PNG
1024×1024 (formato single-size) oppure sostituire il `Contents.json` con il formato
multi-size se si vuole una serie completa.

## Se qualcosa non va (punti UNVERIFIED)

- **pbxproj scritto a mano**: è un file plist testuale validato solo per struttura;
  se Xcode protesta all'apertura, rigenerare il progetto con il wizard KMP
  (kmp.jetbrains.com) e riportare bundle id, `FRAMEWORK_SEARCH_PATHS`
  (`composeApp/build/xcode-frameworks/...`), la build phase script
  `embedAndSignAppleFrameworkForXcode` (prima di Sources) e `Info.plist`.
- **Swift**: `ContentView.swift` fa `import ComposeApp` e chiama
  `MainViewControllerKt.MainViewController()` — i nomi dipendono da
  `baseName = "ComposeApp"` e dal package `com.hooloovoochimico.kmp.hbible`.
- **static framework**: `isStatic = true` richiede Kotlin ≥ 1.9.20 per il task
  `embedAndSignAppleFrameworkForXcode` (qui Kotlin 2.3.20: ok).
- Se il build da Xcode non trova `ComposeApp`, verificare che la directory
  `composeApp/build/xcode-frameworks/<CONFIG>/<SDK>/ComposeApp.framework` esista dopo
  la build phase (si vede nel report di build della script phase).

## Verifiche già fatte su Linux (non ripetibili qui)

```
JAVA_HOME=/opt/android-studio/jbr ./gradlew :composeApp:compileKotlinIosSimulatorArm64   # OK
JAVA_HOME=/opt/android-studio/jbr ./gradlew :composeApp:compileKotlinIosArm64            # OK
```

`linkDebugFrameworkIosSimulatorArm64` **non** è eseguibile su Linux (task disabilitato
da KGP su host non-Apple — vedi nota sopra): il link/embed/codesign del framework e
dell'app restano da fare su Mac.
