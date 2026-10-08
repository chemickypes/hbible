#!/usr/bin/env bash
# Screenshot delle schermate principali a 4 larghezze (NEXT_STEPS punto 5c),
# per la revisione visiva prima di ogni release.
#
# Uso: ./tools/screenshots.sh [serial]   (default ANDROID_SERIAL o emulator-5554)
#
# Per ogni larghezza (wm size + wm density) riavvia l'app e cattura Lettore,
# Note, Esplora, Impostazioni (tap ricavato da uiautomator dump, così funziona
# sia con la bottom bar sia con la rail) e un dettaglio versetto (pressione
# lunga). Le immagini finiscono in composeApp/build/screenshots/<data>/.
# Alla fine (anche in caso di errore) ripristina sempre size/density.
set -u

SERIAL="${1:-${ANDROID_SERIAL:-emulator-5554}}"
PKG="com.hooloovoochimico.kmp.hbible"
ACTIVITY="$PKG/.MainActivity"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/composeApp/build/screenshots/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

adb="adb -s $SERIAL"
$adb wait-for-device

# adb shell può restare appeso dopo un cambio di wm size/density: limiti rigorosi.
ash() { timeout 30 $adb shell "$@"; }
acap() { timeout 60 $adb exec-out "$@"; }

restore() {
  echo ">> Ripristino wm size/density e animazioni"
  ash wm density reset >/dev/null 2>&1
  sleep 2
  ash wm size reset >/dev/null 2>&1
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do
    ash "settings put global $k 1" >/dev/null 2>&1
  done
}
trap restore EXIT

# Animazioni a 0: uiautomator dump fallisce ("not idle") mentre animano.
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  ash "settings put global $k 0" >/dev/null 2>&1
done

dump_ui() {
  local i
  for i in 1 2 3 4 5 6; do
    ash uiautomator dump /sdcard/hbible-ui.xml >/dev/null 2>&1 && \
      timeout 30 $adb shell cat /sdcard/hbible-ui.xml > /tmp/hbible-ui.xml 2>/dev/null && return 0
    sleep 1.5
  done
  return 1
}

# tap_desc "<nome>" : tocca il nodo più in basso con quel content-desc o text.
# (content-desc = tab della bottom bar; text = voci della rail a schermi larghi.
#  "Più in basso" distingue le tab da elementi omonimi, es. "Cerca" nella Note.)
tap_desc() {
  local i line coords best bestcy cy
  for i in 1 2; do
    dump_ui || { sleep 2; continue; }
    best=""; bestcy=-1
    while IFS= read -r line; do
      coords=$(printf '%s' "$line" | grep -o "[0-9]*" | tr '\n' ' ')
      [ -z "$coords" ] && continue
      set -- $coords
      cy=$(( ($2 + $4) / 2 ))
      if [ "$cy" -gt "$bestcy" ]; then bestcy=$cy; best="$coords"; fi
    done < <(tr '>' '>\n' < /tmp/hbible-ui.xml |
      grep -o "\(content-desc\|text\)=\"$1\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"")
    if [ -n "$best" ]; then
      set -- $best
      ash input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
      return 0
    fi
    sleep 2
  done
  return 1
}

# wait_text "<prefisso>" : attende che l'UI mostri un nodo con quel testo.
wait_text() {
  local i
  for i in $(seq 1 30); do
    dump_ui || { sleep 1; continue; }
    grep -q "text=\"$1" /tmp/hbible-ui.xml && return 0
    sleep 1
  done
  return 1
}

restart_app() {
  # connectedDebugAndroidTest disinstalla l'app: la reinstalla se manca.
  ash "pm path $PKG" >/dev/null 2>&1 || \
    timeout 300 $adb install -r "$ROOT/composeApp/build/outputs/apk/debug/composeApp-debug.apk" >/dev/null
  ash am force-stop "$PKG"
  sleep 1
  ash am start -n "$ACTIVITY" >/dev/null
  wait_text "Genesi" || wait_text "Libro" || { echo "!! app non pronta"; return 1; }
  sleep 2
}

capture() { acap screencap -p > "$OUT/$1.png"; echo "   $1.png"; }

# dettaglio versetto: pressione lunga sul primo versetto lungo a schermo.
tap_verse_long() {
  dump_ui || return 1
  local coords
  coords=$(tr '>' '>\n' < /tmp/hbible-ui.xml |
    grep -o 'text="[^"]\{50,\}"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' |
    head -1 | grep -o "[0-9]*" | tr '\n' ' ')
  [ -z "$coords" ] && return 1
  set -- $coords
  ash input swipe $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )) \
                  $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )) 1500
}

# Nome, wm size, wm density (dp: 360x800, 600x960, 840x1200, 1200x800).
CONFIGS=(
  "phone-360 720x1600 320"
  "medium-600 1200x1920 320"
  "expanded-840 1680x2400 320"
  "large-1200 2400x1600 320"
)

echo ">> Output: $OUT"
for cfg in "${CONFIGS[@]}"; do
  set -- $cfg
  label="$1"; size="$2"; density="$3"
  echo ">> $label ($size @ ${density}dpi)"
  ash wm size "$size" >/dev/null && sleep 3
  ash wm density "$density" >/dev/null && sleep 3
  restart_app || continue

  capture "$label-01-lettore"

  tap_desc "Note" && sleep 2 && capture "$label-02-note" || echo "   !! Note non trovata"
  tap_desc "Cerca" && sleep 3 && capture "$label-03-esplora" || echo "   !! Esplora non trovata"
  tap_desc "Impostazioni" && sleep 2 && capture "$label-04-impostazioni" || echo "   !! Impostazioni non trovata"

  tap_desc "Bibbia" && sleep 2
  tap_verse_long && sleep 3 && capture "$label-05-dettaglio" || echo "   !! Dettaglio non aperto"
done

echo ">> Fatto: $(ls "$OUT" | wc -l) immagini in $OUT"
