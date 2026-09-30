#!/usr/bin/env python3
"""Convert the Diodati source into the bundled BibleDoc asset format.

Reads the LaParola OSIS XML of the Diodati translation (1607, ed. Società
Biblica Britannica e Forestiera 1877, pubblico dominio) and the versification
map produced by map_diodati_versification.py, and writes diodati.json in the
same format as nuova_riveduta.json / riveduta_2020.json / riveduta_1927.json:

  {"meta": {...}, "books": [{n, name, abbr, chapters, verses}],
   "verses": [{b, c, v, x}]}

The verse TEXT is cleaned without touching the wording: the `<<title>>`
markers of the psalm headings become the newline-separated title of the NR
convention and whitespace runs collapse. Verse ADDRESSES are renumbered onto
the standard Italian versification with tools/dio_verse_map.json; six verses
without a standard counterpart keep their own (or the next free) address.

Usage:
  python3 tools/convert_diodati.py [--xml PATH] [--map PATH] [--nr PATH] [--out PATH]
"""

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import FILES_DIR
from map_diodati_versification import DEFAULT_XML, clean_text, load_diodati

DEFAULT_MAP = Path(__file__).resolve().parent / "dio_verse_map.json"
DEFAULT_NR = FILES_DIR / "nuova_riveduta.json"
DEFAULT_OUT = FILES_DIR / "diodati.json"

META = {
    "name": "Diodati",
    "abbr": "DIO",
    "description": "La Sacra Bibbia tradotta da Giovanni Diodati (1607), edizione della Società Biblica Britannica e Forestiera (1877)",
    "publisher": "Società Biblica Britannica e Forestiera",
    "year": "1607 (edizione 1877)",
    "copyright": "Pubblico dominio",
    "lang": "it",
    "intro": (
        "\nLa Bibbia di Giovanni Diodati (1607) è la prima traduzione protestante "
        "integrale della Sacra Scrittura in lingua italiana, opera del teologo lucchese "
        "rifugiato a Ginevra. Questa edizione ricalca la stampa della Società Biblica "
        "Britannica e Forestiera del 1877 ed è di pubblico dominio.\n"
        "Nota editoriale: la numerazione dei versetti è stata ricondotta alla "
        "versificazione italiana standard usata dalle altre versioni dell'app (la "
        "tradizione Diodati seguiva in parte la numerazione ebraica, ad es. Levitico "
        "6:1-7 = 5:20-26 o Deuteronomio 29:1 = 28:69). Il testo è riportato integralmente; "
        "i titoli dei Salmi, numerati come versetto 1 nell'edizione, precedono il testo "
        "del versetto come nelle altre versioni.\n"
    ),
}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--xml", type=Path, default=DEFAULT_XML)
    ap.add_argument("--map", type=Path, default=DEFAULT_MAP)
    ap.add_argument("--nr", type=Path, default=DEFAULT_NR)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = ap.parse_args()

    dio = load_diodati(args.xml)
    remaps = {(r["b"], r["c"], r["v"]): (r["tb"], r["tc"], r["tv"]) for r in json.loads(args.map.read_text(encoding="utf-8"))}
    nr = json.loads(args.nr.read_text(encoding="utf-8"))
    std_slots = {(v["b"], v["c"], v["v"]) for v in nr["verses"]}

    OSIS_ORDER = [
        "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1Sam", "2Sam",
        "1Kgs", "2Kgs", "1Chr", "2Chr", "Ezra", "Neh", "Esth", "Job", "Ps", "Prov",
        "Eccl", "Song", "Isa", "Jer", "Lam", "Ezek", "Dan", "Hos", "Joel", "Amos",
        "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
        "Matt", "Mark", "Luke", "John", "Acts", "Rom", "1Cor", "2Cor", "Gal", "Eph",
        "Phil", "Col", "1Thess", "2Thess", "1Tim", "2Tim", "Titus", "Phlm", "Heb",
        "Jas", "1Pet", "2Pet", "1John", "2John", "3John", "Jude", "Rev",
    ]
    nr_books = {b["n"]: b for b in nr["books"]}

    books = []
    verses = []
    seen = set()
    for n, osis in enumerate(OSIS_ORDER, 1):
        book_verses = 0
        chapters = set()
        for (bk, c, vs), text in dio[osis]:
            src = (n, c, vs)
            tgt = remaps.get(src, src)
            assert tgt not in seen, f"duplicate target {tgt} for source {src}"
            seen.add(tgt)
            clean = clean_text(text)
            assert "<<" not in clean and ">>" not in clean, f"title marker left in {src}"
            verses.append({"b": tgt[0], "c": tgt[1], "v": tgt[2], "x": clean})
            book_verses += 1
            chapters.add(tgt[1])
        books.append(
            {
                "n": n,
                "name": nr_books[n]["name"],
                "abbr": nr_books[n]["abbr"],
                "chapters": len(chapters),
                "verses": book_verses,
            }
        )

    verses.sort(key=lambda v: (v["b"], v["c"], v["v"]))
    doc = {"meta": META, "books": books, "verses": verses}
    args.out.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    covered = sum(1 for v in verses if (v["b"], v["c"], v["v"]) in std_slots)
    beyond = len(verses) - covered
    print(f"Versetti totali Diodati: {len(verses)} (di cui {covered} su indirizzi standard, {beyond} extra)")
    print(f"Slot standard coperti: {covered}/{len(std_slots)} ({len(std_slots) - covered} senza testo Diodati)")
    print(f"Scritto {args.out} ({args.out.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
