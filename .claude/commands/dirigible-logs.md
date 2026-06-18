---
description: Stream the running Dirigible server log live (continuous background task)
---

Stream the Dirigible server log (`.claude/run/dirigible.log`) live.

Arguments: `$ARGUMENTS`

Before running, tell the user what you're doing (starting a live tail, or printing a snapshot) and
where to watch it. After launching, report the background task id and its output-file path.

- **Default (live):** run `node .claude/scripts/dirigible.mjs logs --follow --seconds 0`
  **as a background process** (`run_in_background: true`). It prints the recent
  backlog then tails the log continuously, updating live, and self-stops when the
  server stops. Tell the user to watch it in Claude Code's background-task / output
  view — that view updates in real time (the main chat transcript only shows a
  command's output as a block when it finishes, so the live view is the task panel).
- **Snapshot:** if the arguments contain `snapshot`, run
  `node .claude/scripts/dirigible.mjs logs --since 1` in the foreground instead — it prints
  every log entry from the last 1 minute and exits. If the user gives a number after
  `snapshot` (e.g. `/dirigible-logs snapshot 5`), use that many minutes: `--since 5`.
  **Show the command's actual log output verbatim in the session — paste the real log lines
  (in a fenced code block), do NOT summarize, paraphrase, or describe them.** Only after the raw
  lines may you add a brief note (e.g. that the window was idle, or point out an obvious error).

If there is no log file, the server isn't running — tell the user to `/dirigible-start` first.
