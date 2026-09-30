#!/usr/bin/env python3
"""Map Riveduta 1927 verses onto the standard Italian versification.

The R27 source (bibbia-interlineare-project/data/riveduta.json) follows a
KJV/Hebrew-flavoured versification that diverges from the standard Italian one
(the versification of the bundled originals/NR/R2) in ~20 spots (e.g. Lev
6:24-30 = standard 5:20-26, Job 41 = standard 40:25-41:26, Ezek 20:45-49 =
standard 21:1-5).

The app loads the original-language verse by the same (b, c, v) coordinates as
the Italian verse, so R27 must be renumbered onto the standard versification.
The TEXT is never modified, only the verse address is remapped.

Mapping strategy (content decides; hints only help):
  1. Banded global DP alignment (Needleman-Wunsch) per book on token-Jaccard
     similarity — recovers monotone shift runs between the 1927 and 2006
     editions. DP-identity results are immovable (99% of verses).
  2. Editorial markers "(Hc-v)/(Gc-v)" in the R27 text are ADDITIONAL
     candidates, not ground truth: they follow a concordance Hebrew/Greek
     numbering that usually — but not always (e.g. Gen 26:35, Amos 3:14) —
     matches the standard Italian address. A marker wins only through its
     content score, or when it agrees with the DP.
  3. Mid-verse markers mark a verse that merges two standard verses: identity.
  4. MANUAL_OVERRIDES for textual variants (Matt 21:29, Luke 9:44) and
     merged-verse display choice (first slot).

Result: bijection between R27 verses and (a subset of) standard addresses,
monotone within each book, preserving R27 reading order. Verses beyond the
standard chapter end (e.g. Ps 13:6, 2 Sam 20:26, 2 Cor 13:14) keep their own
address as "extra" verses — readable, but without original-language pairing.

Output: tools/r27_verse_map.json  — remapped verses only
        ({"b":3,"c":6,"v":1,"tb":3,"tc":5,"tv":20}, ...); every other verse is
        identity or an extra verse at its own address.
        tools/r27_verse_map_review.txt — all remaps and extras, for audit.

Usage:
  python3 tools/map_r27_versification.py [--riveduta PATH] [--nr PATH]
"""

import argparse
import json
import sys
from difflib import SequenceMatcher
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import DEFAULT_R27, FILES_DIR, normalize, split_marker, tokens_of

DEFAULT_NR = FILES_DIR / "nuova_riveduta.json"
DEFAULT_OUT = Path(__file__).resolve().parent / "r27_verse_map.json"
DEFAULT_REVIEW = Path(__file__).resolve().parent / "r27_verse_map_review.txt"

# Cases where the statistical alignment misreads a textual variant or a merged
# verse; keyed by R27 address, value = target standard address.
MANUAL_OVERRIDES = {
    # Textual variant (which son answers what), not a versification shift.
    (40, 21, 29): (40, 21, 29),
    # R27 9:44 splits std 9:43-tail + 9:44; the saying itself is std 9:44.
    (42, 9, 44): (42, 9, 44),
    # Merged verses (two standard verses in one R27 verse) -> first slot.
    (1, 11, 12): (1, 11, 12),   # Gen 11:12+11:13
    (11, 22, 43): (11, 22, 43), # 1Kgs 22:43+22:44
    (64, 1, 14): (64, 1, 14),   # 3John 1:14+1:15
}

MIN_SCORE = 0.20  # DP pair floor (token Jaccard); true pairs >= ~0.24, noise <= ~0.17
GAP = -0.06       # mild gap penalty: allows real shifts, discourages noise
BAD = -1.0        # effective score for hopeless pairs
BAND = 64         # max |i-j| offset in the DP


def dp_score(a_toks: set, b_toks: set) -> float:
    if not a_toks or not b_toks:
        return BAD
    jac = len(a_toks & b_toks) / len(a_toks | b_toks)
    return jac if jac >= MIN_SCORE else BAD


def align_book(r27_verses: list, std_verses: list) -> list:
    """Banded Needleman-Wunsch alignment; items are (key, norm, toks).

    Returns list of (r27_key, std_key_or_None)."""
    n, m = len(r27_verses), len(std_verses)
    dp = [[0.0] * (m + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        dp[i][0] = dp[i - 1][0] + GAP
    for j in range(1, m + 1):
        dp[0][j] = dp[0][j - 1] + GAP
    for i in range(1, n + 1):
        ra = r27_verses[i - 1]
        row = dp[i]
        prev = dp[i - 1]
        j_lo = max(1, i - BAND)
        j_hi = min(m, i + BAND)
        for j in range(1, m + 1):
            if j_lo <= j <= j_hi:
                s = dp_score(ra[2], std_verses[j - 1][2])
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
            s = dp_score(r27_verses[i - 1][2], std_verses[j - 1][2])
            if abs(dp[i][j] - (dp[i - 1][j - 1] + s)) < 1e-9:
                pairs.append((r27_verses[i - 1][0], std_verses[j - 1][0]))
                i, j = i - 1, j - 1
                continue
        if i > 0 and abs(dp[i][j] - (dp[i - 1][j] + GAP)) < 1e-9:
            pairs.append((r27_verses[i - 1][0], None))
            i -= 1
        else:
            pairs.append((None, std_verses[j - 1][0]))
            j -= 1
    pairs.reverse()
    return pairs


def blended(a_norm: str, a_toks: set, b_norm: str, b_toks: set) -> float:
    if not a_toks or not b_toks:
        return 0.0
    jac = len(a_toks & b_toks) / len(a_toks | b_toks)
    char = SequenceMatcher(None, a_norm, b_norm).ratio()
    return 0.5 * jac + 0.5 * char


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--riveduta", type=Path, default=DEFAULT_R27)
    ap.add_argument("--nr", type=Path, default=DEFAULT_NR)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--review", type=Path, default=DEFAULT_REVIEW)
    args = ap.parse_args()

    r27 = json.loads(args.riveduta.read_text(encoding="utf-8"))
    nr = json.loads(args.nr.read_text(encoding="utf-8"))

    std = {}  # slot -> (text, norm, toks)
    std_by_book = {}
    for v in nr["verses"]:
        norm = normalize(v["x"])
        slot = (v["b"], v["c"], v["v"])
        std[slot] = (v["x"], norm, tokens_of(norm))
        std_by_book.setdefault(v["b"], []).append((slot, norm, tokens_of(norm)))

    remaps = []
    extras = []  # verses kept at their own address beyond the standard end
    anomalies = []
    r27_names = {}

    for bk in r27["books"]:
        b = bk["nr"]
        r27_names[b] = bk["name"]
        seq = []  # (key, norm, toks, marker, marker_pos, clean_text)
        for ch in bk["chapters"]:
            for vs in ch["verses"]:
                clean, marker, mpos = split_marker(vs["text"])
                norm = normalize(clean)
                seq.append(((b, ch["chapter"], vs["verse"]), norm, tokens_of(norm), marker, mpos, clean))
        std_slots = {k for k in std if k[0] == b}

        def score_of(key_norm, key_toks, slot):
            t = std.get(slot)
            if t is None:
                return 0.0
            return blended(key_norm, key_toks, t[1], t[2])

        dp_map = {}
        for ra, sb in align_book([(k, n, t) for (k, n, t, _, _, _) in seq], std_by_book.get(b, [])):
            if ra is not None:
                dp_map[ra] = sb

        final = {}
        reserved = set()
        r27_keys = {k for (k, _, _, _, _, _) in seq}

        # Phase 1: immovable identities (DP identity or mid-verse merge marker).
        for key, norm, toks, marker, mpos, clean in seq:
            if key in MANUAL_OVERRIDES:
                continue
            if mpos > 0:
                final[key] = key
                reserved.add(key)
                continue
            if dp_map.get(key) == key:
                final[key] = key
                reserved.add(key)

        # Phase 2: everything else, in reading order.
        for key, norm, toks, marker, mpos, clean in seq:
            if key in final:
                continue
            if key in MANUAL_OVERRIDES:
                cands = [MANUAL_OVERRIDES[key]]
            else:
                cands = []
                dp = dp_map.get(key)
                if dp is not None and dp != key and dp not in cands:
                    cands.append(dp)
                if marker is not None and mpos == 0:
                    mt = (b, marker[0], marker[1])
                    if mt in std_slots and mt not in cands:
                        cands.append(mt)
            scored = sorted(
                ((score_of(norm, toks, slot), -i, slot) for i, slot in enumerate(cands)),
                reverse=True,
            )
            placed = False
            for s, _, slot in scored:
                if slot in reserved:
                    continue
                # A slot whose natural owner (the R27 verse at that address) is
                # still unplaced and has no better option (DP gap) belongs to
                # its owner: this implements the merged-verse pattern, where
                # an R27 verse absorbing the head of the next standard verse
                # must not steal that verse's address (e.g. Lev 18:1-2).
                if (
                    slot != key
                    and slot in r27_keys
                    and slot not in final
                    and slot not in MANUAL_OVERRIDES
                    and dp_map.get(slot) is None
                ):
                    continue
                final[key] = slot
                reserved.add(slot)
                placed = True
                break
            if not placed:
                if key not in reserved:
                    # Own address free: standard slot, or extra beyond the end.
                    final[key] = key
                    reserved.add(key)
                    if key not in std_slots:
                        extras.append(key)
                else:
                    anomalies.append((b, key, "no available slot (contested and own address taken)"))

        # Assertions: bijection and monotonicity.
        if len(set(final.values())) != len(final.values()):
            print(f"!! book {b} {r27_names[b]}: duplicate targets")
            sys.exit(1)
        if len(final) != len(seq):
            print(f"!! book {b} {r27_names[b]}: {len(seq) - len(final)} verses unmapped")
            sys.exit(1)
        prev_t = None
        for key, slot in sorted(final.items(), key=lambda kv: (kv[0][1], kv[0][2])):
            if prev_t is not None and (slot[1], slot[2]) < (prev_t[1], prev_t[2]):
                anomalies.append((b, key, f"non-monotone: {slot} after {prev_t}"))
            prev_t = slot
        for key, slot in sorted(final.items()):
            if key != slot:
                remaps.append({"b": key[0], "c": key[1], "v": key[2], "tb": slot[0], "tc": slot[1], "tv": slot[2]})

    remaps.sort(key=lambda r: (r["b"], r["c"], r["v"]))

    lines = [f"== R27 versification map: {len(remaps)} remapped verses, {len(extras)} extra verses =="]
    cur_book = None
    for r in remaps:
        if r["b"] != cur_book:
            cur_book = r["b"]
            lines.append(f"\n{r['b']} {r27_names[r['b']]}:")
        lines.append(f"  {r['c']}:{r['v']}  ->  {r['tc']}:{r['tv']}")
    if extras:
        lines.append("\nExtra verses (kept at own address, beyond the standard chapter):")
        for (b, c, v) in extras:
            lines.append(f"  {r27_names[b]} {c}:{v}")
    report = "\n".join(lines)

    if anomalies:
        report += f"\n\n!! {len(anomalies)} ANOMALIES:\n"
        for b, key, msg in anomalies:
            report += f"  {r27_names.get(b, b)} {key[1]}:{key[2]} — {msg}\n"

    # Audit file: every remap and extra with both texts.
    with args.review.open("w", encoding="utf-8") as f:
        f.write(f"R27 versification audit — {len(remaps)} remaps, {len(extras)} extras\n\n")
        for r in remaps:
            src = (r["b"], r["c"], r["v"])
            tgt = (r["tb"], r["tc"], r["tv"])
            r27t = next(
                (
                    split_marker(vs["text"])[0]
                    for ch in next(x for x in r27["books"] if x["nr"] == r["b"])["chapters"]
                    if ch["chapter"] == r["c"]
                    for vs in ch["verses"]
                    if vs["verse"] == r["v"]
                ),
                "?",
            )
            f.write(f"--- {r27_names[r['b']]} {r['c']}:{r['v']}  ->  {r['tc']}:{r['tv']}\n")
            f.write(f"  R27: {r27t}\n")
            f.write(f"  NR : {std.get(tgt, ('?',))[0]}\n\n")
        for (b, c, v) in extras:
            r27t = next(
                (
                    split_marker(vs["text"])[0]
                    for ch in next(x for x in r27["books"] if x["nr"] == b)["chapters"]
                    if ch["chapter"] == c
                    for vs in ch["verses"]
                    if vs["verse"] == v
                ),
                "?",
            )
            f.write(f"--- EXTRA {r27_names[b]} {c}:{v} (no standard counterpart)\n")
            f.write(f"  R27: {r27t}\n\n")

    print(report)
    print(f"\nAudit file: {args.review}")
    if anomalies:
        sys.exit(1)

    args.out.write_text(json.dumps(remaps, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"Written {args.out} ({len(remaps)} remaps)")


if __name__ == "__main__":
    main()
