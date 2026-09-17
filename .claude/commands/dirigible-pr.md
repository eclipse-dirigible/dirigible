---
description: Prepare the branch for a PR — format Java + validate javadoc on changed modules, fix issues
---

Prepare the current branch for a pull request. On its own this command does NOT commit or push —
it leaves that to the user. When it runs as the verification step of `/dirigible-take` or of a
"take this: <issue>" request, the chain in `.claude/docs/take-this-workflow.md` continues with
commit, push and `gh pr create` without asking.

Announce each step to the user as you go (e.g. "Scoping changed modules…", "Formatting modules X, Y…",
"Running the javadoc check…", "Fixing javadoc errors in …", "Done — summary below") so it is always
clear what is happening at the moment.

1. **Scope.** Determine which Maven modules changed:
   `git status --short` and `git diff --name-only master...HEAD` (plus uncommitted changes).
   Map changed files to their owning module directories (the nearest ancestor with a `pom.xml`).

2. **Format.** Run the formatter, scoped to the changed modules for speed:
   `mvn formatter:format -pl <module1>,<module2> -am`
   (fall back to repo-wide `mvn formatter:format` if changes are broad or module mapping is unclear).

3. **Javadoc check.** Run the release-profile javadoc build on the changed modules — this is
   the check CI's release job runs and the default `mvn install` skips:
   `mvn -P release -Dgpg.skip=true -DskipTests -Dlicense.skip=true -Dformatter.skip=true install -pl <modules> -am`
   Watch for the recurring failures (unterminated `{@code`/`{@link}`, unresolved `@link`,
   `no comment` on public SDK members) documented in CLAUDE.md.

4. **Fix and re-run.** Fix any formatter or javadoc errors you can resolve, then re-run the
   relevant step until both pass cleanly. Re-run `mvn formatter:format` last if you edited Java.

5. **Summarize.** Report: which modules were formatted, what javadoc issues were fixed (if any),
   the final pass/fail state, and a short summary of the diff suitable for a PR description.
   When invoked standalone, remind the user the changes are staged for them to review/commit/push;
   inside a take-this run, continue with the commit, push and PR.
