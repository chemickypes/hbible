# Regole R8 della release Android.
# kotlinx.serialization, Room, Ktor, Koin e Compose includono già le proprie
# consumer rules: qui solo ciò che R8 non può dedurre da solo.

# Avvisi di classi opzionali referenziate da Ktor/OkHttp e non presenti su Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
