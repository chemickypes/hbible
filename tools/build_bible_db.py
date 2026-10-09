#!/usr/bin/env python3
"""Genera il database SQLite pre-costruito (bible.db) incluso nell'app.

Il DB nasce dagli stessi JSON della pipeline (composeApp/src/commonMain/
composeResources/files, letti anche dal CMS e da learn-hebrew) e dall'ultimo
schema Room esportato (composeApp/schemas/): tabelle, indici, room_master_table
con l'identity hash e user_version identici a quelli che Room si aspetta, così
l'app apre il file copiato senza migrazioni né import.

Il contenuto replica DefaultBibleRepository.ensureImported() riga per riga
(stesse colonne, stesse join "," e "\\t", INSERT OR REPLACE come i DAO).

Viene invocato dal task Gradle :composeApp:buildBundledDatabase; a mano:
    python3 tools/build_bible_db.py --out /tmp/bible.db
Solo libreria standard.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES_DIR = ROOT / "composeApp/src/commonMain/composeResources/files"
SCHEMA_DIR = ROOT / "composeApp/schemas/com.hooloovoochimico.kmp.hbible.data.local.BibleDatabase"

# Traduzioni incluse, nello stesso ordine dell'import storico dell'app (i libri
# si prendono dalla prima). Deve coincidere con TRANSLATION_NAMES in
# BibleRepository.kt.
# Solo testi a licenza aperta (dal 2026-10-09): le traduzioni chiuse stanno nel
# cassetto del CMS e non devono mai finire qui. La prima è la predefinita e da
# lei si prendono i libri.
TRANSLATION_ASSETS = [
    ("bibbia_aperta.json", "OTB"),
    ("riveduta_1927.json", "R27"),
    ("diodati.json", "DIO"),
    ("martini.json", "MAR"),
]
ORIGINALS_ASSET = "originals.json"
CROSSREFS_ASSET = "crossrefs.json"
LEXICON_ASSET = "lexicon.json"
CONTENT_STATE_ASSET = "content-state.json"


def latest_schema(schema_dir: Path) -> dict:
    versions = sorted(int(p.stem) for p in schema_dir.glob("*.json"))
    if not versions:
        sys.exit(f"Nessuno schema Room in {schema_dir}")
    return json.loads((schema_dir / f"{versions[-1]}.json").read_text("utf-8"))["database"]


def create_schema(conn: sqlite3.Connection, db: dict) -> None:
    for entity in db["entities"]:
        table = entity["tableName"]
        conn.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            conn.execute(index["createSql"].replace("${TABLE_NAME}", table))
    for view in db.get("views", []):
        conn.execute(view["createSql"].replace("${VIEW_NAME}", view["viewName"]))
    for query in db["setupQueries"]:
        conn.execute(query)
    conn.execute(f"PRAGMA user_version = {int(db['version'])}")


def load(name: str):
    return json.loads((FILES_DIR / name).read_text("utf-8"))


def insert(conn: sqlite3.Connection, table: str, columns: list[str], rows) -> int:
    sql = f"INSERT OR REPLACE INTO {table} ({', '.join(columns)}) VALUES ({', '.join('?' * len(columns))})"
    cur = conn.executemany(sql, rows)
    return cur.rowcount


def joined(values, sep: str = ",") -> str:
    return sep.join(str(v) for v in values)


def import_translations(conn: sqlite3.Connection) -> None:
    books_done = False
    for asset, abbr in TRANSLATION_ASSETS:
        if not (FILES_DIR / asset).exists():
            # Rigoroso: l'app considera "incluse" tutte le traduzioni di
            # TRANSLATION_NAMES e le ricopia da qui se mancano nel DB utente.
            sys.exit(f"{asset} mancante in {FILES_DIR}")
        doc = load(asset)
        code = doc["meta"]["abbr"]
        if code != abbr:
            sys.exit(f"{asset}: meta.abbr={code}, atteso {abbr}")
        if not books_done:
            insert(conn, "books", ["n", "name", "abbr", "chapters"],
                   ((b["n"], b["name"], b["abbr"], b["chapters"]) for b in doc["books"]))
            books_done = True
        n = insert(conn, "verses", ["translation", "book", "chapter", "verse", "title", "text", "paragraph"],
                   ((code, v["b"], v["c"], v["v"], v.get("t"), v["x"], 1 if v.get("p") else 0)
                    for v in doc["verses"]))
        print(f"  {code}: {n} versetti")


def import_originals(conn: sqlite3.Connection) -> None:
    doc = load(ORIGINALS_ASSET)
    columns = ["book", "chapter", "verse", "lang", "text", "transliteration", "lemmas",
               "glosses", "glosses_it"]
    rows = (
        (v["b"], v["c"], v["v"], v["lang"], v["text"], v["tr"], v.get("lm", ""),
         joined(v.get("ge", []), "\t"), joined(v.get("gi", []), "\t"))
        for v in doc["verses"]
    )
    print(f"  originali: {insert(conn, 'original_verses', columns, rows)}")
    # Allineamenti generici (formato 2 dei pacchetti: campo `al`, sigla -> indici).
    # Solo le traduzioni incluse: un canale di una traduzione assente non serve.
    included = {abbr for _, abbr in TRANSLATION_ASSETS}
    unknown = sorted({a for v in doc["verses"] for a in v.get("al", {})} - included)
    if unknown:
        sys.exit(f"{ORIGINALS_ASSET}: allineamenti di traduzioni non incluse: {unknown}")
    arows = (
        (v["b"], v["c"], v["v"], abbr, joined(indices))
        for v in doc["verses"] for abbr, indices in v.get("al", {}).items()
    )
    n = insert(conn, "original_alignments", ["book", "chapter", "verse", "translation", "indices"], arows)
    print(f"  allineamenti: {n}")


def import_crossrefs(conn: sqlite3.Connection) -> None:
    doc = load(CROSSREFS_ASSET)
    columns = ["fromBook", "fromChapter", "fromVerse", "toBook", "toChapter", "toVerse"]
    print(f"  riferimenti: {insert(conn, 'cross_references', columns, (tuple(r[:6]) for r in doc['refs']))}")


def import_lexicon(conn: sqlite3.Connection) -> None:
    doc = load(LEXICON_ASSET)
    columns = ["lang", "number", "romanized", "gloss", "gloss_it"]
    total = 0
    for lang in ("he", "el"):
        entries = doc.get(lang, {})
        total += insert(conn, "lexemes", columns,
                        ((lang, number, e.get("tr", ""), e.get("g", ""), e.get("gi", ""))
                         for number, e in entries.items()))
    print(f"  lessico: {total}")


def content_fingerprint(schema: dict) -> str:
    """Impronta dei contenuti inclusi: JSON sorgente + versione dello schema.
    L'app la confronta con quella già applicata e, se cambia (APK con contenuti
    nuovi), ricopia le tabelle dei contenuti dal DB incluso."""
    h = hashlib.sha256(f"schema:{schema['version']}".encode())
    names = [a for a, _ in TRANSLATION_ASSETS] + [ORIGINALS_ASSET, CROSSREFS_ASSET, LEXICON_ASSET]
    if (FILES_DIR / CONTENT_STATE_ASSET).exists():
        names.append(CONTENT_STATE_ASSET)
    for name in names:
        h.update(name.encode())
        h.update((FILES_DIR / name).read_bytes())
    return h.hexdigest()


def package_states() -> list[tuple]:
    """Righe di content_state per i pacchetti del CMS da cui vengono i JSON inclusi
    (content-state.json, scritto da `npm run export:hbible`): il sync dell'app scarica
    poi solo i pacchetti cambiati dopo l'export (R04). Senza il file: nessuna riga."""
    path = FILES_DIR / CONTENT_STATE_ASSET
    if not path.exists():
        return []
    doc = json.loads(path.read_text(encoding="utf-8"))
    return [(pkg, h, doc["version"], 0) for pkg, h in sorted(doc["packages"].items())]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", required=True, type=Path, help="percorso del bible.db da generare")
    args = parser.parse_args()

    schema = latest_schema(SCHEMA_DIR)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.out.with_suffix(".tmp")
    tmp.unlink(missing_ok=True)

    conn = sqlite3.connect(tmp)
    try:
        # File singolo e compatto: niente WAL nel file distribuito (Room lo
        # riattiva all'apertura), pagine piene.
        conn.execute("PRAGMA journal_mode = DELETE")
        create_schema(conn, schema)
        with conn:
            print(f"bible.db (schema v{schema['version']}):")
            import_translations(conn)
            import_originals(conn)
            import_crossrefs(conn)
            import_lexicon(conn)
            fingerprint = content_fingerprint(schema)
            states = package_states()
            insert(conn, "content_state", ["package_id", "hash", "version", "synced_at"],
                   [("bundled", fingerprint, f"schema-{schema['version']}", 0)] + states)
            print(f"  impronta contenuti: {fingerprint[:12]}, stati dei pacchetti: {len(states)}")
        conn.execute("ANALYZE")
        conn.execute("VACUUM")
    finally:
        conn.close()
    os.replace(tmp, args.out)
    print(f"scritto {args.out} ({args.out.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
