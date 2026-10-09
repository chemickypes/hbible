#!/usr/bin/env python3
"""Map Diodati (1607, ed. Società Biblica Britannica e Forestiera 1877) verses
onto the standard Italian versification of the bundled originals/NR/R2.

The Diodati follows the Hebrew/KJV-flavoured versification that diverges from
the standard Italian one in a handful of spots (Exod 35:36 = std 36:1,
Deut 29:1 = std 28:69, Job 39:31-38 = std 38:39-39:5, Mark 9:51 = split of
std 9:50, ...): the same family of divergences already handled for R27 by
map_r27_versification.py. This tool is content-driven like that one, but the
source is the LaParola OSIS XML (no editorial H/G markers): each Diodati verse
is parsed to (book, chapter, verse) from its osisID and the text is cleaned of
the `<<title>>` markers (which become the newline-separated title of NR).

Mapping strategy: banded Needleman-Wunsch per book on token-Jaccard similarity
(reusing align_book from map_r27_versification), then the same phase logic:
identity pairs are immovable, gaps become "extras" kept at their own address
(verses without a standard counterpart, e.g. Diodati splits), and Diodati
verses that absorb two standard verses leave one standard slot unmapped
(merge, e.g. 2 Cor 1:24). MANUAL_OVERRIDES pins contested cases.

Result: bijection between Diodati verses and (a subset of) standard addresses,
monotone within each book, preserving Diodati reading order.

Output: tools/dio_verse_map.json  — remapped verses only
        tools/dio_verse_map_review.txt — all remaps and extras, for audit.

Usage:
  python3 tools/map_diodati_versification.py [--xml PATH] [--nr PATH]
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import FILES_DIR, normalize, tokens_of

# Sorgente OSIS di laparola.net (non versionata): LAPAROLA_DIR punta alla cartella "Testi".
DEFAULT_XML = Path(os.environ.get("LAPAROLA_DIR", "LaParola/Testi")) / "Bibbia/italiano/Diodati.xml"
DEFAULT_NR = FILES_DIR / "nuova_riveduta.json"
DEFAULT_OUT = Path(__file__).resolve().parent / "dio_verse_map.json"
DEFAULT_REVIEW = Path(__file__).resolve().parent / "dio_verse_map_review.txt"

# Diodati verses whose content does not match the versification shift the DP
# prefers (merges that leave a standard slot without a Diodati counterpart).
# Keyed by Diodati address, value = target standard address or None (extra).
MANUAL_OVERRIDES = {
    # Mark 9:50 = both sentences in NR; Diodati splits them across 9:50+51.
    # The DP swaps the pair (9:51 scores marginally higher against the merged
    # NR verse); pin the head sentence to its own address and let 9:51 be an
    # extra at its own address beyond the chapter end.
    (41, 9, 50): (41, 9, 50),
}

# Banded Needleman-Wunsch, adapted from map_r27_versification.align_book with a
# blended similarity (token Jaccard + char ratio): the 1607 wording is far
# enough from the NR that pure Jaccard misranks repetitive verses.
MIN_SCORE = 0.24
MIN_SCORE_RUN = 0.10
GAP = -0.06
BAD = -1.0
BAND = 64


def pair_score(a_norm: str, a_toks: set, b_norm: str, b_toks: set, blended: bool) -> float:
    if not a_toks or not b_toks:
        return BAD
    jac = len(a_toks & b_toks) / len(a_toks | b_toks)
    if not blended:
        return jac if jac >= MIN_SCORE else BAD
    from difflib import SequenceMatcher

    char = SequenceMatcher(None, a_norm, b_norm, autojunk=False).ratio()
    score = 0.5 * jac + 0.5 * char
    return score if score >= MIN_SCORE_RUN else BAD


def align_seq(a_items: list, b_items: list, blended: bool = False) -> list:
    """Banded NW alignment of [(key, norm, toks)] sequences; returns [(a_key, b_key_or_None)]."""
    n, m = len(a_items), len(b_items)
    dp = [[0.0] * (m + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        dp[i][0] = dp[i - 1][0] + GAP
    for j in range(1, m + 1):
        dp[0][j] = dp[0][j - 1] + GAP
    for i in range(1, n + 1):
        ra = a_items[i - 1]
        row = dp[i]
        prev = dp[i - 1]
        j_lo = max(1, i - BAND)
        j_hi = min(m, i + BAND)
        for j in range(1, m + 1):
            if j_lo <= j <= j_hi:
                s = pair_score(ra[1], ra[2], b_items[j - 1][1], b_items[j - 1][2], blended)
                diag = prev[j - 1] + s
                up = prev[j] + GAP
                left = row[j - 1] + GAP
                row[j] = diag if diag >= up and diag >= left else (up if up >= left else left)
            else:
                up = prev[j] + GAP
                left = row[j - 1] + GAP
                row[j] = up if up > left else left
    pairs = []
    i, j = n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and abs(i - j) <= BAND:
            s = pair_score(a_items[i - 1][1], a_items[i - 1][2], b_items[j - 1][1], b_items[j - 1][2], blended)
            if abs(dp[i][j] - (dp[i - 1][j - 1] + s)) < 1e-9:
                pairs.append((a_items[i - 1][0], b_items[j - 1][0]))
                i, j = i - 1, j - 1
                continue
        if i > 0 and abs(dp[i][j] - (dp[i - 1][j] + GAP)) < 1e-9:
            pairs.append((a_items[i - 1][0], None))
            i -= 1
        else:
            pairs.append((None, b_items[j - 1][0]))
            j -= 1
    pairs.reverse()
    return pairs

TITLE_RE = re.compile(r"<<([^<>]*)>>")


def clean_text(text: str) -> str:
    """`<<title>>body` becomes `title\\nbody` (the NR title convention)."""
    clean = TITLE_RE.sub(lambda m: m.group(1).strip() + "\n", text)
    clean = clean.replace("<<", "").replace(">>", "")
    return re.sub(r"[ \t]+", " ", clean).strip()


def load_diodati(path: Path) -> dict:
    """Returns {book_nr: [(key, norm, toks, clean)]} in reading order, plus names."""
    tree = ET.parse(path)
    root = tree.getroot()
    by_os = {}
    for v in root.iter("verse"):
        bk, c, vs = v.get("osisID").split(".")
        text = clean_text("".join(v.itertext()))
        by_os.setdefault(bk, []).append(((bk, int(c), int(vs)), text))
    return by_os


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--xml", type=Path, default=DEFAULT_XML)
    ap.add_argument("--nr", type=Path, default=DEFAULT_NR)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--review", type=Path, default=DEFAULT_REVIEW)
    args = ap.parse_args()

    nr = json.loads(args.nr.read_text(encoding="utf-8"))
    std = {}
    std_by_book = {}
    for v in nr["verses"]:
        norm = normalize(v["x"])
        slot = (v["b"], v["c"], v["v"])
        std[slot] = (v["x"], norm, tokens_of(norm))
        std_by_book.setdefault(v["b"], []).append((slot, norm, tokens_of(norm)))

    dio = load_diodati(args.xml)
    # OSIS book order == the 66-book numbering of the app.
    OSIS_ORDER = [
        "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1Sam", "2Sam",
        "1Kgs", "2Kgs", "1Chr", "2Chr", "Ezra", "Neh", "Esth", "Job", "Ps", "Prov",
        "Eccl", "Song", "Isa", "Jer", "Lam", "Ezek", "Dan", "Hos", "Joel", "Amos",
        "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
        "Matt", "Mark", "Luke", "John", "Acts", "Rom", "1Cor", "2Cor", "Gal", "Eph",
        "Phil", "Col", "1Thess", "2Thess", "1Tim", "2Tim", "Titus", "Phlm", "Heb",
        "Jas", "1Pet", "2Pet", "1John", "2John", "3John", "Jude", "Rev",
    ]
    assert sorted(dio) == sorted(OSIS_ORDER), "unexpected book set in Diodati XML"

    remaps = []
    extras = []
    unmapped_std = []
    anomalies = []
    identity_std = set()
    dio_names = {}

    for b, osis in enumerate(OSIS_ORDER, 1):
        dio_names[b] = osis
        entries = []
        for (bk, c, vs), text in dio[osis]:
            norm = normalize(text.replace("\n", " "))
            entries.append(((b, c, vs), norm, tokens_of(norm), text))
        std_slots = {k for k in std if k[0] == b}

        final = {}
        reserved = set()
        parked = set()

        if {k for (k, _, _, _) in entries} == std_slots:
            for key, norm, toks, text in entries:
                final[key] = key
                reserved.add(key)
                identity_std.add(key)
        else:
            # Phase 1: DP identities are immovable anchors.
            dp_map = {}
            dp_items = [(k, n, t) for (k, n, t, _x) in entries]
            for da, sb in align_seq(dp_items, std_by_book.get(b, [])):
                if da is not None:
                    dp_map[da] = sb
            for key, norm, toks, text in entries:
                if key in MANUAL_OVERRIDES:
                    continue
                if dp_map.get(key) == key:
                    final[key] = key
                    reserved.add(key)
                    identity_std.add(key)

            # Phase 2: manual overrides.
            for key, norm, toks, text in entries:
                if key not in final and key in MANUAL_OVERRIDES:
                    slot = MANUAL_OVERRIDES[key]
                    final[key] = slot
                    if slot is not None:
                        reserved.add(slot)
                        if slot == key:
                            identity_std.add(key)

            def std_order(slot):
                return (slot[1], slot[2])

            def next_free_extra(chapter):
                end = max(v for (bb, cc, v) in std_slots if cc == chapter)
                slot = (b, chapter, end + 1)
                while slot in reserved or slot in std_slots:
                    slot = (b, chapter, slot[2] + 1)
                return slot

            def close_run(run):
                """Solve a run of unanchored verses against the free standard
                slots between the surrounding anchors (content-driven DP)."""
                if not run:
                    return
                idx0 = next(i for i, e in enumerate(entries) if e[0] == run[0])
                idx1 = next(i for i, e in enumerate(entries) if e[0] == run[-1])
                prev_target = next(
                    (final[entries[i][0]] for i in range(idx0 - 1, -1, -1)
                     if entries[i][0] in final and entries[i][0] not in parked),
                    (b, 0, 0),
                )
                next_target = next(
                    (final[entries[i][0]] for i in range(idx1 + 1, len(entries))
                     if entries[i][0] in final and entries[i][0] not in parked),
                    (b + 1, 0, 0),
                )
                free = sorted(
                    (s for s in std_slots if s not in reserved and prev_target < s < next_target),
                    key=std_order,
                )
                run_set = set(run)
                run_items = [(k, n, t) for (k, n, t, _x) in entries if k in run_set]
                free_items = [(s, std[s][1], std[s][2]) for s in free]
                pairs = align_seq(run_items, free_items, blended=True)
                for da, sb in pairs:
                    if da is None or sb is None:
                        continue
                    final[da] = sb
                    reserved.add(sb)
                    if da == sb:
                        identity_std.add(da)
                for da, sb in pairs:
                    if sb is not None:
                        continue
                    # Unpaired Diodati verse: identity if the own address is a
                    # free standard slot inside the anchor range, extra if the
                    # own address is beyond the versification, else the split
                    # duplicate parks past the chapter end.
                    if da in std_slots and da not in reserved and prev_target < da < next_target:
                        final[da] = da
                        reserved.add(da)
                        identity_std.add(da)
                    elif da not in std_slots and da not in reserved:
                        final[da] = da
                        reserved.add(da)
                        extras.append(da)
                    else:
                        slot = next_free_extra(da[1])
                        final[da] = slot
                        reserved.add(slot)
                        parked.add(da)
                        extras.append(da)

            run = []
            for key, _, _, _ in entries:
                if key in final:
                    close_run(run)
                    run = []
                else:
                    run.append(key)
            close_run(run)

        # Assertions: bijection and monotonicity.
        if len(set(final.values())) != len(final.values()):
            print(f"!! book {b} {osis}: duplicate targets")
            sys.exit(1)
        if len(final) != len(entries):
            print(f"!! book {b} {osis}: {len(entries) - len(final)} verses unmapped")
            sys.exit(1)
        prev_t = None
        for key, slot in sorted(final.items(), key=lambda kv: (kv[0][1], kv[0][2])):
            if key in parked:
                continue
            if prev_t is not None and (slot[1], slot[2]) < (prev_t[1], prev_t[2]):
                anomalies.append((b, key, f"non-monotone: {slot} after {prev_t}"))
            prev_t = slot
        for key, slot in sorted(final.items()):
            if key != slot:
                remaps.append({"b": key[0], "c": key[1], "v": key[2], "tb": slot[0], "tc": slot[1], "tv": slot[2]})
            if key == slot and key not in std_slots and key not in extras:
                extras.append(key)

    mapped_std = {(r["tb"], r["tc"], r["tv"]) for r in remaps}
    identity_std = {k for k in identity_std if k in std}
    for slot in sorted(set(std) - mapped_std - identity_std):
        unmapped_std.append(slot)

    remaps.sort(key=lambda r: (r["b"], r["c"], r["v"]))

    lines = [f"== Diodati versification map: {len(remaps)} remapped verses, {len(extras)} extra verses, {len(unmapped_std)} unmapped standard slots =="]
    cur_book = None
    for r in remaps:
        if r["b"] != cur_book:
            cur_book = r["b"]
            lines.append(f"\n{r['b']} {OSIS_ORDER[r['b']-1]}:")
        lines.append(f"  {r['c']}:{r['v']}  ->  {r['tc']}:{r['tv']}")
    if extras:
        lines.append("\nExtra verses (no standard counterpart, kept at own address):")
        for (b, c, v) in extras:
            lines.append(f"  {OSIS_ORDER[b-1]} {c}:{v}")
    if unmapped_std:
        lines.append("\nStandard slots without Diodati text (merged into the previous verse):")
        for (b, c, v) in unmapped_std:
            lines.append(f"  {OSIS_ORDER[b-1]} {c}:{v}")
    report = "\n".join(lines)

    if anomalies:
        report += f"\n\n!! {len(anomalies)} ANOMALIES:\n"
        for b, key, msg in anomalies:
            report += f"  {dio_names.get(b, b)} {key[1]}:{key[2]} — {msg}\n"

    with args.review.open("w", encoding="utf-8") as f:
        f.write(f"Diodati versification audit — {len(remaps)} remaps, {len(extras)} extras, {len(unmapped_std)} unmapped std\n\n")
        dio_text = {(b, c, vs): text for b, osis in enumerate(OSIS_ORDER, 1) for (bk, c, vs), text in dio[osis]}
        for r in remaps:
            src = (r["b"], r["c"], r["v"])
            tgt = (r["tb"], r["tc"], r["tv"])
            f.write(f"--- {OSIS_ORDER[r['b']-1]} {r['c']}:{r['v']}  ->  {r['tc']}:{r['tv']}\n")
            f.write(f"  DIO: {dio_text.get(src, '?')}\n")
            f.write(f"  NR : {std.get(tgt, ('?',))[0]}\n\n")
        for (b, c, v) in extras:
            f.write(f"--- EXTRA {OSIS_ORDER[b-1]} {c}:{v} (no standard counterpart)\n")
            f.write(f"  DIO: {dio_text.get((b, c, v), '?')}\n\n")
        for (b, c, v) in unmapped_std:
            f.write(f"--- UNMAPPED STD {OSIS_ORDER[b-1]} {c}:{v} (merged into previous Diodati verse)\n")
            f.write(f"  NR : {std.get((b, c, v), ('?',))[0]}\n\n")

    print(report)
    print(f"\nAudit file: {args.review}")
    if anomalies:
        sys.exit(1)

    args.out.write_text(json.dumps(remaps, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"Written {args.out} ({len(remaps)} remaps)")


if __name__ == "__main__":
    main()
