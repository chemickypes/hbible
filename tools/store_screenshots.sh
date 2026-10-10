#!/usr/bin/env bash
# Screenshot per la scheda del Play Store (task R07), salvati direttamente in
# fastlane/metadata/android/it-IT/images/{phone,sevenInch,tenInch}Screenshots/.
#
# Uso: ./tools/store_screenshots.sh [serial]   (default ANDROID_SERIAL o emulator-5554)
#
# Richiede ImageMagick (magick). Prerequisito: APK di RELEASE installato (niente voci di sviluppo nelle Impostazioni):
#   ./gradlew :composeApp:assembleRelease && adb install -r composeApp/build/outputs/apk/release/composeApp-release.apk
# Azzera i dati dell'app di release (pm clear: si riparte da Genesi 1, cronologia vuota), mette la
# barra di stato in demo mode (09:41, batteria piena) e per ogni dispositivo cattura:
#   1 lettore (Genesi 1), 2 interlineare (Gn 1:1), 3 dettaglio parola con lessico,
#   4 Salmo 23 in tema scuro, 5 ricerca, 6 impostazioni.
# Alla fine ripristina sempre wm size/density, tema, animazioni e barra di stato.
set -u

SERIAL="${1:-${ANDROID_SERIAL:-emulator-5554}}"
PKG="com.hooloovoochimico.hbible"
ACTIVITY="$PKG/com.hooloovoochimico.kmp.hbible.MainActivity"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMAGES="$ROOT/fastlane/metadata/android/it-IT/images"
UI=/tmp/hbible-store-ui.xml

adb="adb -s $SERIAL"
$adb wait-for-device
ash() { timeout 30 $adb shell "$@"; }

$adb shell pm path "$PKG" >/dev/null 2>&1 || { echo "!! $PKG non installato (serve l'APK di release)"; exit 1; }

demo() { ash am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
restore() {
  echo ">> Ripristino"
  demo exit
  ash cmd uimode night no >/dev/null 2>&1
  ash wm density reset >/dev/null 2>&1; sleep 2
  ash wm size reset >/dev/null 2>&1
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do
    ash settings put global $k 1 >/dev/null 2>&1
  done
}
trap restore EXIT

for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  ash settings put global $k 0 >/dev/null
done
ash settings put global sysui_demo_allowed 1 >/dev/null
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4
demo network -e mobile hide
demo notifications -e visible false

dump_ui() {
  local i
  for i in 1 2 3 4 5; do
    ash uiautomator dump /sdcard/hbible-store-ui.xml >/dev/null 2>&1 &&
      $adb exec-out cat /sdcard/hbible-store-ui.xml > "$UI" 2>/dev/null && return 0
    sleep 1
  done
  return 1
}

# tap "<testo>" [più in basso: 1] : tocca il nodo con text o content-desc uguale.
tap() {
  local i
  for i in 1 2 3; do
    dump_ui || continue
    local xy
    xy=$(python3 - "$1" "${2:-0}" "$UI" <<'PY'
import re, sys
target, lowest, path = sys.argv[1], sys.argv[2] == "1", sys.argv[3]
best = None
for n in re.findall(r"<node [^>]*>", open(path, encoding="utf-8").read()):
    t = re.search(r' text="([^"]*)"', n).group(1)
    d = re.search(r'content-desc="([^"]*)"', n).group(1)
    if target in (t, d):
        b = list(map(int, re.findall(r"\d+", re.search(r'bounds="([^"]*)"', n).group(1))))
        c = ((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
        if best is None or (lowest and c[1] > best[1]):
            best = c
        if not lowest:
            break
if best:
    print(best[0], best[1])
PY
)
    if [ -n "$xy" ]; then ash input tap $xy; return 0; fi
    sleep 1
  done
  echo "   !! non trovato: $1"; return 1
}

has_text() { dump_ui && grep -q "text=\"$1\"" "$UI"; }

wait_text() {
  local i
  for i in $(seq 1 40); do has_text "$1" && return 0; sleep 1; done
  return 1
}

# Scorre (in basso) finché compare "<testo>", poi lo tocca.
scroll_tap() {
  local i h
  h=$(ash wm size | grep -o '[0-9]*x[0-9]*' | tail -1 | cut -dx -f2)
  for i in $(seq 1 12); do
    has_text "$1" && { tap "$1"; return 0; }
    ash input swipe 500 $((h * 3 / 4)) 500 $((h * 2 / 5)) 400; sleep 1
  done
  echo "   !! non trovato scorrendo: $1"; return 1
}

# Il Play Store vuole PNG senza trasparenza (24 bit): ImageMagick toglie il canale alfa.
shot() {
  timeout 60 $adb exec-out screencap -p > "$1"
  magick "$1" -alpha off "PNG24:$1"
  echo "   $(basename "$(dirname "$1")")/$(basename "$1")"
}

# Prima parola ebraica dell'interlineare (testo con lettere ebraiche): ne tocca la seconda.
tap_hebrew_word() {
  dump_ui || return 1
  local xy
  xy=$(python3 - "$UI" <<'PY'
import re, sys
nodes = []
for n in re.findall(r"<node [^>]*>", open(sys.argv[1], encoding="utf-8").read()):
    t = re.search(r' text="([^"]*)"', n).group(1)
    if re.search(r"[א-ת]", t) and " " not in t:
        b = list(map(int, re.findall(r"\d+", re.search(r'bounds="([^"]*)"', n).group(1))))
        nodes.append(((b[0] + b[2]) // 2, (b[1] + b[3]) // 2))
if len(nodes) >= 2:
    print(*nodes[1])
PY
)
  [ -n "$xy" ] && ash input tap $xy
}

# Nome cartella, wm size, density. Telefono 1080x1920 (9:16: il Play Store vuole il lato lungo
# al massimo doppio del corto), tablet 7" in verticale, tablet 10" in orizzontale.
CONFIGS=(
  "phoneScreenshots 1080x1920 420"
  "sevenInchScreenshots 1200x1920 320"
  "tenInchScreenshots 2560x1600 320"
)

for cfg in "${CONFIGS[@]}"; do
  set -- $cfg
  dir="$IMAGES/$1"; size="$2"; density="$3"
  echo ">> $1 ($size @ ${density}dpi)"
  mkdir -p "$dir"; rm -f "$dir"/*.png
  ash cmd uimode night no >/dev/null
  ash wm size "$size" >/dev/null && sleep 2
  ash wm density "$density" >/dev/null && sleep 2
  ash pm clear "$PKG" >/dev/null
  ash am start -n "$ACTIVITY" >/dev/null
  wait_text "Genesi 1" || { echo "   !! app non pronta"; continue; }
  sleep 3
  shot "$dir/1.png"

  tap "Interlineare" 1 && wait_text "Genesi 1:1" && sleep 2 && shot "$dir/2.png"
  tap_hebrew_word && sleep 3 && shot "$dir/3.png"
  ash input keyevent KEYCODE_BACK; sleep 2

  tap "Bibbia" 1 && sleep 2
  # Foglio "Scegli un libro" (tessere): si cerca il libro invece di scorrere l'elenco.
  tap "Genesi 1" && sleep 2 && tap "Cerca un libro (es. Romani, 1Cor)" && sleep 1
  ash input text "salmi"; sleep 2; ash input keyevent KEYCODE_ESCAPE; sleep 1
  tap "Salmi" && sleep 2 && tap "23" && sleep 3
  ash cmd uimode night yes >/dev/null; sleep 4
  has_text "Salmi 23" && shot "$dir/4.png"
  ash cmd uimode night no >/dev/null; sleep 3

  tap "Cerca" 1 && sleep 2 && tap "Testo o riferimento (1 Gv 1:3)" && sleep 1
  ash input text "pastore"; ash input keyevent KEYCODE_ENTER; sleep 4
  ash input keyevent KEYCODE_ESCAPE; sleep 1
  shot "$dir/5.png"

  tap "Impostazioni" 1 && sleep 3 && shot "$dir/6.png"
done

echo ">> Fatto"
