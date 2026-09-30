#!/usr/bin/env python3
"""Fill the Italian contextual glosses (gi) of the NT from the interlinear dump.

Source: Downloads/interlineare.sql — pipe-delimited, one row per NT word:
  book(1-27)|chapter|verse|position|form|lemma|it1|it2|it3
Three editorial Italian channels; this import uses column 1 (best match with
the Nuova Riveduta, the app's default translation, verified 98% token overlap).

The gi channel of every NT verse of the bundled originals.json gains the
Italian gloss of the aligned word. Rules (same discipline as import_glosses.py):
  - positions pair 1:1 when counts match; otherwise difflib blocks on
    normalized Greek forms recover confident subalignments;
  - a gloss is attached only where the normalized FORM matches (the dump has
    no Strong numbers — form evidence only);
  - '-', '>', '<' (inheritance/absorption markers) get no gloss: the Italian
    of those Greek tokens is already inside an adjacent translated word;
  - <sup>n</sup> word-order markers stripped; {supplied} words stripped (they
    are not renderings of this Greek token);
  - existing curated gi entries (custom_translations seeds) are NEVER
    overwritten: the dump fills only empty positions.

Usage:
  python3 tools/import_glosses_it_nt.py [--dump PATH] [--originals PATH]
"""

import argparse
import json
import re
import sys
from difflib import SequenceMatcher
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import FILES_DIR

DEFAULT_DUMP = Path.home() / "Downloads" / "interlineare.sql"
DEFAULT_ORIGINALS = FILES_DIR / "originals.json"

GR_PUNCT = ".,;:!?·«»\"'’‘()[]{}…—–"


def norm_el(s: str) -> str:
    return s.strip(GR_PUNCT).casefold()


def clean_it(s: str) -> str:
    """Editorial markers out: <sup>n</sup>, {supplied}, absorption markers."""
    s = re.sub(r"<sup>\d*</sup>", "", s)
    s = re.sub(r"\{[^}]*\}", "", s)
    s = s.strip()
    if not s or s in ("-", ">") or s.startswith(">") or s.startswith("<"):
        return ""
    return re.sub(r"\s+", " ", s)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    ap.add_argument("--originals", type=Path, default=DEFAULT_ORIGINALS)
    args = ap.parse_args()

    doc = json.loads(args.originals.read_text(encoding="utf-8"))
    verses = doc["verses"]
    before = [
        {f: v.get(f) for f in ("b", "c", "v", "lang", "text", "tr", "lm", "anr", "ar2", "ar27", "ge")}
        for v in verses
    ]

    # Dump index: (1-27, c, v) -> [(position, form, italian)] sorted by position.
    dump = {}
    with args.dump.open(encoding="utf-8") as fh:
        for line in fh:
            p = line.rstrip("\n").rstrip("|").split("|")
            if len(p) != 9:
                continue
            b, c, v, w, form, lemma, i1 = p[0], p[1], p[2], p[3], p[4], p[5], p[6]
            it = clean_it(i1)
            dump.setdefault((int(b), int(c), int(v)), []).append((int(w), form, it))
    for k in dump:
        dump[k].sort(key=lambda x: x[0])

    stats = {"versetti": 0, "curati": 0, "nuove": 0, "vuote": 0, "posizioni": 0}
    samples = []
    for v in verses:
        if v["lang"] != "el" or not (40 <= v["b"] <= 66):
            continue
        key = (v["b"] - 39, v["c"], v["v"])
        rows = dump.get(key)
        n_tokens = len(v["lm"].split()) if v.get("lm") else 0
        if not rows or not n_tokens:
            continue
        gi = v["gi"]
        assert len(gi) == n_tokens, f"gi channel mismatch at {key}"
        otokens = v["text"].split()
        dforms = [norm_el(r[1]) for r in rows]
        dits = [r[2] for r in rows]
        oforms = [norm_el(t) for t in otokens]

        mapping = {}
        if len(rows) == n_tokens:
            for i in range(n_tokens):
                if dforms[i] == oforms[i]:
                    mapping[i] = i
        else:
            sm = SequenceMatcher(None, dforms, oforms, autojunk=False)
            for block in sm.get_matching_blocks():
                for k in range(block.size):
                    i, j = block.a + k, block.b + k
                    if dforms[i] == oforms[j]:
                        mapping[j] = i

        touched = False
        for orig_pos, dump_pos in mapping.items():
            if orig_pos >= n_tokens:
                continue
            it = dits[dump_pos]
            if not it:
                continue
            stats["posizioni"] += 1
            if gi[orig_pos]:
                stats["curati"] += 1  # curated seed: kept
            else:
                gi[orig_pos] = it
                stats["nuove"] += 1
                touched = True
        stats["vuote"] += sum(1 for g in gi if not g)
        if touched:
            stats["versetti"] += 1
        if (v["b"], v["c"], v["v"]) in ((40, 1, 1), (43, 3, 16), (66, 21, 21)):
            samples.append((key, v["text"].split()[:8], gi[:8]))

    # --- Verification: everything except gi untouched. ---
    for o, n in zip(before, verses):
        for field, val in o.items():
            assert val == n.get(field), f"field {field} changed at {(n['b'], n['c'], n['v'])}"
        if n["lang"] == "el" and 40 <= n["b"] <= 66:
            assert len(n["gi"]) == (len(n["lm"].split()) if n.get("lm") else 0)

    args.originals.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    print(f"Versetti NT con nuove gloss: {stats['versetti']}")
    print(f"Posizioni riempite dal dump: {stats['nuove']} (curate conservate: {stats['curati']}, ancora vuote: {stats['vuote']})")
    for key, greek, gi in samples:
        print(f"\n  {key}:")
        for g, it in zip(greek, gi):
            print(f"    {g:<20} {it or '·'}")
    print(f"\nWritten {args.originals} ({args.originals.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
