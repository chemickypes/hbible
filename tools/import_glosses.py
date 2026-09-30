#!/usr/bin/env python3
"""Attach per-word contextual glosses from the bibbia-interlineare project.

Source: the 66 interlinear files (data/interlinear/{book}.json), one entry per
verse {"id": "BBCCCVVV", "verse": [{word, number, text(=EN gloss)}]}, plus the
AI-translated Italian glosses saved in data/custom_translations.json
(keys "BBCCCVVV_i" = contextual Italian gloss for word i, "h1234"/"g5678" =
Strong-keyed Italian gloss for the lexicon).

Each verse of the bundled originals.json gains two parallel channels:
  ge: List<String> — contextual EN gloss per original token ("" = none)
  gi: List<String> — contextual IT gloss per original token ("" = none,
                     currently only the custom_translations seeds)

Join strategy per verse: when the interlinear word count matches the original
token count, positions pair 1:1 and a gloss is attached where the Strong number
matches OR the normalized word form matches (editions differ in vowels,
punctuation, some Strong numbering). When counts differ, difflib alignment on
normalized words recovers the matching subsequences (Greek word-order/edition
differences, Hebrew maqaf splits). Unmatched positions get no gloss — nothing
is ever attached on weak evidence.

Strong-keyed Italian glosses are exported to tools/gloss_it_seeds.json for the
lexicon pipeline (B3), not to the verse channels.

Usage:
  python3 tools/import_glosses.py \
      [--interlinear DIR] [--translations PATH] [--originals PATH] [--seeds PATH]
"""

import argparse
import json
import re
import sys
from difflib import SequenceMatcher
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import BIBBIA_INTERLINEARE, FILES_DIR

DEFAULT_INTERLINEAR = BIBBIA_INTERLINEARE / "data" / "interlinear"
DEFAULT_TRANSLATIONS = BIBBIA_INTERLINEARE / "data" / "custom_translations.json"
DEFAULT_ORIGINALS = FILES_DIR / "originals.json"
DEFAULT_SEEDS = Path(__file__).resolve().parent / "gloss_it_seeds.json"

GR_PUNCT = ".,;:!?·«»\"'’‘()[]{}…—–"


def strip_he(s: str) -> str:
    return re.sub(r"[\u0591-\u05C7]", "", s)


def norm_word(lang: str, s: str) -> str:
    if lang == "he":
        return strip_he(s)
    return s.strip(GR_PUNCT).casefold()


def attach_positions(lang: str, words: list, otokens: list, ostrongs: list):
    """Returns {orig_position: interlinear_position} for confidently matched pairs."""
    n, m = len(words), len(otokens)
    out = {}
    if n == m:
        for i in range(n):
            s_ok = words[i]["number"].lstrip("hg") == ostrongs[i]
            w_ok = norm_word(lang, words[i]["word"]) == norm_word(lang, otokens[i])
            if s_ok or w_ok:
                out[i] = i
        return out
    # Count mismatch: align normalized word sequences, keep confident blocks.
    iw = [norm_word(lang, w["word"]) for w in words]
    ow = [norm_word(lang, t) for t in otokens]
    sm = SequenceMatcher(None, iw, ow, autojunk=False)
    for block in sm.get_matching_blocks():
        for k in range(block.size):
            i, j = block.a + k, block.b + k
            if j >= len(ostrongs):
                continue
            s_ok = words[i]["number"].lstrip("hg") == ostrongs[j]
            if s_ok or iw[i] == ow[j]:
                out[j] = i
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--interlinear", type=Path, default=DEFAULT_INTERLINEAR)
    ap.add_argument("--translations", type=Path, default=DEFAULT_TRANSLATIONS)
    ap.add_argument("--originals", type=Path, default=DEFAULT_ORIGINALS)
    ap.add_argument("--seeds", type=Path, default=DEFAULT_SEEDS)
    args = ap.parse_args()

    doc = json.loads(args.originals.read_text(encoding="utf-8"))
    verses = doc["verses"]
    by_key = {(v["b"], v["c"], v["v"]): v for v in verses}
    # Pre-edit snapshot for the final verification (git is the recovery path).
    before = [
        {f: v.get(f) for f in ("b", "c", "v", "lang", "text", "tr", "lm", "anr", "ar2", "ar27")}
        for v in verses
    ]

    # Interlinear index.
    inter = {}
    for f in sorted(args.interlinear.glob("*.json")):
        for entry in json.loads(f.read_text(encoding="utf-8")):
            key = (int(entry["id"][0:2]), int(entry["id"][2:5]), int(entry["id"][5:8]))
            inter[key] = entry["verse"]

    translations = json.loads(args.translations.read_text(encoding="utf-8"))
    contextual_it = {}  # key -> {array_index: italian gloss}
    lexicon_seeds = {}  # strong number -> italian gloss
    for k, val in translations.items():
        m = re.fullmatch(r"(\d{8})_(\d+)", k)
        if m:
            key = (int(m.group(1)[0:2]), int(m.group(1)[2:5]), int(m.group(1)[5:8]))
            contextual_it.setdefault(key, {})[int(m.group(2))] = val
        elif re.fullmatch(r"[hg]\d+", k):
            lexicon_seeds[k] = val

    stats = {"he": [0, 0, 0], "el": [0, 0, 0]}  # verses with glosses, glossed positions, total positions
    for v in verses:
        key = (v["b"], v["c"], v["v"])
        words = inter.get(key)
        n_tokens = len(v["lm"].split()) if v.get("lm") else 0
        ge = [""] * n_tokens
        gi = [""] * n_tokens
        if words and n_tokens:
            mapping = attach_positions(v["lang"], words, v["text"].split(), v["lm"].split())
            any_g = False
            for orig_pos, inter_pos in mapping.items():
                if orig_pos >= n_tokens or inter_pos >= len(words):
                    continue
                g = words[inter_pos].get("text", "").strip()
                if g:
                    ge[orig_pos] = g
                    any_g = True
            # Italian contextual glosses: custom_translations uses the
            # interlinear array index -> map it back to the original position.
            for inter_pos, it_gloss in contextual_it.get(key, {}).items():
                for orig_pos, ip in mapping.items():
                    if ip == inter_pos and orig_pos < n_tokens:
                        gi[orig_pos] = it_gloss
            lang = v["lang"]
            stats[lang][2] += n_tokens
            if any_g:
                stats[lang][0] += 1
                stats[lang][1] += sum(1 for g in ge if g)
        v["ge"] = ge
        v["gi"] = gi

    # --- Verification: existing fields untouched. ---
    for o, n in zip(before, verses):
        for field in ("b", "c", "v", "lang", "text", "tr", "lm", "anr", "ar2", "ar27"):
            assert o[field] == n.get(field), f"field {field} changed at {(n['b'], n['c'], n['v'])}"
        assert len(n["ge"]) == len(n["gi"]) == (len(n["lm"].split()) if n.get("lm") else 0)

    args.originals.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    args.seeds.write_text(json.dumps(lexicon_seeds, ensure_ascii=False, indent=1), encoding="utf-8")

    print("== Contextual glosses ==")
    for lang, (nv, np_, tp) in stats.items():
        print(f"  {lang}: {nv} versetti con gloss, {np_}/{tp} posizioni ({100 * np_ / tp if tp else 0:.1f}%)")
    print(f"Seeds lessico IT: {len(lexicon_seeds)} voci -> {args.seeds}")
    print(f"Written {args.originals} ({args.originals.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
