---
description: Show the running Dirigible server log in the session (snapshot, or bounded live follow)
---

Show the Dirigible server log (from `.claude/run/dirigible.log`).

Arguments: `$ARGUMENTS`

- **Default (snapshot):** run `node .claude/scripts/dirigible.mjs logs` in the
  foreground. It prints the last 80 lines, which appear directly in the session.
  Pass `--lines N` for a larger snapshot.
- **Live follow:** if the arguments contain `follow`, run
  `node .claude/scripts/dirigible.mjs logs --follow` in the **foreground** (set the
  Bash `timeout` a little above the follow window). It tails for up to ~60s — or
  until the server stops — then returns the captured output. Pass `--seconds N` to
  change the window.

Note: Claude Code does not stream a background process's output into the
conversation live, which is why follow runs in the foreground and returns a
captured window. For a truly continuous live tail, open `.claude/run/dirigible.log`
in your editor/IDE.

If there is no log file, the server isn't running — tell the user to `/dirigible-start` first.
