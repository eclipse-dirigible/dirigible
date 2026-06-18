---
description: Run the test suites — all by default, or only unit / only integration tests
---

Run the Dirigible test suites (cross-platform — macOS / Linux / Windows).

Arguments: `$ARGUMENTS`
Optional mode (default = all): `unit` (unit tests only) or `integration` / `it` (integration
tests only). Anything else is rejected by the script.

**Keep the user informed.** Before running, post a one-line note saying which suite you're about
to run and warn that it can take many minutes (integration tests boot the full app and drive
headless Chrome, and require `ttyd` to be installed). Surface the script's `>> [time] …`
progress/result lines rather than replacing them with a terse summary.

1. Pick the mode from the arguments: `unit` → unit only, `integration` or `it` → integration
   only, otherwise **all** (unit + integration).
2. Run the matching command **as a background task** (`run_in_background: true`) — test runs are
   long, so the user watches live progress in Claude Code's background-task view. Report the
   background task id and its output-file path.
   - all (default): `node .claude/scripts/dirigible.mjs test`
   - unit only: `node .claude/scripts/dirigible.mjs test unit`
   - integration only: `node .claude/scripts/dirigible.mjs test integration`
   The script always does a `clean install` (matching CI, and wiping the stale H2 under
   `tests/tests-integrations/target` that otherwise breaks IT re-runs) and adds
   `-D selenide.headless=true` whenever integration tests run.
3. When the task finishes, summarize: PASSED or FAILED and how long it took (from the final
   `Tests PASSED/FAILED …` line). On failure, call out the failing test class(es)/module from
   the Maven output and point to the reports — surefire reports under each module's
   `target/surefire-reports`, and for integration tests the Selenide screenshots/reports under
   `tests/tests-integrations/build/reports/tests`.
