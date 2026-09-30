#!/usr/bin/env python3
"""Compute the Diodati word alignment (ar_dio) for the bundled originals.json.

Extends the original-language table with a per-word alignment column,
Diodati -> original, using the same statistical pipeline as align_r27.py
(ported from HBible/tools/extract_originals.py): IBM Model 1 + EM trained on
(lemma sequence, normalized Italian token sequence) pairs, then greedy 1:1
alignment per verse with position prior and a relative-confidence filter.

This script does NOT regenerate the original texts: it reads the existing
originals.json asset and only ADDS the "ar_dio" array to every verse, leaving
text/tr/lm/anr/ar2/ar27/ge/gi untouched (asserted byte-for-byte after writing).

Output: originals.json rewritten in place;
        stats + verification report on stdout.

Usage:
  python3 tools/align_diodati.py [--originals PATH] [--diodati PATH]
"""

import argparse
import json
import sys
from pathlib import Path

sys_path = Path(__file__).resolve().parent
sys.path.insert(0, str(sys_path))
from r27_common import FILES_DIR

DEFAULT_ORIGINALS = FILES_DIR / "originals.json"
DEFAULT_DIO = FILES_DIR / "diodati.json"

PUNCT = ".,;:!?«»\"'()[]{}…—–-’‘“”"

# Italian function words: never meaningful as 1:1 word alignments
STOPWORDS = {
    "il", "lo", "la", "i", "gli", "le", "un", "uno", "una", "e", "ed", "o", "od",
    "a", "ad", "di", "del", "dello", "della", "dei", "delle", "degli", "dal",
    "dalla", "dal", "che", "in", "su", "con", "per", "tra", "fra", "non", "si",
    "ci", "vi", "mi", "ti", "li", "ne", "è", "sono", "era", "furono", "fu",
    "sia", "ha", "hanno", "se", "ma", "perché", "quando", "come", "dove", "più",
    "molto", "tutto", "tutta", "tutti", "tutte", "questo", "questa", "questi",
    "queste", "quello", "quella", "quelli", "quelle", "suo", "sua", "suoi",
    "sue", "loro", "mio", "tuo", "nostro", "vostro", "io", "tu", "egli", "ella",
    "lei", "noi", "voi", "essi", "esse", "me", "te", "nel", "nella", "nei",
    "nelle", "col", "coi", "al", "alla", "ai", "alle", "allo", "eccomi",
    "alquanto",
}


def norm_token(tok: str) -> str:
    return tok.strip(PUNCT).lower()


def train_model1(pairs: list, iters: int = 8) -> dict:
    """pairs: [(lemma_list, [norm_italian_tokens])]. Returns {(lemma, it): prob}."""
    co = {}
    for lems, its in pairs:
        ue = set(lems)
        for f in its:
            for e in ue:
                co.setdefault(e, set()).add(f)
    t = {}
    for e, fs in co.items():
        p = 1.0 / len(fs)
        for f in fs:
            t[(e, f)] = p
    co.clear()
    for _ in range(iters):
        count = {}
        total = {}
        for lems, its in pairs:
            ue = set(lems)
            for f in its:
                z = 0.0
                for e in ue:
                    z += t.get((e, f), 0.0)
                if z <= 0:
                    continue
                for e in ue:
                    c = t.get((e, f), 0.0) / z
                    key = (e, f)
                    count[key] = count.get(key, 0.0) + c
                    total[e] = total.get(e, 0.0) + c
        t = {k: v / total[k[0]] for k, v in count.items() if total[k[0]] > 0}
    return t


def align_verse(
    lems: list,
    raw_tokens: list,
    norm_tokens: list,
    t: dict,
    best: dict,
    min_prob: float = 0.005,
):
    """Greedy 1:1 alignment with position prior + relative-confidence filter.
    Returns [-1 | italian token index] per original word. A pair is kept only if
    the Italian token is the lemma's dominant translation (p / max_p >= 0.35),
    which is scale-free (works for hapaxes too). `best` = max non-stopword
    probability per lemma, precomputed once per model."""
    n_le, n_it = len(lems), len(norm_tokens)
    cand = []
    for i, e in enumerate(lems):
        if e == "-" or e == "853" or best.get(e, 0.0) <= 0:  # missing lemmas + object marker
            continue
        pe = (i + 1) / (n_le + 1)
        for j, f in enumerate(norm_tokens):
            if not f or f in STOPWORDS:
                continue
            p = t.get((e, f), 0.0)
            if p < min_prob:
                continue
            conf = p / best[e]
            if conf < 0.35:
                continue
            pf = (j + 1) / (n_it + 1)
            dist = abs(pe - pf)
            prior = 1.0 / (1.0 + (dist / 0.15) ** 2)
            cand.append((conf * prior, conf, i, j))
    cand.sort(key=lambda x: (-x[0], -x[1]))
    used_i, used_j = set(), set()
    out = [-1] * len(lems)
    for score, conf, i, j in cand:
        if i in used_i or j in used_j:
            continue
        used_i.add(i)
        used_j.add(j)
        out[i] = j
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--originals", type=Path, default=DEFAULT_ORIGINALS)
    ap.add_argument("--diodati", type=Path, default=DEFAULT_DIO)
    args = ap.parse_args()

    original_bytes = args.originals.read_bytes()
    doc = json.loads(original_bytes.decode("utf-8"))
    dio = json.loads(args.diodati.read_text(encoding="utf-8"))
    dio_by_key = {(v["b"], v["c"], v["v"]): v["x"] for v in dio["verses"]}

    verses = doc["verses"]

    # Train + align per language.
    stats = {}
    for lang in ("he", "el"):
        flat = [v for v in verses if v["lang"] == lang]
        pairs = []
        meta = []
        for v in flat:
            key = (v["b"], v["c"], v["v"])
            it_text = dio_by_key.get(key)
            if not it_text:
                continue
            raw = it_text.replace("\n", " ").split()
            norm = [norm_token(t) for t in raw]
            pairs.append((v["lm"].split(), norm))
            meta.append((v, raw, norm))
        print(f"align {lang}->diodati: {len(pairs)} verses", flush=True)
        model = train_model1(pairs)
        pairs.clear()
        best = {}
        for (e, f), p in model.items():
            if f in STOPWORDS or not f:
                continue
            if p > best.get(e, 0.0):
                best[e] = p
        aligned_words = 0
        total_words = 0
        for v, raw, norm in meta:
            idxs = align_verse(v["lm"].split(), raw, norm, model, best)
            v["ar_dio"] = idxs
            aligned_words += sum(1 for i in idxs if i >= 0)
            total_words += len(idxs)
        stats[lang] = (aligned_words, total_words, len(meta))

    # Verses without a Diodati counterpart keep an empty (all-unaligned) array
    # of the right length, so the reader can always index ar_dio by position.
    for v in verses:
        if "ar_dio" not in v:
            n = len(v["lm"].split()) if v.get("lm") else 0
            v["ar_dio"] = [-1] * n if n else []

    # --- Verification: existing fields must be untouched. ---
    old = json.loads(original_bytes.decode("utf-8"))
    assert len(old["verses"]) == len(verses)
    for o, n in zip(old["verses"], verses):
        for field in ("b", "c", "v", "lang", "text", "tr", "lm", "anr", "ar2", "ar27", "ge", "gi"):
            assert o.get(field) == n.get(field), f"field {field} changed at {(o['b'], o['c'], o['v'])}"
        if n.get("lm"):
            assert len(n["ar_dio"]) == len(n["lm"].split()), f"ar_dio length mismatch at {(n['b'], n['c'], n['v'])}"

    args.originals.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    print("\n== Diodati alignment stats ==")
    for lang, (aw, tw, nv) in stats.items():
        pct = 100 * aw / tw if tw else 0
        print(f"  {lang}: {nv} verses, {aw}/{tw} words aligned ({pct:.1f}%)")
    print(f"\nWritten {args.originals} ({args.originals.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
