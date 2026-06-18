---
description: Build (quick) and start Dirigible in the background; optional debug + clean; then follow logs live
---

Build and start Dirigible locally (cross-platform — macOS / Linux / Windows).

Arguments: `$ARGUMENTS`
Optional, off by default: `debug` (enable JDWP remote debugging on port 8000) and/or
`clean` (wipe `target/` first, resetting the runtime DB/repository). Either, both, or neither.

1. Build with the quick profile (always `mvn install -P quick-build` — no clean by default, so the
   runtime H2 DB and repository under `./target/dirigible` survive the rebuild):
   - If the arguments contain `clean`, run `node .claude/scripts/dirigible.mjs build quick --clean`.
   - Otherwise run `node .claude/scripts/dirigible.mjs build quick`.
   - If the build fails, STOP. Show the relevant Maven error and do not start anything.
2. Start the jar (the script truncates `dirigible.log` fresh, launches in the background, records
   the PID, and polls `/actuator/health/readiness` until the app is up):
   - If the arguments contain `debug`, run `node .claude/scripts/dirigible.mjs start --debug`
     (JDWP on 8000, transport=dt_socket, suspend=n — the JVM does not wait for a debugger).
   - Otherwise run `node .claude/scripts/dirigible.mjs start`.
3. Report the PID, the log path, the UI URL (http://localhost:8080, admin/admin), and — when
   `debug` was given — the JDWP attach port (8000).
4. Stream the server log live: run `node .claude/scripts/dirigible.mjs logs --follow --seconds 0`
   **as a background process** (`run_in_background: true`). It updates live in Claude Code's
   background-task view and self-stops when the server stops. (Available anytime via `/dirigible-logs`.)

If the start step reports it's already running, tell the user to run `/dirigible-stop` first.
