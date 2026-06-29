#!/usr/bin/env python3
"""
Reliability harness for the Agentic Task Manager AppFunctions.

It measures how reliably a Gemini agent turns a natural-language request into the RIGHT function
call with the RIGHT parameters — the number that tells you whether the app is genuinely
"agent-ready", and the core of the project's distinctive write-up.

For every intent in the dataset it sends Gemini:
  - a system instruction framing it as an on-device task-manager assistant,
  - the five functions as tool (function) declarations,
  - a fixed snapshot of the current tasks (so id-based parameters are resolvable),
  - the user's request,
and records which function Gemini chose and with which arguments. It then compares that against
the expected function/params and aggregates accuracy.

It runs the whole dataset under TWO description variants — "rich" (the real KDocs) and "terse"
(bare one-liners) — to quantify how much the quality of the KDoc/description moves accuracy.

Usage:
  python3 harness.py --dataset dataset.json --out report.json [--model gemini-2.5-flash]
                     [--variants rich,terse] [--limit N] [--sleep 1.0]

The API key is read from ../../gemini.api.key (git-ignored); it is never printed.
Standard library only.
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
KEY_FILE = os.path.normpath(os.path.join(HERE, "..", "..", "gemini.api.key"))

FUNCTIONS = ["getActionableTasks", "getBlockingOverdueTasks", "addTask", "completeTask", "deleteTask"]


class QuotaExhausted(Exception):
    """Raised to abort the run cleanly when the Gemini free-tier quota is spent."""

# Minimal id<->title directory. Deliberately carries NO status / actionability / overdue / dependency
# information: the model must CALL a function to read that state (it can't shortcut a query by reading
# the answer from the prompt), while still being able to resolve "the slides" -> t-slides for actions.
TASK_CONTEXT = """You do not have the current task data loaded — call a function to read state.
For reference, the ids of the known tasks are:
- t-venue = "Book venue"
- t-slides = "Prepare slides"
- t-present = "Present at the GDG meetup"
- t-milk = "Buy milk"
- t-plants = "Water the plants\""""

SYSTEM_INSTRUCTION = (
    "You are an on-device assistant for a task manager. When the user's request matches one of the "
    "provided functions, call exactly one function with the best arguments. If NO function fits the "
    "request, do not call any function — reply with a short sentence instead. Resolve task references "
    "to ids using the provided current task list."
)

# --- Rich descriptions: the real KDocs (condensed). The point of the project: KDoc == the prompt. ---
RICH = {
    "getActionableTasks": "Returns the tasks the user can work on right now: open tasks whose every "
        "dependency is already completed, so nothing is blocking them, ordered by urgency. Use when the "
        "user asks what they can do now, what is unblocked or ready, or what to focus on next.",
    "getBlockingOverdueTasks": "Returns open tasks that are past their due date AND are blocking at least "
        "one other open task — both late and holding up the rest of the plan. Use for 'what's overdue and "
        "stuck', 'what's holding everything up', 'what's late and blocking other work'. Not for a plain "
        "list of overdue tasks.",
    "addTask": "Creates a new task. Optionally links the tasks it depends on (the new task becomes "
        "actionable only after those are completed): pass ids in dependsOnTaskIds or natural-language "
        "titles in dependsOnTitles. A dependency that would create a cycle is rejected.",
    "completeTask": "Marks a task as completed and reports which previously-blocked tasks became "
        "actionable as a result. Use for 'mark X done', 'complete X', 'I finished X'.",
    "deleteTask": "Deletes a task permanently. Destructive and irreversible: call first with "
        "confirmed=false to receive a summary of exactly what would be deleted, then call again with "
        "confirmed=true only after the user has agreed.",
}

# --- Terse descriptions: deliberately unhelpful, to isolate the effect of description quality. ---
TERSE = {
    "getActionableTasks": "Get actionable tasks.",
    "getBlockingOverdueTasks": "Get blocking overdue tasks.",
    "addTask": "Add a task.",
    "completeTask": "Complete a task.",
    "deleteTask": "Delete a task.",
}

# Field descriptions (used only in the rich variant).
FIELD_DESC = {
    "title": "Short title of the new task.",
    "description": "Optional longer description.",
    "priority": "One of LOW, MEDIUM, HIGH, URGENT.",
    "dueDate": "Due date or date-time in ISO-8601.",
    "dependsOnTaskIds": "Ids of existing tasks this task depends on.",
    "dependsOnTitles": "Natural-language titles of tasks to depend on (resolved by best match).",
    "parentTaskId": "Id of a parent task, to create a sub-task.",
    "recurrence": "One of NONE, DAILY, WEEKLY, MONTHLY.",
    "taskId": "Id of the target task.",
    "confirmed": "Must be false on the first call; true only after the user confirms.",
}


def field(name, typ, rich, enum=None, items=None):
    p = {"type": typ}
    if rich:
        p["description"] = FIELD_DESC.get(name, "")
    if enum:
        p["enum"] = enum
    if items:
        p["items"] = items
    return p


def declarations(rich: bool):
    """Build the five Gemini function declarations for the given description variant."""
    desc = RICH if rich else TERSE
    add_props = {
        "title": field("title", "STRING", rich),
        "description": field("description", "STRING", rich),
        "priority": field("priority", "STRING", rich, enum=["LOW", "MEDIUM", "HIGH", "URGENT"]),
        "dueDate": field("dueDate", "STRING", rich),
        "dependsOnTaskIds": field("dependsOnTaskIds", "ARRAY", rich, items={"type": "STRING"}),
        "dependsOnTitles": field("dependsOnTitles", "ARRAY", rich, items={"type": "STRING"}),
        "parentTaskId": field("parentTaskId", "STRING", rich),
        "recurrence": field("recurrence", "STRING", rich, enum=["NONE", "DAILY", "WEEKLY", "MONTHLY"]),
    }
    return [
        {"name": "getActionableTasks", "description": desc["getActionableTasks"]},
        {"name": "getBlockingOverdueTasks", "description": desc["getBlockingOverdueTasks"]},
        {
            "name": "addTask", "description": desc["addTask"],
            "parameters": {"type": "OBJECT", "properties": add_props, "required": ["title"]},
        },
        {
            "name": "completeTask", "description": desc["completeTask"],
            "parameters": {"type": "OBJECT", "properties": {"taskId": field("taskId", "STRING", rich)},
                           "required": ["taskId"]},
        },
        {
            "name": "deleteTask", "description": desc["deleteTask"],
            "parameters": {"type": "OBJECT", "properties": {
                "taskId": field("taskId", "STRING", rich),
                "confirmed": field("confirmed", "BOOLEAN", rich),
            }, "required": ["taskId"]},
        },
    ]


def call_gemini(model, api_key, decls, intent, sleep, max_retries=4):
    """Single generateContent call.

    Returns (function_name_or_None, args_dict). Two sentinels: "__QUOTA__" when the free-tier
    quota is exhausted (so the caller can abort the whole run instead of grinding for hours), and
    "__ERROR__" for other failures.
    """
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    body = {
        "system_instruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
        "contents": [{"role": "user", "parts": [{"text": f"{TASK_CONTEXT}\n\nUser: {intent}"}]}],
        "tools": [{"function_declarations": decls}],
        "tool_config": {"function_calling_config": {"mode": "AUTO"}},
        "generationConfig": {"temperature": 0},
    }
    data = json.dumps(body).encode()
    backoff = 8.0
    for attempt in range(max_retries):
        req = urllib.request.Request(url, data=data, method="POST")
        req.add_header("Content-Type", "application/json")
        req.add_header("x-goog-api-key", api_key)
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                payload = json.loads(resp.read())
            time.sleep(sleep)
            parts = payload.get("candidates", [{}])[0].get("content", {}).get("parts", []) or []
            for p in parts:
                if "functionCall" in p:
                    fc = p["functionCall"]
                    return fc.get("name"), (fc.get("args") or {})
            return None, {}  # no function call -> treated as NONE
        except urllib.error.HTTPError as e:
            code = e.code
            if code in (429, 500, 503):
                body = ""
                try:
                    body = e.read().decode(errors="ignore")
                except Exception:  # noqa: BLE001
                    pass
                # "limit: 0" means the free-tier daily allowance is spent — retrying is futile.
                if "limit: 0" in body:
                    return "__QUOTA__", {}
                # Per-minute throttle: honour the server's suggested "retry in Ns".
                m = re.search(r"retry in ([0-9.]+)s", body)
                wait = (float(m.group(1)) + 1.5) if m else backoff
                time.sleep(min(wait, 45))
                backoff = min(backoff * 2, 45)
                continue
            sys.stderr.write(f"HTTP {code} for intent: {intent[:60]}\n")
            return "__ERROR__", {}
        except Exception as e:  # noqa: BLE001
            sys.stderr.write(f"error {e} for intent: {intent[:60]}\n")
            time.sleep(backoff)
            backoff = min(backoff * 2, 45)
    # Retries exhausted while rate-limited: treat as a quota stop, not a per-item error.
    return "__QUOTA__", {}


def norm(s):
    return re.sub(r"\s+", " ", str(s).strip().lower())


def params_match(expected, got):
    """Lenient per-parameter comparison. Returns (all_ok, detail dict)."""
    detail = {}
    ok = True
    for key, exp in expected.items():
        if exp in (None, "", [], {}):
            continue
        g = got.get(key)
        if key == "taskId":
            good = norm(g) == norm(exp)
        elif key == "title":
            good = g is not None and (norm(exp) in norm(g) or norm(g) in norm(exp))
        elif key in ("dependsOnTitles", "dependsOnTaskIds"):
            gv = " ".join(norm(x) for x in (g or []))
            # also accept the cross form (titles given but ids extracted, or vice versa)
            cross = " ".join(norm(x) for x in (got.get("dependsOnTaskIds", []) + got.get("dependsOnTitles", [])))
            hay = gv + " " + cross
            good = all(
                norm(e) in hay or any(tok in hay for tok in norm(e).split() if len(tok) > 3)
                for e in exp
            )
        elif key in ("priority", "recurrence"):
            good = norm(g) == norm(exp)
        elif key == "dueDate":
            good = bool(g)  # presence is enough; date normalization varies
        elif key == "confirmed":
            good = bool(g) == bool(exp)
        else:
            good = norm(g) == norm(exp)
        detail[key] = good
        ok = ok and good
    return ok, detail


def run_variant(variant, model, api_key, dataset, sleep, limit):
    rich = variant == "rich"
    decls = declarations(rich)
    items = dataset[:limit] if limit else dataset
    results = []
    for i, item in enumerate(items):
        exp_fn = item["expectedFunction"]
        got_fn, got_args = call_gemini(model, api_key, decls, item["intent"], sleep)
        if got_fn == "__QUOTA__":
            # Stop the whole run immediately rather than grind through retries for hours.
            raise QuotaExhausted(f"quota hit at {item['id']} ({i + 1}/{len(items)}, variant={variant})")
        predicted = None if got_fn is None else got_fn
        fn_correct = (
            (exp_fn == "NONE" and predicted is None)
            or (exp_fn != "NONE" and predicted == exp_fn)
        )
        exp_params = item.get("expectedParams") or {}
        has_params = any(v not in (None, "", [], {}) for v in exp_params.values())
        if fn_correct and exp_fn != "NONE" and has_params:
            p_ok, p_detail = params_match(exp_params, got_args)
        else:
            p_ok, p_detail = (None, {})
        results.append({
            "id": item["id"], "goal": item["goal"], "intent": item["intent"],
            "expectedFunction": exp_fn, "predictedFunction": predicted,
            "functionCorrect": fn_correct, "paramsCorrect": p_ok, "paramsDetail": p_detail,
            "predictedArgs": got_args,
        })
        sys.stderr.write(f"[{variant}] {i+1}/{len(items)} {item['id']} exp={exp_fn} got={predicted} "
                         f"fn={'ok' if fn_correct else 'X'}\n")
    return results


def aggregate(results):
    total = len(results)
    fn_ok = sum(1 for r in results if r["functionCorrect"])
    by_goal = {}
    for r in results:
        g = by_goal.setdefault(r["goal"], {"n": 0, "ok": 0})
        g["n"] += 1
        g["ok"] += 1 if r["functionCorrect"] else 0
    none_items = [r for r in results if r["expectedFunction"] == "NONE"]
    none_ok = sum(1 for r in none_items if r["functionCorrect"])
    forced = sum(1 for r in none_items if r["predictedFunction"] is not None)
    param_items = [r for r in results if r["paramsCorrect"] is not None]
    param_ok = sum(1 for r in param_items if r["paramsCorrect"])
    return {
        "total": total,
        "functionAccuracy": round(fn_ok / total, 4) if total else 0,
        "functionCorrect": fn_ok,
        "byGoal": {g: {"accuracy": round(v["ok"] / v["n"], 4), "n": v["n"]} for g, v in by_goal.items()},
        "restraintAccuracy": round(none_ok / len(none_items), 4) if none_items else None,
        "outOfScopeForcedCalls": forced,
        "paramAccuracy": round(param_ok / len(param_items), 4) if param_items else None,
        "paramItems": len(param_items),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dataset", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--model", default="gemini-2.5-flash")
    ap.add_argument("--variants", default="rich,terse")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--sleep", type=float, default=1.0)
    args = ap.parse_args()

    with open(KEY_FILE) as f:
        api_key = f.read().strip()
    with open(args.dataset) as f:
        dataset = json.load(f)["dataset"]

    # Preflight: one cheap call. If the free-tier quota is already spent, fail fast with guidance
    # instead of starting a run that can only produce errors.
    probe, _ = call_gemini(args.model, api_key, declarations(True), "ping", sleep=0)
    if probe == "__QUOTA__":
        sys.exit(
            f"Free-tier quota for {args.model} is exhausted. Re-run after the daily reset, try a "
            f"different model with --model, or enable billing on the API key."
        )

    report = {"model": args.model, "datasetSize": len(dataset), "variants": {}}
    try:
        for variant in [v.strip() for v in args.variants.split(",") if v.strip()]:
            res = run_variant(variant, args.model, api_key, dataset, args.sleep, args.limit)
            report["variants"][variant] = {"summary": aggregate(res), "results": res}
    except QuotaExhausted as e:
        sys.exit(
            f"Aborted ({e}). Free-tier quota ran out mid-run. Re-run after the daily reset or enable "
            f"billing; partial results were not written."
        )

    with open(args.out, "w") as f:
        json.dump(report, f, indent=2)

    print(f"\n=== Reliability report ({args.model}) ===")
    for variant, v in report["variants"].items():
        s = v["summary"]
        print(f"\n[{variant}] function accuracy: {s['functionAccuracy']*100:.1f}%  "
              f"({s['functionCorrect']}/{s['total']})")
        print(f"  restraint (out-of-scope handled): {s['restraintAccuracy']}  "
              f"forced wrong calls: {s['outOfScopeForcedCalls']}")
        print(f"  param accuracy (routed items): {s['paramAccuracy']}  over {s['paramItems']} items")
        print("  by goal: " + ", ".join(f"{g}={d['accuracy']*100:.0f}%({d['n']})"
                                         for g, d in s["byGoal"].items()))
    if "rich" in report["variants"] and "terse" in report["variants"]:
        r = report["variants"]["rich"]["summary"]["functionAccuracy"]
        t = report["variants"]["terse"]["summary"]["functionAccuracy"]
        print(f"\nKDoc effect: rich {r*100:.1f}% vs terse {t*100:.1f}%  (delta {(r-t)*100:+.1f} pts)")
    print(f"\nWrote {args.out}")


if __name__ == "__main__":
    main()
