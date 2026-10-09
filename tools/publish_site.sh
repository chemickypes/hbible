#!/usr/bin/env bash
# Pubblica le pagine statiche di site/ (privacy policy, …) nel branch gh-pages del repo pubblico,
# servito da GitHub Pages su https://chemickypes.github.io/hbible/ (task R08). Non tocca content/,
# che è gestito dal CMS (npm run deploy:content).
#
# Uso: tools/publish_site.sh [--push]   (senza --push mostra solo cosa cambierebbe)
# Copia locale del branch: HBIBLE_PAGES_DIR (default ../../hbible-pages, la stessa del CMS).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PAGES="${HBIBLE_PAGES_DIR:-$ROOT/../../hbible-pages}"
PUSH=0; [ "${1:-}" = "--push" ] && PUSH=1

[ -d "$PAGES/.git" ] || { echo "Manca $PAGES: esegui prima npm run deploy:content nel CMS (crea la copia)."; exit 1; }
cd "$PAGES"
case "$(git remote get-url origin)" in *HBible-dev*) echo "La copia punta al repo privato: mi fermo."; exit 1;; esac
[ "$(git symbolic-ref --short HEAD)" = gh-pages ] || { echo "La copia non è sul branch gh-pages."; exit 1; }
git fetch -q origin && git merge -q --ff-only origin/gh-pages

# Copia site/ nella radice (le cartelle di site/ sostituiscono quelle omonime; content/ escluso).
for d in "$ROOT"/site/*; do
  name="$(basename "$d")"
  [ "$name" = content ] && { echo "site/content è riservato ai contenuti del CMS"; exit 1; }
  rm -rf "./$name"; cp -r "$d" "./$name"
done

git add -A
if git diff --cached --quiet; then echo "Il sito è già aggiornato."; exit 0; fi
git diff --cached --stat
if [ "$PUSH" = 0 ]; then echo "Prova: niente commit né push. Per pubblicare: tools/publish_site.sh --push"; exit 0; fi
git commit -q -m "Pagine del sito: $(cd "$ROOT" && git log -1 --format=%h -- site)"
git push -q origin gh-pages
echo "Pubblicato su https://chemickypes.github.io/hbible/"
