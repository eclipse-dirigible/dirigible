---
description: Build (quick-build) and start the Dirigible fat jar in the background
---

Build and start Dirigible locally (cross-platform — macOS / Linux / Windows).

1. Run `node .claude/scripts/dirigible.mjs build quick`.
   - If the build fails, STOP. Show the relevant Maven error and do not start anything.
2. On a successful build, run `node .claude/scripts/dirigible.mjs start`.
   - The script launches the jar in the background, records the PID, and polls
     `/actuator/health/readiness` until the app is up (or times out).
3. Report the PID, the log path, and the UI URL (http://localhost:8080, admin/admin).

If the start step reports it's already running, tell the user to run `/dirigible-stop` first.
