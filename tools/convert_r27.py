#!/usr/bin/env python3
"""Convert the Riveduta 1927 source into the bundled BibleDoc asset format.

Reads bibbia-interlineare-project/data/riveduta.json (66 books, verse records
{chapter, verse, name, text}) and the versification map produced by
map_r27_versification.py, and writes riveduta_1927.json in the same format as
nuova_riveduta.json / riveduta_2020.json:

  {"meta": {...}, "books": [{n, name, abbr, chapters, verses}],
   "verses": [{b, c, v, x}]}

The verse TEXT is preserved verbatim except for two cleanups that never touch
the wording:
  - editorial versification markers "(Hc-v)/(Gc-v)" are removed;
  - runs of whitespace are collapsed and the text trimmed.

Verse ADDRESSES are renumbered onto the standard Italian versification using
tools/r27_verse_map.json (the text of R27 6:1 is stored at standard 5:20 etc.);
three verses (2 Sam 20:26, Ps 13:6, 2 Cor 13:14) keep their own address beyond
the standard chapter end. A note in meta.intro documents this.

Usage:
  python3 tools/convert_r27.py [--riveduta PATH] [--map PATH] [--out PATH]
"""

import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import DEFAULT_R27, FILES_DIR, split_marker

DEFAULT_MAP = Path(__file__).resolve().parent / "r27_verse_map.json"
DEFAULT_OUT = FILES_DIR / "riveduta_1927.json"
DEFAULT_NR = FILES_DIR / "nuova_riveduta.json"

# Italian-standard book abbreviations (same convention as the NR asset).
BOOK_ABBR = {
    1: "Gen", 2: "Es", 3: "Lv", 4: "Nm", 5: "Dt", 6: "Gs", 7: "Gdc", 8: "Rt",
    9: "1Sam", 10: "2Sam", 11: "1Re", 12: "2Re", 13: "1Cr", 14: "2Cr",
    15: "Esd", 16: "Ne", 17: "Est", 18: "Gio", 19: "Sal", 20: "Pr", 21: "Qo",
    22: "Ct", 23: "Is", 24: "Ger", 25: "Lam", 26: "Ez", 27: "Dn", 28: "Os",
    29: "Gl", 30: "Am", 31: "Ob", 32: "Gio", 33: "Mi", 34: "Na", 35: "Ab",
    36: "Sof", 37: "Ag", 38: "Zc", 39: "Mal", 40: "Mt", 41: "Mc", 42: "Lc",
    43: "Gv", 44: "At", 45: "Rm", 46: "1Cor", 47: "2Cor", 48: "Gal", 49: "Ef",
    50: "Fil", 51: "Col", 52: "1Ts", 53: "2Ts", 54: "1Tm", 55: "2Tm",
    56: "Tt", 57: "Fm", 58: "Eb", 59: "Gc", 60: "1Pt", 61: "2Pt", 62: "1Gv",
    63: "2Gv", 64: "3Gv", 65: "Gd", 66: "Ap",
}

META = {
    "name": "Riveduta 1927",
    "abbr": "R27",
    "description": "La Sacra Bibbia nella versione Riveduta (1927), revisione di Giovanni Luzzi sulla Diodati",
    "publisher": "Società Biblica Britannica e Forestiera",
    "year": "1927",
    "copyright": "Pubblico dominio",
    "lang": "it",
    "intro": (
        "\nLa Riveduta è il frutto della revisione della Bibbia di Giovanni Diodati (1607) "
        "curata da un comitato di studiosi coordinato da Giovanni Luzzi, pubblicata nel 1927. "
        "È il testo che ha fatto da base alla Nuova Riveduta ed è oggi di pubblico dominio.\n"
        "Nota editoriale: la numerazione dei versetti è stata ricondotta alla versificazione "
        "italiana standard usata dalle altre versioni dell'app (in circa 20 punti la tradizione "
        "Diodati numerava diversamente, ad es. Levitico 6:24-30 = 5:20-26). Il testo è riportato "
        "integralmente; i rimandi editoriali alla numerazione ebraica/greca presenti nell'edizione "
        "cartacea sono stati rimossi."
    ),
}


def clean_text(text: str) -> str:
    clean, _, _ = split_marker(text)
    return re.sub(r"\s+", " ", clean).strip()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--riveduta", type=Path, default=DEFAULT_R27)
    ap.add_argument("--map", type=Path, default=DEFAULT_MAP)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--nr", type=Path, default=DEFAULT_NR)
    args = ap.parse_args()

    r27 = json.loads(args.riveduta.read_text(encoding="utf-8"))
    remaps = {(r["b"], r["c"], r["v"]): (r["tb"], r["tc"], r["tv"]) for r in json.loads(args.map.read_text(encoding="utf-8"))}
    nr = json.loads(args.nr.read_text(encoding="utf-8"))
    std_slots = {(v["b"], v["c"], v["v"]) for v in nr["verses"]}

    books = []
    verses = []
    seen = set()
    for bk in r27["books"]:
        n = bk["nr"]
        book_verses = 0
        for ch in bk["chapters"]:
            for vs in ch["verses"]:
                src = (n, ch["chapter"], vs["verse"])
                tgt = remaps.get(src, src)
                assert tgt not in seen, f"duplicate target {tgt} for source {src}"
                seen.add(tgt)
                verses.append({"b": tgt[0], "c": tgt[1], "v": tgt[2], "x": clean_text(vs["text"])})
                book_verses += 1
        books.append(
            {
                "n": n,
                "name": bk["name"].strip(),
                "abbr": BOOK_ABBR[n],
                "chapters": len(bk["chapters"]),
                "verses": book_verses,
            }
        )

    verses.sort(key=lambda v: (v["b"], v["c"], v["v"]))
    doc = {"meta": META, "books": books, "verses": verses}
    args.out.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    # Report: coverage against the standard versification.
    covered = sum(1 for v in verses if (v["b"], v["c"], v["v"]) in std_slots)
    beyond = len(verses) - covered
    print(f"Versetti totali R27: {len(verses)} (di cui {covered} su indirizzi standard, {beyond} extra)")
    print(f"Slot standard coperti: {covered}/{len(std_slots)}")
    print(f"Scritto {args.out} ({args.out.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
