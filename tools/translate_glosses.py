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
import hashlib
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


ZAI_URL = "https://api.z.ai/api/paas/v4/chat/completions"
OLLAMA_URL = "http://localhost:11434/api/chat"
ZAI_AUTH = Path.home() / ".local/share/opencode/auth.json"


def zai_key() -> str:
    # opencode ruota il token: si legge SEMPRE fresco (env come fallback).
    key = os.environ.get("ZAI_API_KEY", "")
    if ZAI_AUTH.exists():
        try:
            key = json.loads(ZAI_AUTH.read_text(encoding="utf-8"))["zai"]["key"]
        except (KeyError, ValueError):
            pass
    if not key:
        sys.exit("ZAI_API_KEY non impostata né trovata in opencode auth.json.")
    return key


def _clean_json(text: str) -> str:
    text = text.strip()
    if text.startswith("```"):
        text = text.split("```")[1]
        if text.startswith("json"):
            text = text[4:]
    return text.strip()


def call_zai(api_key: str, prompt: str, model: str = "glm-4.5-air", timeout: int = 300) -> dict:
    key = api_key or zai_key()
    body = {
        "model": model,
        "messages": [
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.1,
        "response_format": {"type": "json_object"},
        "thinking": {"type": "disabled"},
        "stream": False,
    }
    req = urllib.request.Request(
        ZAI_URL,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {key}"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    if "error" in data:
        raise RuntimeError(f"zai error: {data['error']}")
    return json.loads(_clean_json(data["choices"][0]["message"]["content"]))


def call_ollama(model: str, prompt: str, timeout: int = 1200) -> dict:
    body = {
        "model": model,
        "stream": False,
        "format": "json",
        "messages": [
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": prompt},
        ],
        "options": {"num_ctx": 8192, "temperature": 0.1},
    }
    req = urllib.request.Request(
        OLLAMA_URL,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    return json.loads(_clean_json(data["message"]["content"]))


def make_caller(backend: str, args):
    if backend == "gemini":
        key = os.environ.get("GEMINI_API_KEY", "")
        if not key:
            sys.exit("GEMINI_API_KEY non impostata.")
        return lambda prompt: call_gemini(key, prompt)
    if backend == "zai":
        # chiave passata vuota: call_zai la rilegge fresca da auth.json a ogni chiamata
        return lambda prompt: call_zai("", prompt, model=args.model or "glm-4.5-air")
    if backend == "ollama":
        return lambda prompt: call_ollama(args.model or "gemma4:e4b", prompt)
    sys.exit(f"backend sconosciuto: {backend}")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--lexicon", type=Path, default=DEFAULT_LEXICON)
    ap.add_argument("--progress", type=Path, default=None,
                    help="default: gloss_it_progress_<backend>.json accanto allo script")
    ap.add_argument("--backend", choices=("gemini", "zai", "ollama"), default="gemini")
    ap.add_argument("--model", default=None, help="override del modello (zai/ollama)")
    ap.add_argument("--lang", choices=("all", "he", "el"), default="all",
                    help="restrige il worker a una lingua (split parallelo)")
    ap.add_argument("--part", default=None, metavar="N/M",
                    help="prende solo le chiavi con md5%%M == N (split deterministico)")
    ap.add_argument("--no-apply", action="store_true",
                    help="salva solo il checkpoint, NON tocca lexicon.json (merge dopo con --merge)")
    ap.add_argument("--merge", action="store_true",
                    help="applica TUTTI i gloss_it_progress_*.json al lexicon ed esce")
    ap.add_argument("--batch", type=int, default=40)
    ap.add_argument("--delay", type=float, default=1.5)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    if args.progress is None:
        args.progress = Path(__file__).resolve().parent / f"gloss_it_progress_{args.backend}.json"

    lex = json.loads(args.lexicon.read_text(encoding="utf-8"))

    if args.merge:
        applied = 0
        for pf in sorted(Path(__file__).resolve().parent.glob("gloss_it_progress*.json")):
            if pf.name == "gloss_it_progress.json":
                continue
            done = json.loads(pf.read_text(encoding="utf-8"))
            n = 0
            for k, t in done.items():
                lang, num = k[:2], k[2:]
                if lang in lex and num in lex[lang] and isinstance(t, str) and t.strip():
                    lex[lang][num]["gi"] = t.strip()
                    n += 1
            applied += n
            print(f"  {pf.name}: {n} voci applicate")
        args.lexicon.write_text(json.dumps(lex, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        print(f"Merge completato: {applied} definizioni in italiano su lexicon.json.")
        return

    todo = {
        f"{lang}{num}": entry["g"]
        for lang in ("he", "el")
        for num, entry in lex[lang].items()
        if entry["g"] and not entry.get("gi")
    }
    if args.lang != "all":
        todo = {k: v for k, v in todo.items() if k.startswith(args.lang)}
    if args.part:
        n, m = (int(x) for x in args.part.split("/"))
        todo = {k: v for k, v in todo.items() if hashlib.md5(k.encode()).digest()[0] % m == n}
    done = {}
    if args.progress.exists():
        done = json.loads(args.progress.read_text(encoding="utf-8"))
        for k in list(todo):
            if k in done:
                todo.pop(k)
    print(f"[{args.backend}] Voci da tradurre: {len(todo)} (già pronte: {len(done)})", flush=True)
    if args.dry_run:
        for k in list(todo)[:5]:
            print(f"  {k}: {todo[k][:70]}")
        print("(dry-run: nessuna chiamata)")
        return

    if not todo:
        print("Tutto già tradotto.")
        return

    call = make_caller(args.backend, args)
    keys = list(todo)
    for start in range(0, len(keys), args.batch):
        chunk_keys = keys[start : start + args.batch]
        batch = {k: todo[k] for k in chunk_keys}
        for attempt in (1, 2, 3, 4, 5):
            try:
                result = call(build_prompt(batch))
                if not any(k in result or k[2:] in result for k in chunk_keys):
                    raise ValueError(f"risposta senza le chiavi attese: {str(result)[:120]}")
                break
            except (urllib.error.URLError, TimeoutError, KeyError, AttributeError, json.JSONDecodeError, ValueError) as e:
                print(f"  batch {start // args.batch + 1}: tentativo {attempt} fallito ({e})", flush=True)
                if attempt == 5:
                    skipped = Path(__file__).resolve().parent / "gloss_it_skipped.txt"
                    with skipped.open("a", encoding="utf-8") as sf:
                        sf.write(",".join(chunk_keys) + "\n")
                    print(f"  batch {start // args.batch + 1}: SALTATO ({len(chunk_keys)} chiavi -> gloss_it_skipped.txt)", flush=True)
                    result = None
                    break
                time.sleep(8 * attempt)
        if result is None:
            continue
        applied = 0
        rkeys = {kk[2:]: v for kk, v in result.items() if isinstance(v, str)}
        for k in chunk_keys:
            t = result.get(k) or rkeys.get(k[2:])
            if isinstance(t, str) and t.strip():
                done[k] = t.strip()
                applied += 1
        if not args.no_apply:
            for k, t in done.items():
                lang, num = k[:2], k[2:]
                if lang in lex and num in lex[lang]:
                    lex[lang][num]["gi"] = t
            args.lexicon.write_text(json.dumps(lex, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        args.progress.write_text(json.dumps(done, ensure_ascii=False), encoding="utf-8")
        print(f"  [{start + len(chunk_keys)}/{len(keys)}] tradotte {applied}/{len(chunk_keys)} (checkpoint salvato)", flush=True)
        time.sleep(args.delay)

    print(f"\nFatto: {len(done)} definizioni in italiano nel checkpoint {args.progress.name}.", flush=True)


if __name__ == "__main__":
    main()
