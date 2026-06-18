---
description: Build (quick-build) and start the Dirigible fat jar in the background
---

Build and start Dirigible locally (cross-platform — macOS / Linux / Windows).

Arguments: `$ARGUMENTS`

1. Build with the quick profile (always `mvn install -P quick-build` — no clean by default, so the
   runtime H2 DB and repository under `./target/dirigible` survive the rebuild):
   - If the arguments above contain `clean` (e.g. `/dirigible-start clean` or `--clean`), run
     `node .claude/scripts/dirigible.mjs build quick --clean` to wipe `target/` and start from a
     fresh DB/repository.
   - Otherwise run `node .claude/scripts/dirigible.mjs build quick`.
   - If the build fails, STOP. Show the relevant Maven error and do not start anything.
2. On a successful build, run `node .claude/scripts/dirigible.mjs start`.
   - The script launches the jar in the background, records the PID, and polls
     `/actuator/health/readiness` until the app is up (or times out).
3. Report the PID, the log path, and the UI URL (http://localhost:8080, admin/admin).
4. Once ready, stream the server log live: run `node .claude/scripts/dirigible.mjs logs`
   **as a background process** (`run_in_background: true`) so new output flows into
   the session. It detaches itself when the server stops; the server keeps running.

If the start step reports it's already running, tell the user to run `/dirigible-stop` first.
