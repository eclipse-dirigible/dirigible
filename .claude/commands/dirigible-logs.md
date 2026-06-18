---
description: Stream the running Dirigible server log live in the session
---

Follow the Dirigible server log in real time.

Run `node .claude/scripts/dirigible.mjs logs` **as a background process**
(`run_in_background: true`) so its output streams into the session live. It prints
the last 50 lines of `.claude/run/dirigible.log`, then follows new output until the
server stops (it detaches itself when the server process is gone) or the command is
stopped. The server keeps running either way.

- Pass `--lines N` to change the initial backlog (e.g. `... logs --lines 200`).
- If there is no log file yet, the follower waits briefly, then reports that
  Dirigible isn't running — start it first with `/dirigible-start`.
