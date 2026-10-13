#!/usr/bin/env python3
"""Grafiche degli screenshot per il Play Store (task R07).

Prende gli screenshot grezzi in fastlane/raw-screenshots/<cartella>/<n>.png (li scrive
tools/store_screenshots.sh), li mette in una cornice di dispositivo su fondo oro con titolo e
testo da captions.json e salva i PNG finali (24 bit, senza trasparenza) in
fastlane/metadata/android/it-IT/images/<cartella>/<n>.png.

Uso:  python3 tools/store_frames/render.py
Richiede Playwright per Python con Chromium (pip install playwright; playwright install chromium)
e ImageMagick (magick o convert) per togliere il canale alfa.
"""
import html
import json
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

from playwright.sync_api import sync_playwright

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RAW = ROOT / "fastlane/raw-screenshots"
OUT = ROOT / "fastlane/metadata/android/it-IT/images"

# cartella -> (larghezza, altezza, layout)
FORMATS = {
    "phoneScreenshots": (1080, 1920, "phone"),
    "sevenInchScreenshots": (1200, 1920, "seven"),
    "tenInchScreenshots": (2560, 1600, "ten"),
}

CSS = """
@font-face { font-family: 'Noto Serif'; font-weight: 600; font-style: normal; src: url('fonts/noto-serif-latin-600-normal.woff2'); }
@font-face { font-family: 'Noto Serif'; font-weight: 600; font-style: italic; src: url('fonts/noto-serif-latin-600-italic.woff2'); }
@font-face { font-family: 'Noto Sans'; font-weight: 400; src: url('fonts/noto-sans-latin-400-normal.woff2'); }
@font-face { font-family: 'Noto Sans'; font-weight: 600; src: url('fonts/noto-sans-latin-600-normal.woff2'); }
* { box-sizing: border-box; }
html, body { margin: 0; }
.canvas { position: relative; overflow: hidden; width: %(w)dpx; height: %(h)dpx;
  background: radial-gradient(ellipse at 50%% 78%%, #A06C1E 0%%, rgba(160,108,30,0) 60%%),
              linear-gradient(180deg, #8F5E15 0%%, #8A5A12 50%%, #6E4708 100%%); }
.copy { position: absolute; color: #FBF7EF; }
.kicker { font: 600 %(kicker)dpx/1 'Noto Sans', sans-serif; letter-spacing: .16em; text-transform: uppercase;
  color: #F2C987; margin: 0 0 %(gap1)dpx; }
h1 { font: 600 %(title)dpx/1.12 'Noto Serif', serif; margin: 0 0 %(gap2)dpx; letter-spacing: -.01em; }
h1 em { font-style: italic; color: #FFDDB0; }
p { font: 400 %(text)dpx/1.42 'Noto Sans', sans-serif; margin: 0; color: rgba(251,247,239,.86); }
.device { position: absolute; background: #1F1A14; padding: %(bezel)dpx; border-radius: %(radius)dpx;
  box-shadow: 0 0 0 3px rgba(255,221,176,.18), 0 50px 110px rgba(40,20,0,.55); }
.device img { display: block; width: 100%%; border-radius: %(inner)dpx; }
"""

LAYOUTS = {
    # testo in alto, dispositivo centrato che esce dal bordo inferiore
    "phone": dict(kicker=30, title=82, text=38, gap1=26, gap2=28, bezel=18, radius=74, inner=58,
                  copy="left:90px; right:90px; top:110px;",
                  device="left:130px; width:820px; top:560px;"),
    "seven": dict(kicker=32, title=88, text=40, gap1=28, gap2=30, bezel=18, radius=60, inner=44,
                  copy="left:100px; right:100px; top:120px;",
                  device="left:150px; width:900px; top:580px;"),
    # testo a sinistra, tablet a destra (intero: l'interlineare ebraico sta sul bordo destro)
    "ten": dict(kicker=38, title=104, text=46, gap1=34, gap2=36, bezel=22, radius=56, inner=36,
                copy="left:130px; width:700px; top:50%; transform:translateY(-50%);",
                device="left:900px; width:1540px; top:50%; transform:translateY(-50%);"),
}


def title_html(title: str) -> str:
    return re.sub(r"\*(.+?)\*", r"<em>\1</em>", html.escape(title))


def page(w, h, layout, cap, shot: Path) -> str:
    L = LAYOUTS[layout]
    css = CSS % dict(w=w, h=h, **{k: v for k, v in L.items() if isinstance(v, int)})
    return f"""<!doctype html><html lang="it"><head><meta charset="utf-8"><style>{css}</style></head><body>
<div class="canvas">
  <div class="copy" style="{L['copy']}">
    <div class="kicker">{html.escape(cap['kicker'])}</div>
    <h1>{title_html(cap['title'])}</h1>
    <p>{html.escape(cap['text'])}</p>
  </div>
  <div class="device" style="{L['device']}"><img src="{shot.as_uri()}"></div>
</div></body></html>"""


def main() -> None:
    caps = json.loads((HERE / "captions.json").read_text(encoding="utf-8"))
    magick = shutil.which("magick") or shutil.which("convert")
    if not magick:
        raise SystemExit("serve ImageMagick (magick o convert)")
    with sync_playwright() as p, tempfile.TemporaryDirectory(dir=HERE) as tmp:
        browser = p.chromium.launch()
        for folder, (w, h, layout) in FORMATS.items():
            shots = sorted((RAW / folder).glob("*.png"), key=lambda f: int(f.stem))
            (OUT / folder).mkdir(parents=True, exist_ok=True)
            for shot in shots:
                cap = caps[int(shot.stem) - 1]
                src = Path(tmp) / f"{folder}-{shot.stem}.html"
                src.write_text(page(w, h, layout, cap, shot), encoding="utf-8")
                tab = browser.new_page(viewport={"width": w, "height": h})
                tab.goto(src.as_uri())
                tab.evaluate("document.fonts.ready")
                tab.wait_for_load_state("networkidle")
                png = Path(tmp) / f"{folder}-{shot.stem}.png"
                tab.screenshot(path=str(png))
                tab.close()
                dest = OUT / folder / shot.name
                subprocess.run([magick, str(png), "-alpha", "off", f"PNG24:{dest}"], check=True)
                print(f"{dest.relative_to(ROOT)}  {w}x{h}")
        browser.close()


if __name__ == "__main__":
    main()
