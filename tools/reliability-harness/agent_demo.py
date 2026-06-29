#!/usr/bin/env python3
"""
Real agent loop. A natural-language instruction is sent to a model, which chooses one AppFunction
and its arguments; that function is then executed on the running app via `adb cmd app_function`, so
the change appears in the UI. This is the same loop the on-device Gemini assistant would run — only
the transport is adb instead of the (EAP-gated) system assistant.

Usage:
  GITHUB_TOKEN=$(gh auth token) python3 agent_demo.py "add a high priority task to call the plumber"
                                                       [--model openai/gpt-4.1]
"""
import json
import os
import subprocess
import sys
import time
import urllib.request

from harness import declarations, to_openai_tools  # reuse the function declarations

PKG = "io.github.tonytonycoder11.agentictaskmanager"
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
ENDPOINT = "https://models.github.ai/inference/chat/completions"
CLASS = {
    "getActionableTasks": "TaskQueryFunctions", "getBlockingOverdueTasks": "TaskQueryFunctions",
    "addTask": "TaskActionFunctions", "completeTask": "TaskActionFunctions", "deleteTask": "TaskActionFunctions",
}
SYSTEM = (
    "You operate a task manager by calling its functions. For the user's request, call exactly one "
    "function with the best arguments. Resolve a task referred to by name to its id using the "
    "'Known tasks' list."
)


def adb_shell(cmd):
    # adb joins argv with spaces and the device shell re-parses, so the whole device command is
    # passed as ONE string with the JSON shell-single-quoted, to survive that re-split.
    return subprocess.run([ADB, "shell", cmd], capture_output=True, text=True).stdout


def _sq(s):  # shell single-quote
    return "'" + s.replace("'", "'\\''") + "'"


def execute(func, model_args):
    """Run the chosen AppFunction on the device. Query functions take no params; actions take {params:...}."""
    fid = f"{PKG}.agent.{CLASS[func]}#{func}"
    params = "{}" if CLASS[func] == "TaskQueryFunctions" else json.dumps({"params": model_args})
    cmd = (f"cmd app_function execute-app-function --package {PKG} --function {fid} "
           f"--parameters {_sq(params)} --brief-yaml")
    return adb_shell(cmd).strip()


def known_tasks():
    """Read the current actionable tasks from the device so the model can resolve names to ids."""
    cmd = (f"cmd app_function execute-app-function --package {PKG} "
           f"--function {PKG}.agent.TaskQueryFunctions#getActionableTasks --parameters {_sq('{}')}")
    try:
        tasks = json.loads(adb_shell(cmd))["androidAppfunctionsReturnValue"][0].get("tasks", [])
        return [(t["id"][0], t["title"][0], t["status"][0]) for t in tasks]
    except Exception:  # noqa: BLE001
        return []


def choose_function(token, model, instruction, ctx):
    tools = to_openai_tools(declarations(rich=True))
    context = ("Known tasks:\n" + "\n".join(f'- {i} = "{t}" ({s})' for i, t, s in ctx) + "\n\n") if ctx else ""
    body = {
        "model": model,
        "messages": [{"role": "system", "content": SYSTEM},
                     {"role": "user", "content": context + "Request: " + instruction}],
        "tools": tools, "tool_choice": "auto", "temperature": 0,
    }
    data = json.dumps(body).encode()
    payload = None
    for attempt in range(4):
        req = urllib.request.Request(ENDPOINT, data=data, method="POST")
        req.add_header("Content-Type", "application/json")
        req.add_header("Authorization", f"Bearer {token}")
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                payload = json.loads(r.read())
            break
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 503) and attempt < 3:
                time.sleep(8 * (attempt + 1))
                continue
            raise
    if payload is None:
        sys.exit("model unreachable (rate limited). Try a different --model with free quota.")
    msg = payload["choices"][0]["message"]
    if not msg.get("tool_calls"):
        return None, {}, msg.get("content", "")
    fc = msg["tool_calls"][0]["function"]
    return fc["name"], json.loads(fc.get("arguments") or "{}"), None


def main():
    argv = sys.argv[1:]
    model = "openai/gpt-4.1"
    if "--model" in argv:
        i = argv.index("--model")
        model = argv[i + 1]
        argv = argv[:i] + argv[i + 2:]
    instruction = " ".join(argv).strip()
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not instruction or not token:
        sys.exit('Usage: GITHUB_TOKEN=$(gh auth token) python3 agent_demo.py "<instruction>" [--model ID]')

    ctx = known_tasks()
    func, fargs, text = choose_function(token, model, instruction, ctx)
    print(f'Instruction : "{instruction}"')
    print(f"Model       : {model}")
    if not func:
        print(f"Decision    : NO function call (model replied: {text[:140]!r})")
        return
    print(f"Decision    : {func}({json.dumps(fargs)})")
    print("Executed on device ->")
    print(execute(func, fargs))


if __name__ == "__main__":
    main()
