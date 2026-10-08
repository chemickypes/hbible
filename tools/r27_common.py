#!/usr/bin/env python3
# ATTENZIONE (dal 2026-10-08): i JSON in FILES_DIR sono generati dal CMS
# (bibbia-interlineare-project, `npm run publish` + `npm run export:hbible`).
# Non scriverci con questi script: le modifiche vanno fatte nel CMS, altrimenti
# il prossimo export le sovrascrive. Vedi AGENTS.md.
"""Shared helpers for the Riveduta 1927 (R27) pipeline tools.

The R27 source text embeds editorial versification markers such as "(H21-11)"
or "(G1-15)": the publisher's note stating that the following text belongs to
Hebrew/Greek verse 21:11 / 1:15. Since the standard Italian versification used
by the bundled originals follows the Hebrew (MT) numbering, a start-of-verse
marker directly gives the standard address of that verse's content; markers
found mid-verse mark a split point (the verse merges two standard verses).
"""

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BIBBIA_INTERLINEARE = ROOT.parent.parent / "bibbia-interlineare-project"
DEFAULT_R27 = BIBBIA_INTERLINEARE / "data" / "riveduta.json"
FILES_DIR = ROOT / "composeApp/src/commonMain/composeResources/files"

# Editorial marker "(H<chapter>-<verse>)" / "(G<chapter>-<verse>)".
MARKER_RE = re.compile(r"\(([HG])(\d+)-(\d+)\)")

PUNCT = ".,;:!?«»\"'()[]{}…—–’‘“”·"


def normalize(text: str) -> str:
    return " ".join(t.strip(PUNCT).lower() for t in text.split() if t.strip(PUNCT))


def tokens_of(norm: str) -> set:
    return set(norm.split())


def split_marker(text: str) -> tuple:
    """Returns (clean_text, marker_tuple_or_None, marker_position).

    marker_tuple = (chapter, verse) of the FIRST H/G marker; marker_position
    is its character offset in the ORIGINAL text (0 = start-of-verse marker,
    >0 = mid-verse split marker). clean_text has every marker removed (a few
    verses carry two, e.g. Ap 13:1 "(G12-18) ... (G13-1) ...").
    """
    matches = list(MARKER_RE.finditer(text))
    if not matches:
        return text, None, -1
    first = matches[0]
    chapter, verse = int(first.group(2)), int(first.group(3))
    clean = re.sub(r"\s+", " ", MARKER_RE.sub(" ", text)).strip()
    return clean, (chapter, verse), first.start()


def strip_marker(text: str) -> str:
    """Removes the editorial marker and normalizes spacing (keeps wording)."""
    return split_marker(text)[0]
