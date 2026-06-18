---
description: Stop the locally running Dirigible fat jar
---

Stop the running Dirigible instance (cross-platform — macOS / Linux / Windows).

Tell the user you're stopping the server, then run `node .claude/scripts/dirigible.mjs stop` and
surface the script's `>> [time] …` lines (which PID it signalled, graceful-wait, result) rather
than just saying "done". The script reads the PID from `.claude/run/dirigible.pid`, terminates it
(graceful SIGTERM then SIGKILL on POSIX; `taskkill /T /F` on Windows), and removes the PID file.
