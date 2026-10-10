#!/usr/bin/env python3
"""versionName e versionCode dal tag git del rilascio (task R06/R09).

Tag `vX.Y.Z` (definitivo) o `vX.Y.Z-<etichetta>N` (prova: -test1, -rc2, …, N da 1 a 98).
versionCode = (X*10000 + Y*100 + Z) * 100 + N, con N = 99 per il definitivo: cresce sempre,
anche da una prova al definitivo della stessa versione (1.0.0-test1 → 1000001, 1.0.0 → 1000099).
Traccia del Play Store dall'etichetta: -alpha → alpha (test chiuso), -beta → beta (test aperto),
le altre prove (-test, -rc, …) → internal; il definitivo → production (in bozza, vedi release.yml).
Stampa `version_name=…`, `version_code=…` e `track=…` (formato di $GITHUB_OUTPUT).
Uso: release_version.py v1.2.3
"""
from __future__ import annotations

import re
import sys

TAG = re.compile(r"^v(\d+)\.(\d+)\.(\d+)(?:-([a-z]+)(\d+))?$")


TRACKS = {"alpha": "alpha", "beta": "beta"}


def track(tag: str) -> str:
    m = TAG.match(tag)
    if not m:
        raise ValueError(f"tag non valido: {tag!r}")
    return TRACKS.get(m.group(4), "internal") if m.group(4) else "production"


def parse(tag: str) -> tuple[str, int]:
    m = TAG.match(tag)
    if not m:
        raise ValueError(f"tag non valido: {tag!r} (atteso vX.Y.Z o vX.Y.Z-test1)")
    major, minor, patch = (int(m.group(i)) for i in (1, 2, 3))
    if minor > 99 or patch > 99:
        raise ValueError("minor e patch al massimo 99")
    n = int(m.group(5)) if m.group(4) else 99
    if not 1 <= n <= 98 and m.group(4):
        raise ValueError("numero della prova da 1 a 98")
    name = f"{major}.{minor}.{patch}" + (f"-{m.group(4)}{n}" if m.group(4) else "")
    return name, (major * 10000 + minor * 100 + patch) * 100 + n


if __name__ == "__main__":
    try:
        name, code = parse(sys.argv[1])
    except (IndexError, ValueError) as e:
        sys.exit(f"errore: {e}")
    print(f"version_name={name}")
    print(f"version_code={code}")
    print(f"track={track(sys.argv[1])}")
