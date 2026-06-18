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
4. Once ready, show the startup log in the session: run
   `node .claude/scripts/dirigible.mjs logs --lines 40` (a snapshot, printed inline).
   Tell the user they can run `/dirigible-logs` for more, `/dirigible-logs follow` to
   watch live for a bit, or open `.claude/run/dirigible.log` in their editor for a
   continuous tail. (The log is truncated fresh on every start.)

If the start step reports it's already running, tell the user to run `/dirigible-stop` first.
