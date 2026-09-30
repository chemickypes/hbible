#!/usr/bin/env python3
"""Upgrade the bundled Strong's lexicon with clean OpenScriptures definitions.

Current lexicon.json entries carry KJV-style glosses ("chief, (fore-)father(-less),
X patrimony, principal. Compare names in \"Abi-\".") — historically accurate but
cryptic on a phone screen. The OpenScriptures Strong's dictionaries (the same
source family used by the bibbia-interlineare project) provide cleaner fields:

  def.short — concise definition      -> becomes "g" (shown in the word card)
  def.long  — full definition         -> becomes "gl" (context for the AI
                                         translation step; ignored by the app
                                         thanks to ignoreUnknownKeys)
  def.lit   — literal (Greek)         -> folded into "gl"

Per entry the script keeps the existing romanized "tr", replaces "g" with the
cleaned short definition when available (fallback: current gloss), and adds
"gi" (Italian gloss) from tools/gloss_it_seeds.json — the 9 curated seeds from
the bibbia-interlineare AI translations; the batch step (translate_glosses.py)
fills the rest.

Entry set is NOT extended: the bundled lemmas only reference the current
numbers, and OpenScriptures has no transliteration for the extras.

Usage:
  python3 tools/upgrade_lexicon.py [--seeds PATH] [--out PATH]
"""

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import BIBBIA_INTERLINEARE, FILES_DIR

DEFAULT_SEEDS = Path(__file__).resolve().parent / "gloss_it_seeds.json"
DEFAULT_OUT = FILES_DIR / "lexicon.json"
OS_HEBREW = BIBBIA_INTERLINEARE / "data" / "lexicon" / "hebrew.json"
OS_GREEK = BIBBIA_INTERLINEARE / "data" / "lexicon" / "greek.json"


def load_os(path: Path) -> dict:
    """strongs number (without letter) -> {short, long}."""
    out = {}
    for entry in json.loads(path.read_text(encoding="utf-8")):
        num = entry.get("strongs", "")[1:]
        if not num:
            continue
        d = entry.get("data", {}).get("def", {})
        short = (d.get("short") or "").strip()
        long_parts = list(d.get("long") or [])
        lit = (d.get("lit") or "").strip()
        if lit:
            long_parts.insert(0, f"[{lit}]")

        def flatten(parts) -> list:
            out = []
            for p in parts:
                if isinstance(p, list):
                    out.extend(flatten(p))
                elif isinstance(p, str) and p.strip():
                    out.append(p.strip())
            return out

        out[num] = {
            "short": short,
            "long": "; ".join(flatten(long_parts)),
        }
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--seeds", type=Path, default=DEFAULT_SEEDS)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = ap.parse_args()

    lex = json.loads(args.out.read_text(encoding="utf-8"))
    os_he = load_os(OS_HEBREW)
    os_el = load_os(OS_GREEK)
    seeds = json.loads(args.seeds.read_text(encoding="utf-8"))

    stats = {"he": [0, 0, 0], "el": [0, 0, 0]}  # entries, upgraded g, gi seeds
    seed_prefix = {"he": "h", "el": "g"}  # seeds use the Strong letter convention
    for lang, os_dict in (("he", os_he), ("el", os_el)):
        for num, entry in lex[lang].items():
            stats[lang][0] += 1
            os_entry = os_dict.get(num)
            if os_entry and os_entry["short"]:
                entry["g"] = os_entry["short"]
                stats[lang][1] += 1
            if os_entry and os_entry["long"]:
                entry["gl"] = os_entry["long"]
            seed = seeds.get(f"{seed_prefix[lang]}{num}")
            if seed:
                entry["gi"] = seed
                stats[lang][2] += 1

    args.out.write_text(json.dumps(lex, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print("== Lessico aggiornato ==")
    for lang, (n, up, gi) in stats.items():
        print(f"  {lang}: {n} voci, {up} g sostituite con OS short ({100 * up / n:.1f}%), {gi} seed gi")
    print(f"Written {args.out} ({args.out.stat().st_size / 1e6:.1f} MB)")

    # Sample comparison (old values from git HEAD, before the rewrite).
    print("\nEsempi (prima -> dopo):")
    import subprocess

    for lang, num in (("he", "1"), ("he", "216"), ("el", "25"), ("el", "3056"), ("he", "7225")):
        old_json = subprocess.run(
            ["git", "show", f"HEAD:composeApp/src/commonMain/composeResources/files/lexicon.json"],
            capture_output=True,
            text=True,
            check=True,
        ).stdout
        old = json.loads(old_json)[lang][num]["g"]
        print(f"  {lang}{num}:")
        print(f"    old g: {old[:90]}")
        print(f"    new g: {lex[lang][num]['g'][:90]}")


if __name__ == "__main__":
    main()
