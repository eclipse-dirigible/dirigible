# Dev-loop automation — maintainer reference

Technical reference for the Claude Code slash commands (`.claude/commands/dirigible-*.md`) and the
cross-platform Node.js driver (`.claude/scripts/dirigible.mjs`) that automate the local dev loop.
Read this before changing the commands or the driver. For the **user-facing guide** (what to type,
examples), see the "Claude Code Commands" section of the root [`README.md`](../../README.md).

## Overview

The commands wrap a single dependency-free Node.js driver, so they behave identically on macOS,
Linux, and Windows (Node 22+ is already a build prerequisite — no bash/PowerShell split). The driver
prints timestamped `>> [HH:MM:SS]` progress lines for every step, and the command files instruct the
assistant to run each step as a separate visible tool call, announce it beforehand, and surface those
lines (not a terse summary) — so the user sees progress in any session.

Live process output still can't stream into the chat transcript mid-command (Claude Code shows a
command's output as a block only when it finishes); that's why the build's duration is announced up
front and the log tail runs as a background task.

## Commands → driver

- **`/dirigible-start`** → `dirigible.mjs build quick` (`-T 1C install -P quick-build`, **no clean**
  so runtime data under `./target/dirigible` survives) then `dirigible.mjs start`, then follows the
  log live via `dirigible.mjs logs --follow --seconds 0` **as a background task**. Optional args (off
  by default): `debug` → `start --debug` (JDWP on 8000, `suspend=n`); `clean` → build with `clean` to
  reset the DB/repository. There is **no** separate debug command — `/dirigible-start debug` replaces it.
- **`/dirigible-stop`** → `dirigible.mjs stop`.
- **`/dirigible-logs`** → `dirigible.mjs logs --follow --seconds 0` launched **in the background**
  (continuous live tail; self-stops when the server dies). The user watches it in Claude Code's
  background-task view — the main transcript only shows a command's output as a block when it
  finishes, so live tailing must be a background task. `snapshot` arg → a one-off foreground
  `dirigible.mjs logs --since 1` (last minute) printed inline (verbatim lines **plus** a short
  summary); `snapshot N` → `--since N`.
- **`/dirigible-test`** → `dirigible.mjs test [all|unit|integration]` launched **as a background task**
  (test runs are long; the user watches progress in the background-task view). Default `all` =
  `-P tests`, `unit` = `-P unit-tests`, `integration`/`it` = `-P integration-tests`. Summarize
  pass/fail on completion.
- **`/dirigible-pr`** → format + javadoc-validate the changed Maven modules (the `mvn formatter:format`
  and release-profile javadoc commands from the root CLAUDE.md), fix issues, summarize; does **not**
  commit/push.

## `dirigible.mjs` subcommands

Also runnable directly: `node .claude/scripts/dirigible.mjs <cmd>`.

- `build [quick|full] [--clean]` — `quick` (default) = `mvn -T 1C install -P quick-build`; `full` =
  `mvn install` with unit tests. **No `clean` by default** so the runtime H2 DB + repository under
  `./target/dirigible` survive a rebuild; `--clean` prepends the `clean` goal to wipe `target/`.
  Inherits stdio, exits non-zero on failure so the command stops before launching.
- `start [--debug]` — launches the newest `build/application/target/dirigible-application-*-executable.jar`
  detached (`spawn` `detached:true`+`unref`), **truncates** `.claude/run/dirigible.log` (opens it with
  `'w'` — fresh log per start), writes the PID to `.claude/run/dirigible.pid`, then polls
  `http://localhost:${DIRIGIBLE_SERVER_PORT:-8080}/actuator/health/readiness` via global `fetch`
  (180s budget). Refuses to start if the recorded PID is alive.
- `stop` — reads the PID file; POSIX = SIGTERM then SIGKILL after 15s, Windows =
  `taskkill /PID <pid> /T /F`; removes the PID file. No-op (not an error) when nothing is running.
- `test [all|unit|integration]` — runs the test suites with the exact maven invocations CI uses
  (`.github/workflows/build.yml`): `all` (default) = `mvn clean install -P tests`, `unit` =
  `-P unit-tests`, `integration` (alias `it`) = `-P integration-tests`. `clean` is **always**
  included — it matches CI and wipes the stale file-backed H2 under `tests/tests-integrations/target`
  that otherwise breaks IT re-runs; `-D selenide.headless=true` is added whenever integration tests
  run (Selenide drives Chrome, ttyd must be installed). Inherits stdio, exits non-zero on failure.
  An unrecognized mode fails fast (it is **not** silently treated as `all`).
- `logs [--lines N | --since MIN] [--follow] [--seconds N]` — reads `dirigible.log` with Node fs only
  (no `tail`). Default = snapshot: prints the last `N` lines (default 80) and exits, so the output
  reliably appears in the session. `--since MIN` selects the backlog by time instead — every entry
  logged in the last `MIN` minutes — by comparing each line's `YYYY-MM-DD HH:MM:SS.mmm` prefix as a
  **string** against a formatted cutoff (the fixed-width local timestamp sorts chronologically as
  text, so this avoids slow `new Date()` parsing on big logs; continuation lines inherit the previous
  entry's verdict so stack traces stay whole); `/dirigible-logs snapshot` uses `--since 1`. `--follow`
  tails appended bytes via an offset+poll loop (handles truncation/rotation) until the recorded PID
  dies; bounded by `--seconds` (default 60) for a foreground run that returns its window, or
  `--seconds 0` for an unbounded follow used by the background live-tail task (`/dirigible-logs`).

## Cross-platform mechanics & permissions

Relies only on Node built-ins: `process.kill(pid, 0)` for liveness, `spawnSync('mvn', …, {shell:true})`
so Windows resolves `mvn.cmd` via PATHEXT, `readdirSync`+mtime sort for the jar glob, `fetch` for
readiness, and `openSync`/`readSync` for the log read (no `curl`/`tail` dependency). `.claude/run/`
is ephemeral runtime state.

Execution is whitelisted team-wide in the committed `.claude/settings.json` —
`Bash(node .claude/scripts/dirigible.mjs *)` plus the narrow `Bash(git status *)` /
`Bash(git diff *)` / `Bash(mvn formatter:format *)` / `Bash(mvn -P release *)` rules `/dirigible-pr`
needs — so the commands run without per-call prompts. The `.gitignore` keeps `.claude/` local state
ignored but tracks `.claude/commands/`, `.claude/scripts/`, and `.claude/settings.json`.
