---
description: Take a GitHub issue end to end — worktree off origin/master, implement + verify, commit, push, open the PR
---

Take the issue named in the arguments all the way to an open pull request, following
`.claude/docs/take-this-workflow.md` (imported into CLAUDE.md) exactly. This command IS the
standing authorization for every step below: do not stop to ask whether to branch, commit, push or
open the PR. Never merge.

Arguments: `$ARGUMENTS` — an issue URL or number (`https://github.com/eclipse-dirigible/dirigible/issues/NNNN`
or `#NNNN`). Refuse politely if it is neither.

**Keep the user informed at every step.** Before each command post a one-line note saying what you are
about to do; run each step as a separate, visible tool call; surface the key result lines.

1. **Read the issue.** `gh issue view NNNN --repo eclipse-dirigible/dirigible --comments`. Restate
   the defect and the acceptance criteria in two sentences. If the issue admits materially different
   designs, pick the one the issue's own text points at and say so — do not stop to ask unless the
   choices lead to different deliverables.

2. **Check the shared checkout, then branch in a worktree.** `git status -sb` in the current checkout
   (if it is dirty, leave it alone — that is someone else's work). Then
   `git fetch origin && git worktree add ../wt-NNNN -b issue-NNNN-<slug> origin/master`, `cd` there,
   and confirm with `git status -sb` that HEAD is the new branch. All further work happens in the
   worktree.

3. **Isolate the Maven repository if anything else may build concurrently** (`pgrep -fl maven`,
   `git worktree list` showing other worktrees): clone `~/.m2/repository` to `~/.m2/repo-wt-NNNN` per
   the workflow doc and pass `-Dmaven.repo.local=$HOME/.m2/repo-wt-NNNN` to every Maven command.

4. **Implement.** Surgical changes traceable to the issue. For a template or generator change, grep
   the sibling templates and the other branches of the same template for the same code shape and fix
   them all. Write the test that reproduces the defect first when that is feasible.

5. **Verify.** In this order, each as its own visible step:
   - `mvn formatter:format` on the changed modules (`-pl <modules> -am`);
   - `find . -name formatter-maven-cache.properties -path '*/target/*' -delete` then
     `mvn -T 1C formatter:validate` — must be `BUILD SUCCESS`;
   - the changed modules' unit suites: `mvn -pl <modules> -am test -Dlicense.skip=true`;
   - the integration tests that cover the change, by name, after a
     `mvn -T 1C -P quick-build install -DskipTests -Dmaven.gitcommitid.skip=true` in the worktree
     (`rm -rf tests/tests-integrations/target/dirigible` first);
   - if the change added or changed Javadoc on a public SDK type, the release-profile javadoc build
     on the touched modules.
   If any step is red for a cause in your change, fix it and re-run. If it is red for a cause outside
   your change, say so with the output — that is a block, and the one legitimate reason to return
   without a PR.

6. **Stage explicitly and commit.** `git status --short`; if `modules/parsers/typescript/**` shows as
   modified, `git checkout -- modules/parsers/typescript` — it is build churn, never your change.
   `git add <the intended paths>`, then a plain `git commit` (identity from git config, never set by
   hand, never `--no-verify`) with subject `<area>: <what changed> (#NNNN)` and a body that states
   the cause, the change and what was verified.

7. **Push and open the PR.** `gh pr list --head issue-NNNN-<slug> --state all` (must be empty),
   `git push -u origin issue-NNNN-<slug>`, confirm `git rev-parse HEAD origin/issue-NNNN-<slug>`
   match, then `gh pr create --base master --title "<commit subject>" --body "<cause / change /
   verification> ... Fixes #NNNN"`.

8. **Report.** The PR URL, the branch and worktree path, what was verified and what was not, and the
   cleanup to run after the merge (`git worktree remove ../wt-NNNN`, `git branch -d`,
   `rm -rf ~/.m2/repo-wt-NNNN`).
