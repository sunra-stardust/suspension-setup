#!/usr/bin/env python3
"""PreToolUse guard for Claude Code (docs/HARNESS.md §5.3). Reads the hook JSON on stdin.

  guard.py bash   Bash tool: denies pushes to main, force pushes without lease, ref/tag deletion,
                  tag pushes, release/secret commands; asks before a rollback; denies a push whose
                  commit was not verified (build/verify-stamp, written by `./gradlew verify`).
  guard.py paths  Edit/Write tools: asks before a protected file (risk class R3) changes.

Standard library only. Fails open on its own errors: the GitHub ruleset on main is the
server-side backstop, and a broken guard must not stop all work.
"""
import fnmatch
import json
import os
import re
import shlex
import subprocess
import sys

HARNESS = "docs/HARNESS.md"


def decide(decision, reason):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": decision,
        "permissionDecisionReason": reason,
    }}))
    sys.exit(0)


def repo_root(data):
    root = os.environ.get("CLAUDE_PROJECT_DIR") or data.get("cwd") or os.getcwd()
    try:
        out = subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=root,
                             capture_output=True, text=True, timeout=10)
        if out.returncode == 0:
            return out.stdout.strip()
    except (OSError, subprocess.SubprocessError):
        pass
    return root


def git(root, *args):
    out = subprocess.run(["git", *args], cwd=root, capture_output=True, text=True, timeout=30)
    return out.stdout.strip() if out.returncode == 0 else ""


# ---------- bash ----------

def segments(command):
    """Split a shell command line into simple commands (token lists); quotes stay intact."""
    for part in re.split(r"&&|\|\||[;|\n]", command):
        try:
            tokens = shlex.split(part, comments=True)
        except ValueError:
            tokens = part.split()
        if tokens:
            yield tokens


def subcommand(tokens, program):
    """Arguments after `<program> [global options] <sub>`, e.g. git -C dir push … → (push, […])."""
    if not tokens or os.path.basename(tokens[0]) != program:
        return None, []
    i = 1
    while i < len(tokens) and tokens[i].startswith("-"):
        i += 2 if tokens[i] in ("-C", "-c", "-R", "--repo") else 1
    return (tokens[i], tokens[i + 1:]) if i < len(tokens) else (None, [])


def sdk_available(root):
    props = os.path.join(root, "local.properties")
    if os.path.exists(props):
        with open(props, encoding="utf-8", errors="replace") as f:
            if any(line.startswith("sdk.dir=") for line in f):
                return True
    return bool(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT"))


def check_push(root, args):
    opts = [a for a in args if a.startswith("-")]
    positional = [a for a in args if not a.startswith("-")]
    lease = any(a.startswith("--force-with-lease") for a in opts)
    if ("--force" in opts or "-f" in opts or any(re.match(r"^-[a-z]*f", a) for a in opts if not a.startswith("--"))) and not lease:
        decide("deny", "Force push without --force-with-lease is blocked. Use --force-with-lease, and only on your own claude/* branch.")
    if any(a in ("--delete", "-d", "--mirror", "--all", "--tags", "--follow-tags", "--prune") for a in opts):
        decide("deny", "Deleting refs and pushing tags or all branches is blocked — tags and releases belong to the pipeline, deletions to the owner.")
    refspecs = positional[1:]  # positional[0] is the remote
    for spec in refspecs:
        if spec.startswith("+"):
            decide("deny", "A '+' refspec is a force push; use --force-with-lease on your own claude/* branch.")
        if spec.startswith(":"):
            decide("deny", "Pushing ':<ref>' deletes a remote ref — blocked.")
        dst = spec.split(":", 1)[-1]
        if dst in ("main", "refs/heads/main") or dst.startswith("refs/tags/") or dst.startswith("v") and re.match(r"^v\d", dst):
            decide("deny", f"Agents never push main or tags. Push your claude/* branch; the pipeline moves main (see {HARNESS}).")
    if not refspecs and git(root, "rev-parse", "--abbrev-ref", "HEAD") == "main":
        decide("deny", "You are on main: agents never push main. Create a claude/<topic> branch and push that.")

    # Verify before push (CLAUDE.md), made mechanical.
    if not sdk_available(root):
        return  # no Android SDK in this session: CI verifies every push instead (CLAUDE.md)
    src = refspecs[0].split(":", 1)[0] if refspecs and ":" in refspecs[0] else "HEAD"
    tree = git(root, "rev-parse", f"{src or 'HEAD'}^{{tree}}") or git(root, "rev-parse", "HEAD^{tree}")
    stamp_file = os.path.join(root, "build", "verify-stamp")
    stamp = open(stamp_file, encoding="utf-8").read().strip() if os.path.exists(stamp_file) else ""
    if not tree or stamp != tree:
        decide("deny", "This commit was not verified: run `./gradlew verify` on exactly this state (all changes committed), then push again. "
                       "The verify task records the tested tree in build/verify-stamp.")


def check_bash(root, command):
    for tokens in segments(command):
        sub, args = subcommand(tokens, "git")
        if sub == "push":
            check_push(root, args)
        elif sub == "tag" and any(a in ("-d", "--delete") for a in args):
            decide("deny", "Deleting tags is blocked (CLAUDE.md rule 7).")
        elif sub == "update-ref" and "-d" in args:
            decide("deny", "Deleting refs is blocked.")
        sub, args = subcommand(tokens, "gh")
        if sub == "release" and args and args[0] in ("delete", "delete-asset", "create", "upload"):
            decide("deny", "Releases are published only by the pipeline and deleted only by the owner (CLAUDE.md rule 7).")
        if sub == "secret" or sub == "variable":
            decide("deny", "Secrets and variables are owner-only (CLAUDE.md rule 7).")
        if sub == "workflow" and args[:1] == ["run"] and any("rollback" in a for a in args):
            decide("ask", "A rollback republishes old app code to the owner's phone — the owner decides (docs/HARNESS.md H7).")
        if sub == "pr" and args[:1] == ["merge"]:
            decide("ask", "Merging is the owner's decision (docs/HARNESS.md §4).")
        if sub == "repo" and args[:1] in (["delete"], ["edit"]):
            decide("deny", "Repository settings are owner-only (docs/HARNESS.md H8).")


# ---------- paths ----------

def glob_regex(pattern):
    out, i = "", 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out, i = out + "(?:.*/)?", i + 3
        elif pattern.startswith("**", i):
            out, i = out + ".*", i + 2
        elif pattern[i] == "*":
            out, i = out + "[^/]*", i + 1
        else:
            out, i = out + re.escape(pattern[i]), i + 1
    return re.compile(out + r"\Z")


def protected_patterns(root):
    path = os.path.join(root, ".claude", "hooks", "protected-paths.txt")
    with open(path, encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip() and not line.startswith("#")]


def is_protected(root, file_path):
    rel = os.path.relpath(os.path.abspath(os.path.join(root, file_path)), root).replace(os.sep, "/")
    if rel.startswith("../"):
        return None
    return rel if any(glob_regex(p).match(rel) or fnmatch.fnmatch(rel, p) for p in protected_patterns(root)) else None


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        data = json.load(sys.stdin)
        root = repo_root(data)
        tool_input = data.get("tool_input") or {}
        if mode == "bash":
            check_bash(root, tool_input.get("command") or "")
        elif mode == "paths":
            target = tool_input.get("file_path") or tool_input.get("notebook_path") or ""
            rel = is_protected(root, target) if target else None
            if rel:
                decide("ask", f"{rel} is protected (risk class R3, {HARNESS}): the change needs the owner's merge — "
                              "commit with [owner-review] as the last body line and open a PR labelled needs-owner.")
    except SystemExit:
        raise
    except Exception as e:  # noqa: BLE001 - fail open, but say so
        print(f"guard.py: skipped ({type(e).__name__}: {e})", file=sys.stderr)
    sys.exit(0)


if __name__ == "__main__":
    main()
