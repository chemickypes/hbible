#!/usr/bin/env python3
"""Pre-generate the Italian glosses (gi) for the bundled lexicon via Gemini.

Translates the (clean OpenScriptures) English definitions of lexicon.json to
Italian in batches, filling the "gi" field of every entry that lacks one, so
the app's word cards show Italian without waiting for the on-demand AI call.

Requires GEMINI_API_KEY in the environment (Gemini API, model
gemini-2.5-flash-lite by default — cheap and fast). The run is RESUMABLE:
progress is checkpointed to --progress after every batch, already-translated
entries are skipped, and lexicon.json is rewritten atomically at the end of
each batch (Ctrl-C safe).

Usage:
  export GEMINI_API_KEY=...   (from https://aistudio.google.com/apikey)
  python3 tools/translate_glosses.py [--dry-run] [--batch 40] [--delay 1.5]

Estimated cost for the full lexicon (~14.300 definitions, ~150 token each):
under 1$ on flash-lite; ~15-20 minutes with the default pacing.
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from r27_common import FILES_DIR

DEFAULT_LEXICON = FILES_DIR / "lexicon.json"
DEFAULT_PROGRESS = Path(__file__).resolve().parent / "gloss_it_progress.json"

MODEL = "gemini-2.5-flash-lite"
URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent"

SYSTEM = (
    "Sei un lessicografo biblico che prepara un dizionario Strong in italiano. "
    "Traduci in italiano le definizioni che ti vengono date, mantenendo: "
    "(1) le parentesi e i loro contenuti tra parentesi quando precisano il senso; "
    "(2) i riferimenti a numeri Strong ('Compare 5368' -> 'Cfr. 5368'); "
    "(3) i termini tecnici (Aramaic -> aramaico, figurative -> figurato, "
    "proper noun -> nome proprio, Septuagint -> Settanta). "
    "Sii conciso e usa l'italiano della Bibbia (Dio, Signore, Eterno, alleanza, ecc.). "
    "Rispondi SOLO con un oggetto JSON che mappa ogni numero alla traduzione."
)


def build_prompt(batch: dict) -> str:
    lines = [f"{key}: {gloss}" for key, gloss in batch.items()]
    return "Traduci queste definizioni:\n" + "\n".join(lines)


def call_gemini(api_key: str, prompt: str, timeout: int = 120) -> dict:
    body = {
        "system_instruction": {"parts": [{"text": SYSTEM}]},
        "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0.1, "responseMimeType": "application/json"},
    }
    req = urllib.request.Request(
        URL,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "x-goog-api-key": api_key},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    text = data["candidates"][0]["content"]["parts"][0]["text"]
    return json.loads(text)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--lexicon", type=Path, default=DEFAULT_LEXICON)
    ap.add_argument("--progress", type=Path, default=DEFAULT_PROGRESS)
    ap.add_argument("--batch", type=int, default=40)
    ap.add_argument("--delay", type=float, default=1.5)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    api_key = os.environ.get("GEMINI_API_KEY", "")
    lex = json.loads(args.lexicon.read_text(encoding="utf-8"))

    todo = {
        f"{lang}{num}": entry["g"]
        for lang in ("he", "el")
        for num, entry in lex[lang].items()
        if entry["g"] and not entry.get("gi")
    }
    done = {}
    if args.progress.exists():
        done = json.loads(args.progress.read_text(encoding="utf-8"))
        for k in list(todo):
            if k in done:
                todo.pop(k)
    print(f"Voci da tradurre: {len(todo)} (già pronte: {len(done)})")
    if args.dry_run:
        for k in list(todo)[:5]:
            print(f"  {k}: {todo[k][:70]}")
        print("(dry-run: nessuna chiamata)")
        return
    if not api_key:
        sys.exit("GEMINI_API_KEY non impostata: export GEMINI_API_KEY=... e rilancia.")
    if not todo:
        print("Tutto già tradotto.")
        return

    keys = list(todo)
    for start in range(0, len(keys), args.batch):
        chunk_keys = keys[start : start + args.batch]
        batch = {k: todo[k] for k in chunk_keys}
        for attempt in (1, 2, 3):
            try:
                result = call_gemini(api_key, build_prompt(batch))
                break
            except (urllib.error.URLError, TimeoutError, KeyError, json.JSONDecodeError) as e:
                print(f"  batch {start // args.batch + 1}: tentativo {attempt} fallito ({e})")
                if attempt == 3:
                    raise
                time.sleep(5 * attempt)
        applied = 0
        for k in chunk_keys:
            t = result.get(k)
            if isinstance(t, str) and t.strip():
                done[k] = t.strip()
                applied += 1
        # Apply translations to the lexicon.
        for k, t in done.items():
            lang, num = k[:2], k[2:]
            if lang in lex and num in lex[lang]:
                lex[lang][num]["gi"] = t
        args.lexicon.write_text(json.dumps(lex, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        args.progress.write_text(json.dumps(done, ensure_ascii=False), encoding="utf-8")
        print(f"  [{start + len(chunk_keys)}/{len(keys)}] tradotte {applied}/{len(chunk_keys)} (checkpoint salvato)")
        time.sleep(args.delay)

    print(f"\nFatto: {len(done)} definizioni in italiano, lexicon.json aggiornato.")


if __name__ == "__main__":
    main()
