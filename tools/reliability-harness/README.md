# Reliability harness

Exposing functions to an agent is easy. The honest question is: when a person asks for something in
their own words, how often does the agent pick the right function with the right arguments — and
does that hold across different models, not just one? This harness measures exactly that. The app is
built so any function-calling model can drive it (Gemini is the reference), so the harness runs the
same dataset against several models and compares.

## What it measures

For each natural-language intent the harness sends the model the five functions as tool
declarations, a minimal id↔title directory of the current tasks (so id-based parameters are
resolvable), and the user's request. It records the function the model chose and the arguments it
extracted, then scores them:

- **Function accuracy** — did it call the function the intent actually meant?
- **Restraint** — for out-of-scope requests ("what's the weather"), did it correctly call *nothing*?
- **Parameter accuracy** — on correctly-routed write calls, did it extract the right title, id,
  dependency, priority, due date, recurrence?

Each metric is reported overall and per goal.

## Providers and models

```bash
# Gemini (reference). Needs a key in ../../gemini.api.key (git-ignored).
python3 harness.py --dataset dataset.json --out report.json --provider gemini --model gemini-2.5-flash

# GitHub Models (free with a GitHub token; OpenAI-compatible). Many models, e.g. gpt-4o-mini,
# gpt-4o, meta/llama-3.3-70b-instruct, mistral-ai/mistral-small-2503.
GITHUB_TOKEN=$(gh auth token) python3 harness.py --dataset dataset.json --out report.json \
    --provider github --model openai/gpt-4o-mini
```

Other options: `--variants` (`rich`, `terse`, or both), `--goals addTask,deleteTask` (run a subset),
`--limit N`, `--sleep` (pacing). The run writes a full per-item report and prints a summary,
including the rich-vs-terse delta. It paces itself, backs off on rate limits, and aborts cleanly
when a free quota is exhausted instead of grinding.

## The description experiment

A function's description (its KDoc) is the prompt the model reads to decide whether and how to call
it. The harness runs the dataset twice — **rich** (the real descriptions) and **terse** (one-liners
like "Add a task.") — to measure how much the wording moves accuracy.

The result is not the naive "more text is better". In practice the effect cuts both ways: verbose
*safety* caveats ("destructive and irreversible — confirm first") can make a model abstain from a
tool call it should make, while a terse description makes it act. The project's descriptions are
therefore written **action-first** — they state plainly when to call the function and keep the
safety semantics as instructions to act safely (e.g. "always call delete; passing confirmed=false is
the safe first step"), rather than as warnings that suppress the call. The harness is how that choice
is checked, and re-checked across models.

## How scoring works, and its limits

Function scoring is exact (predicted name vs expected; "no call" is the expected answer for
out-of-scope intents). Parameter scoring is lenient where exactness would be unfair: a title matches
on a normalized substring, a dependency matches whether the model passed a title or an id, a due date
counts if one was extracted, and priority/recurrence match case-insensitively. Calls run at
temperature 0 for repeatability, but a hosted model is not perfectly deterministic and per-goal
counts are small (10–12), so treat small single-run differences as noise; the signal is in
consistent patterns and in agreement across models.

End-to-end execution against the running app is verified separately (`adb shell cmd
app_function execute-app-function`, and the official Testing Agent).
