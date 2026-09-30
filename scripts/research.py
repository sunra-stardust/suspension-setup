#!/usr/bin/env python3
"""Check and summarise the catalog research data in research/ (format: research/FORMAT.md).

  python3 scripts/research.py check                 validate every file; exit 1 on errors
  python3 scripts/research.py overview              regenerate research/README.md
  python3 scripts/research.py review [--base REF]   write research/reviews/<today>.md with every
                                                    value that is new or changed since REF
                                                    (default origin/main) and print its path

Standard library only, so it runs in any cloud session without installing anything.
"""
import argparse
import datetime
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RESEARCH = ROOT / "research"
KINDS = {"suspension": "products", "bike": "models"}
DIRS = {"suspension": "suspension", "bike": "bikes"}
MAKER_STATUS = {"seed", "listed", "researched", "inactive"}
VALUE_STATUS = {"missing", "single", "verified", "conflict"}
DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
ID = re.compile(r"^[a-z0-9][a-z0-9-]*$")


# ---------- loading ----------

def load_tree(read):
    """read(relative_path) -> str or None. Returns (makers, {(kind, maker_id): items})."""
    raw = read("research/makers.json")
    makers = json.loads(raw)["makers"] if raw else []
    items = {}
    for m in makers:
        for kind in m.get("kinds", []):
            if kind not in DIRS:
                continue
            text = read(f"research/{DIRS[kind]}/{m['id']}.json")
            if text:
                items[(kind, m["id"])] = json.loads(text).get(KINDS[kind], [])
    return makers, items


def read_worktree(rel):
    p = ROOT / rel
    return p.read_text(encoding="utf-8") if p.exists() else None


def read_ref(ref):
    def read(rel):
        r = subprocess.run(["git", "show", f"{ref}:{rel}"], cwd=ROOT, capture_output=True, text=True)
        return r.stdout if r.returncode == 0 else None
    return read


# ---------- check ----------

def check_value(where, v, errors):
    if not isinstance(v, dict) or v.get("status") not in VALUE_STATUS:
        errors.append(f"{where}: needs a value object with status in {sorted(VALUE_STATUS)}")
        return
    s = v["status"]
    if s == "missing":
        if v.get("value") is not None:
            errors.append(f"{where}: status missing but value is set")
        return
    if s == "conflict":
        c = v.get("candidates")
        if v.get("value") is not None:
            errors.append(f"{where}: conflict must keep value null")
        if not isinstance(c, list) or len(c) < 2 or not all(isinstance(x, dict) and x.get("url") and x.get("quote") for x in c):
            errors.append(f"{where}: conflict needs >= 2 candidates with url and quote")
        if not v.get("issue"):
            errors.append(f"{where}: conflict needs the data-conflict issue URL")
        return
    for k in ("url", "quote"):
        if not v.get(k):
            errors.append(f"{where}: status {s} needs {k}")
    if not DATE.match(str(v.get("retrieved", ""))):
        errors.append(f"{where}: retrieved must be YYYY-MM-DD")
    if s == "verified":
        vb = v.get("verifiedBy")
        if not isinstance(vb, dict) or not vb.get("url") or not vb.get("quote") or not DATE.match(str(vb.get("retrieved", ""))):
            errors.append(f"{where}: verified needs verifiedBy with url, quote, retrieved")


def check(_args):
    errors = []
    path = RESEARCH / "makers.json"
    try:
        makers = json.loads(path.read_text(encoding="utf-8"))["makers"]
    except Exception as e:  # noqa: BLE001 - report any parse problem
        print(f"research/makers.json: {e}")
        return 1
    seen = set()
    for m in makers:
        mid = m.get("id", "?")
        if not ID.match(mid):
            errors.append(f"maker {mid}: id must match {ID.pattern}")
        if mid in seen:
            errors.append(f"maker {mid}: duplicate id")
        seen.add(mid)
        if not m.get("name"):
            errors.append(f"maker {mid}: name missing")
        if not m.get("kinds") or not set(m["kinds"]) <= set(DIRS):
            errors.append(f"maker {mid}: kinds must be a non-empty subset of {sorted(DIRS)}")
        if m.get("status") not in MAKER_STATUS:
            errors.append(f"maker {mid}: status must be one of {sorted(MAKER_STATUS)}")
        if m.get("lastChecked") is not None and not DATE.match(str(m["lastChecked"])):
            errors.append(f"maker {mid}: lastChecked must be YYYY-MM-DD or null")
    for kind, d in DIRS.items():
        for f in sorted((RESEARCH / d).glob("*.json")) if (RESEARCH / d).exists() else []:
            rel = f"research/{d}/{f.name}"
            owner = next((m for m in makers if m.get("id") == f.stem), None)
            if owner is None or kind not in owner.get("kinds", []):
                errors.append(f"{rel}: no maker '{f.stem}' with kind '{kind}' in makers.json")
            try:
                doc = json.loads(f.read_text(encoding="utf-8"))
            except json.JSONDecodeError as e:
                errors.append(f"{rel}: {e}")
                continue
            if doc.get("maker") != f.stem:
                errors.append(f"{rel}: 'maker' must be '{f.stem}'")
            ids = set()
            for item in doc.get(KINDS[kind], []):
                iid = item.get("id", "?")
                if not ID.match(iid) or iid in ids:
                    errors.append(f"{rel}: item id '{iid}' invalid or duplicate")
                ids.add(iid)
                if not item.get("name"):
                    errors.append(f"{rel} {iid}: name missing")
                if kind == "suspension" and item.get("type") not in ("fork", "shock"):
                    errors.append(f"{rel} {iid}: type must be fork or shock")
                if not isinstance(item.get("modelYears"), list):
                    errors.append(f"{rel} {iid}: modelYears must be a list (empty = unknown)")
                for name, v in (item.get("fields") or {}).items():
                    check_value(f"{rel} {iid}.{name}", v, errors)
    for e in errors:
        print(e)
    print(f"{len(makers)} makers, {len(errors)} errors")
    return 1 if errors else 0


# ---------- overview ----------

def counts(items):
    c = {s: 0 for s in VALUE_STATUS}
    for item in items:
        for v in (item.get("fields") or {}).values():
            c[v.get("status", "missing")] = c.get(v.get("status", "missing"), 0) + 1
    return c


def overview(_args):
    makers, items = load_tree(read_worktree)
    lines = [
        "# Research overview",
        "",
        "Generated by `python3 scripts/research.py overview` — do not edit. Format: [`FORMAT.md`](FORMAT.md).",
        "",
        "✅ verified (two independent passes) · 🔸 single pass · ⚠️ conflict · ⬜ missing (placeholder)",
        "",
    ]
    for kind, title in (("suspension", "Suspension makers"), ("bike", "Bike makers")):
        rows = [m for m in makers if kind in m.get("kinds", [])]
        tot = {s: 0 for s in VALUE_STATUS}
        body = []
        for m in sorted(rows, key=lambda x: x["name"].lower()):
            its = items.get((kind, m["id"]), [])
            c = counts(its)
            for s in tot:
                tot[s] += c[s]
            link = f"[{m['name']}]({DIRS[kind]}/{m['id']}.json)" if its else m["name"]
            body.append(f"| {link} | {m['status']} | {len(its)} | {c['verified']} | {c['single']} | {c['conflict']} | {c['missing']} | {m.get('lastChecked') or '—'} |")
        lines += [
            f"## {title} ({len(rows)})",
            "",
            f"Values: ✅ {tot['verified']} · 🔸 {tot['single']} · ⚠️ {tot['conflict']} · ⬜ {tot['missing']}",
            "",
            f"| Maker | Status | {'Products' if kind == 'suspension' else 'Models'} | ✅ | 🔸 | ⚠️ | ⬜ | Last checked |",
            "|---|---|---|---|---|---|---|---|",
            *body,
            "",
        ]
    (RESEARCH / "README.md").write_text("\n".join(lines), encoding="utf-8")
    print("research/README.md written")
    return 0


# ---------- review ----------

ICON = {"verified": "✅", "single": "🔸", "conflict": "⚠️", "missing": "⬜"}


def fmt(v):
    if v is None:
        return "—"
    s = json.dumps(v, ensure_ascii=False)
    return s if len(s) <= 80 else s[:77] + "…"


def review(args):
    new_makers, new_items = load_tree(read_worktree)
    old_makers, old_items = load_tree(read_ref(args.base))
    old_ids = {m["id"] for m in old_makers}
    added = [m for m in new_makers if m["id"] not in old_ids]
    names = {m["id"]: m["name"] for m in new_makers}
    stats = {s: 0 for s in VALUE_STATUS}
    sections = []
    for key in sorted(new_items, key=lambda k: (k[0], names.get(k[1], k[1]).lower())):
        kind, mid = key
        before = {i["id"]: i for i in old_items.get(key, [])}
        rows = []
        for item in new_items[key]:
            old_fields = (before.get(item["id"]) or {}).get("fields") or {}
            for name, v in sorted((item.get("fields") or {}).items()):
                o = old_fields.get(name)
                if o == v or v.get("status") == "missing" and (o is None or o.get("status") == "missing"):
                    continue
                stats[v["status"]] += 1
                years = ", ".join(map(str, item.get("modelYears") or [])) or "?"
                src = f"[{v['url'].split('/')[2]}]({v['url']})" if v.get("url") else "—"
                quote = (v.get("quote") or "").replace("|", "\\|").replace("\n", " ")
                if v["status"] == "verified":
                    quote += f" — 2nd pass: [{v['verifiedBy']['url'].split('/')[2]}]({v['verifiedBy']['url']})"
                if v["status"] == "conflict":
                    quote = " vs. ".join(f"{fmt(c.get('value'))} ([source]({c['url']}))" for c in v["candidates"]) + f" → [issue]({v.get('issue')})"
                old = fmt(o.get("value")) if o else "new"
                rows.append(f"| {item['name']} ({years}) | {name} | {old} | {ICON[v['status']]} {fmt(v.get('value'))} | {src} {v.get('where') or ''} | {quote} |")
        if rows:
            sections += [f"### {names.get(mid, mid)} ({kind})", "",
                         "| Item | Field | Before | Now | Source | Quote |", "|---|---|---|---|---|---|", *rows, ""]
    today = datetime.date.today().isoformat()
    out = [
        f"# Research review {today}",
        "",
        f"Changes since `{args.base}`. ✅ {stats['verified']} verified · 🔸 {stats['single']} single pass · ⚠️ {stats['conflict']} conflicts",
        "",
        "Only ✅ values may go into the app catalog. 🔸 values are recorded for the next run to confirm.",
        "",
    ]
    if added:
        out += [f"## New makers ({len(added)})", "", ", ".join(f"{m['name']} ({'/'.join(m['kinds'])})" for m in added), ""]
    out += ["## Values", ""] + (sections or ["No value changes.", ""])
    target = RESEARCH / "reviews" / f"{today}.md"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(out), encoding="utf-8")
    print(target.relative_to(ROOT))
    return 0


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("check")
    sub.add_parser("overview")
    r = sub.add_parser("review")
    r.add_argument("--base", default="origin/main")
    a = p.parse_args()
    return {"check": check, "overview": overview, "review": review}[a.cmd](a)


if __name__ == "__main__":
    sys.exit(main())
