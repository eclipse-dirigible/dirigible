---
description: Stop the locally running Dirigible fat jar
---

Stop the running Dirigible instance (cross-platform — macOS / Linux / Windows).

Run `node .claude/scripts/dirigible.mjs stop` and report the outcome (stopped, or
already not running). The script reads the PID from `.claude/run/dirigible.pid`,
terminates it (graceful SIGTERM then SIGKILL on POSIX; `taskkill /T /F` on Windows),
and removes the PID file.
