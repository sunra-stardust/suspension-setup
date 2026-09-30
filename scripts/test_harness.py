#!/usr/bin/env python3
"""Tests for the Claude Code guard hook (.claude/hooks/guard.py). Standard library only.

  python3 scripts/test_harness.py

Builds a throw-away git repo, copies the hook and its path list into it and checks every rule
(docs/HARNESS.md §5.3). Runs in CI (pr-check.yml → Harness).
"""
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
GUARD = ROOT / ".claude" / "hooks" / "guard.py"
PATHS = ROOT / ".claude" / "hooks" / "protected-paths.txt"


class GuardTest(unittest.TestCase):

    def setUp(self):
        self.repo = pathlib.Path(tempfile.mkdtemp())
        self.git("init", "-q", "-b", "main")
        self.git("config", "user.email", "t@example.com")
        self.git("config", "user.name", "t")
        (self.repo / ".claude" / "hooks").mkdir(parents=True)
        shutil.copy(PATHS, self.repo / ".claude" / "hooks" / "protected-paths.txt")
        (self.repo / "README.md").write_text("x\n")
        self.git("add", "-A")
        self.git("commit", "-q", "-m", "init")
        self.git("checkout", "-q", "-b", "claude/topic")
        (self.repo / "local.properties").write_text("sdk.dir=/sdk\n")  # untracked, like the real one

    def tearDown(self):
        shutil.rmtree(self.repo)

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.repo, check=True, capture_output=True, text=True).stdout.strip()

    def stamp(self):
        (self.repo / "build").mkdir(exist_ok=True)
        (self.repo / "build" / "verify-stamp").write_text(self.git("rev-parse", "HEAD^{tree}") + "\n")

    def run_guard(self, mode, tool_input):
        env = dict(os.environ, CLAUDE_PROJECT_DIR=str(self.repo))
        env.pop("ANDROID_HOME", None)
        env.pop("ANDROID_SDK_ROOT", None)
        out = subprocess.run([sys.executable, str(GUARD), mode], input=json.dumps({"tool_input": tool_input, "cwd": str(self.repo)}),
                             capture_output=True, text=True, env=env, timeout=30)
        self.assertEqual(0, out.returncode, out.stderr)
        return json.loads(out.stdout)["hookSpecificOutput"]["permissionDecision"] if out.stdout.strip() else "allow"

    def bash(self, command):
        return self.run_guard("bash", {"command": command})

    # --- pushes ---

    def test_push_to_main_is_denied(self):
        self.stamp()
        for cmd in ("git push origin main", "git push origin HEAD:main", "git push origin claude/topic:refs/heads/main",
                    "ls && git push -u origin HEAD:main", "git -C . push origin main"):
            self.assertEqual("deny", self.bash(cmd), cmd)

    def test_bare_push_on_main_is_denied(self):
        self.git("checkout", "-q", "main")
        self.stamp()
        self.assertEqual("deny", self.bash("git push"))

    def test_verified_push_of_own_branch_is_allowed(self):
        self.stamp()
        self.assertEqual("allow", self.bash("git push -u origin claude/topic"))
        self.assertEqual("allow", self.bash("git push --force-with-lease origin claude/topic"))

    def test_unverified_push_is_denied(self):
        self.assertEqual("deny", self.bash("git push -u origin claude/topic"))
        self.stamp()
        (self.repo / "a.txt").write_text("new\n")
        self.git("add", "a.txt")
        self.git("commit", "-q", "-m", "change after verify")
        self.assertEqual("deny", self.bash("git push -u origin claude/topic"))

    def test_without_android_sdk_ci_verifies_instead(self):
        (self.repo / "local.properties").unlink()
        self.assertEqual("allow", self.bash("git push -u origin claude/topic"))

    def test_force_and_deletion_are_denied(self):
        self.stamp()
        for cmd in ("git push --force origin claude/topic", "git push -f origin claude/topic", "git push -uf origin claude/topic",
                    "git push origin +claude/topic", "git push origin :claude/topic", "git push --delete origin claude/topic",
                    "git push --tags", "git push origin v0.6.40", "git push origin refs/tags/v1", "git tag -d v0.6.32"):
            self.assertEqual("deny", self.bash(cmd), cmd)

    def test_text_that_mentions_a_push_is_not_a_push(self):
        self.assertEqual("allow", self.bash('echo "git push origin main"'))
        self.assertEqual("allow", self.bash("git log --oneline -3"))

    # --- gh ---

    def test_gh_rules(self):
        self.assertEqual("deny", self.bash("gh release delete v0.6.32"))
        self.assertEqual("deny", self.bash("gh release create v9"))
        self.assertEqual("deny", self.bash("gh secret set X"))
        self.assertEqual("ask", self.bash("gh workflow run rollback.yml -f version=0.6.30"))
        self.assertEqual("ask", self.bash("gh pr merge 15"))
        self.assertEqual("allow", self.bash("gh run list --workflow ship.yml --limit 3"))
        self.assertEqual("allow", self.bash("gh release view --json tagName"))

    # --- protected paths ---

    def test_protected_paths_ask(self):
        for path in ("CLAUDE.md", ".claude/settings.json", ".claude/hooks/guard.py", ".github/workflows/ship.yml",
                     "app/src/main/AndroidManifest.xml", "app/src/github/AndroidManifest.xml",
                     "app/src/main/java/dev/suspension/app/data/GarageRepository.kt",
                     "app/src/main/java/dev/suspension/app/update/Updater.kt", "gradle/libs.versions.toml",
                     "app/build.gradle.kts", str(self.repo / "CLAUDE.md")):
            self.assertEqual("ask", self.run_guard("paths", {"file_path": path}), path)

    def test_ordinary_paths_are_allowed(self):
        for path in ("app/src/main/java/dev/suspension/app/ui/SetupScreen.kt",
                     "app/src/main/java/dev/suspension/app/data/ScenarioData.kt",
                     "app/src/main/res/values/strings.xml", "app/src/main/resources/catalog/catalog.json",
                     "app/src/test/java/dev/suspension/app/CatalogDataTest.kt", "research/makers.json",
                     "docs/ROADMAP.md", ".claude/skills/ship/SKILL.md", "/tmp/elsewhere.txt"):
            self.assertEqual("allow", self.run_guard("paths", {"file_path": path}), path)

    def test_broken_input_fails_open(self):
        out = subprocess.run([sys.executable, str(GUARD), "bash"], input="not json", capture_output=True, text=True, timeout=30)
        self.assertEqual(0, out.returncode)
        self.assertEqual("", out.stdout.strip())


if __name__ == "__main__":
    unittest.main(verbosity=2)
