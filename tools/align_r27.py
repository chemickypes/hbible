#!/usr/bin/env python3
"""Compute the R27 word alignment (ar27) for the bundled originals.json.

Extends the original-language table with a third per-word alignment column,
Riveduta 1927 -> original, using the same statistical pipeline as the source
app (HBible/tools/extract_originals.py): IBM Model 1 + EM trained on
(lemma sequence, normalized Italian token sequence) pairs, then greedy 1:1
alignment per verse with position prior and a relative-confidence filter.

Unlike the source tool, this script does NOT regenerate the original texts
from the XML sources: it reads the existing originals.json asset and only
ADDS the "ar27" array to every verse, leaving text/tr/lm/anr/ar2 untouched
(asserted byte-for-byte after writing).

Output: originals.json rewritten in place (backup at originals.json.bak);
        stats + verification report on stdout.

Usage:
  python3 tools/align_r27.py [--originals PATH] [--r27 PATH]
"""

import argparse
import json
from pathlib import Path

sys_path = Path(__file__).resolve().parent
import sys

sys.path.insert(0, str(sys_path))
from r27_common import FILES_DIR

DEFAULT_ORIGINALS = FILES_DIR / "originals.json"
DEFAULT_R27 = FILES_DIR / "riveduta_1927.json"

# ---------------------------------------------------------------------------
# Statistical word alignment (IBM Model 1 + EM) original -> Italian
# (ported verbatim from HBible/tools/extract_originals.py)
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--originals", type=Path, default=DEFAULT_ORIGINALS)
    ap.add_argument("--r27", type=Path, default=DEFAULT_R27)
    args = ap.parse_args()

    doc = json.loads(args.originals.read_text(encoding="utf-8"))
    r27 = json.loads(args.r27.read_text(encoding="utf-8"))
    r27_by_key = {(v["b"], v["c"], v["v"]): v["x"] for v in r27["verses"]}

    verses = doc["verses"]

    # Train + align per language.
    stats = {}
    for lang in ("he", "el"):
        flat = [v for v in verses if v["lang"] == lang]
        pairs = []
        meta = []
        for v in flat:
            key = (v["b"], v["c"], v["v"])
            it_text = r27_by_key.get(key)
            if not it_text:
                continue
            raw = it_text.split()
            norm = [norm_token(t) for t in raw]
            pairs.append((v["lm"].split(), norm))
            meta.append((v, raw, norm))
        print(f"align {lang}->r27: {len(pairs)} verses", flush=True)
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
            v["ar27"] = idxs
            aligned_words += sum(1 for i in idxs if i >= 0)
            total_words += len(idxs)
        stats[lang] = (aligned_words, total_words, len(meta))

    # Verses without an R27 counterpart keep an empty (all-unaligned) array of
    # the right length, so the reader can always index ar27 by word position.
    for v in verses:
        if "ar27" not in v:
            n = len(v["lm"].split()) if v.get("lm") else 0
            v["ar27"] = [-1] * n if n else []

    # --- Verification: existing fields must be untouched. ---
    backup = args.originals.with_suffix(".json.bak")
    if not backup.exists():
        backup.write_text(args.originals.read_text(encoding="utf-8"), encoding="utf-8")
    old = json.loads(backup.read_text(encoding="utf-8"))
    assert len(old["verses"]) == len(verses)
    for o, n in zip(old["verses"], verses):
        for field in ("b", "c", "v", "lang", "text", "tr", "lm", "anr", "ar2"):
            assert o.get(field) == n.get(field), f"field {field} changed at {(o['b'], o['c'], o['v'])}"
        # ar27 length must match the lemma channel
        if n.get("lm"):
            assert len(n["ar27"]) == len(n["lm"].split()), f"ar27 length mismatch at {(n['b'], n['c'], n['v'])}"

    args.originals.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    print("\n== R27 alignment stats ==")
    for lang, (aw, tw, nv) in stats.items():
        pct = 100 * aw / tw if tw else 0
        print(f"  {lang}: {nv} verses, {aw}/{tw} words aligned ({pct:.1f}%)")
    print(f"\nWritten {args.originals} ({args.originals.stat().st_size / 1e6:.1f} MB); backup at {backup}")


if __name__ == "__main__":
    main()
