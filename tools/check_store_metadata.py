#!/usr/bin/env python3
"""Controlla i metadati della scheda Play Store in fastlane/metadata/android (task R07).

Limiti di Google Play: titolo 30 caratteri, descrizione breve 80, completa 4000, note di
versione 500; icona 512x512 PNG a 32 bit; grafica in evidenza 1024x500 e screenshot senza
trasparenza (PNG a 24 bit); da 2 a 8 screenshot
per tipo di dispositivo, lati tra 320 e 3840 px, lato lungo al massimo doppio del corto.
Esce con codice 1 se qualcosa non va. Solo libreria standard (gira anche in CI).
"""
from __future__ import annotations

import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
META = ROOT / "fastlane/metadata/android"

TEXT_LIMITS = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000}
CHANGELOG_LIMIT = 500
SCREENSHOT_DIRS = ["phoneScreenshots", "sevenInchScreenshots", "tenInchScreenshots"]

errors: list[str] = []


def png_info(path: Path) -> tuple[int, int, int]:
    """Larghezza, altezza e color type (6 = RGBA) dall'header PNG."""
    data = path.read_bytes()[:33]
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError("non è un PNG")
    width, height, _depth, color_type = struct.unpack(">IIBB", data[16:26])
    return width, height, color_type


def check_locale(locale: Path) -> None:
    name = locale.name
    for file, limit in TEXT_LIMITS.items():
        path = locale / file
        if not path.exists():
            errors.append(f"{name}/{file}: manca")
            continue
        n = len(path.read_text(encoding="utf-8").strip())
        if n == 0 or n > limit:
            errors.append(f"{name}/{file}: {n} caratteri (limite {limit})")
    for path in sorted((locale / "changelogs").glob("*.txt")):
        if not (path.stem.isdigit() or path.stem == "default"):
            errors.append(f"{name}/changelogs/{path.name}: il nome deve essere il versionCode o default")
        n = len(path.read_text(encoding="utf-8").strip())
        if n > CHANGELOG_LIMIT:
            errors.append(f"{name}/changelogs/{path.name}: {n} caratteri (limite {CHANGELOG_LIMIT})")

    images = locale / "images"
    for file, size, rgba in [("icon.png", (512, 512), True), ("featureGraphic.png", (1024, 500), False)]:
        path = images / file
        if not path.exists():
            errors.append(f"{name}/images/{file}: manca")
            continue
        w, h, color_type = png_info(path)
        if (w, h) != size:
            errors.append(f"{name}/images/{file}: {w}x{h}, serve {size[0]}x{size[1]}")
        if rgba and color_type != 6:
            errors.append(f"{name}/images/{file}: serve un PNG a 32 bit (RGBA)")
        if not rgba and color_type in (4, 6):
            errors.append(f"{name}/images/{file}: serve un PNG senza trasparenza (24 bit)")

    for folder in SCREENSHOT_DIRS:
        shots = sorted((images / folder).glob("*.png"))
        if folder == "phoneScreenshots" and not 2 <= len(shots) <= 8:
            errors.append(f"{name}/{folder}: {len(shots)} screenshot (servono da 2 a 8)")
        elif shots and not 2 <= len(shots) <= 8:
            errors.append(f"{name}/{folder}: {len(shots)} screenshot (da 2 a 8)")
        for path in shots:
            w, h, color_type = png_info(path)
            if color_type in (4, 6):
                errors.append(f"{name}/{folder}/{path.name}: serve un PNG senza trasparenza (24 bit)")
            if not (320 <= min(w, h) and max(w, h) <= 3840):
                errors.append(f"{name}/{folder}/{path.name}: {w}x{h} fuori dai limiti 320–3840 px")
            if max(w, h) > 2 * min(w, h):
                errors.append(f"{name}/{folder}/{path.name}: {w}x{h}, lato lungo oltre il doppio del corto")


def main() -> int:
    locales = sorted(p for p in META.iterdir() if p.is_dir()) if META.exists() else []
    if not locales:
        print(f"Nessuna lingua in {META}")
        return 1
    for locale in locales:
        check_locale(locale)
    if errors:
        print("Metadati dello store NON validi:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"Metadati dello store validi ({', '.join(p.name for p in locales)}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
