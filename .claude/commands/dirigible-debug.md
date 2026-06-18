---
description: Build (quick-build) and start Dirigible with JDWP remote debugging on port 8000
---

Build and start Dirigible with remote debugging enabled (cross-platform — macOS / Linux / Windows).

1. Run `node .claude/scripts/dirigible.mjs build quick`.
   - If the build fails, STOP. Show the relevant Maven error and do not start anything.
2. On a successful build, run `node .claude/scripts/dirigible.mjs start --debug`.
   - The script launches the jar in the background, records the PID, and polls
     `/actuator/health/readiness` until the app is up (or times out).
3. Report the PID, log path, UI URL, and the JDWP attach port (8000,
   transport=dt_socket, suspend=n — the JVM does not wait for a debugger).
