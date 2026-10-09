# HBible

Lettore della Bibbia per Android e iOS (Kotlin Multiplatform + Compose Multiplatform), pensato
per lo studio: testo italiano, interlineare ebraico/greco parola per parola, lessico di Strong,
riferimenti incrociati, note personali e ricerca. Funziona offline.

- **Traduzioni:** Bibbia Aperta (Open Translation Bible, CC BY-SA 4.0, predefinita),
  Riveduta 1927, Diodati, Martini. Solo testi con licenza aperta o di pubblico dominio.
- **Interlineare:** testo ebraico (WLC/OSHB) e greco (Nestle 1904) con traslitterazione,
  numeri di Strong, gloss inglesi e italiane e allineamento con la traduzione scelta.
- **Aggiornamenti dei contenuti** (in preparazione): l'app scaricherà le correzioni
  pubblicate senza bisogno di aggiornarla (pacchetti statici con hash, branch `gh-pages`).
- **Assistente AI** facoltativo, con la propria chiave del provider (OpenAI, Anthropic,
  Gemini, Z.ai): la chiave resta sul dispositivo.

## Compilare

Requisiti: JDK 21+ (il wrapper scarica JBR 25), Android SDK con `platforms;android-37.0`,
`python3` sul PATH (genera il database incluso dai JSON in `composeApp/src/commonMain/composeResources/files`).

```bash
./gradlew :composeApp:assembleDebug          # APK di debug
./gradlew :composeApp:testDebugUnitTest      # test
./gradlew :composeApp:compileKotlinIosSimulatorArm64   # verifica iOS (su macOS anche build Xcode in iosApp/)
```

La build di release legge la chiave di firma da `keystore.properties` (non versionato) o dalle
variabili d'ambiente del workflow di rilascio.

## Licenze

- Codice: [GNU GPL v3](LICENSE).
- Contenuti (testi biblici, originali, lessico, rimandi, gloss): vedi [CONTENT-LICENSE.md](CONTENT-LICENSE.md).
