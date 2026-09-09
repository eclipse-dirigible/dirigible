## "Take this issue" means ship the PR

**Trigger.** Any of these, from anyone, is a standing authorization for the WHOLE chain below, end to
end, without asking at any step: `take this: <issue url>`, `take this issue`, `take #NNNN`,
`implement #NNNN`, `fix #NNNN`, `fix this`, `do it`, or the `/dirigible-take <issue url>` command.
Never stop after implementing to ask whether to branch, commit, push or open the PR - the asking is the
thing being complained about. The only reason to come back without a PR URL is a genuine block: tests
red for a cause outside the change, or a design fork whose branches lead to materially different work.
Then say what blocks, what you did instead, and where the branch is.

**Never merge.** This repository is PR-only: the chain ends at the PR URL. Merging is a human decision.

### The chain

```
1. read the issue          gh issue view NNNN --repo eclipse-dirigible/dirigible
2. worktree off origin     git fetch origin && git worktree add ../wt-NNNN -b issue-NNNN-<slug> origin/master
3. isolate .m2 (if shared) see "Maven repository isolation"
4. implement + test        module unit suite green, the relevant ITs green (see "Verification bar")
5. format                  mvn formatter:format (changed modules), then validate with the cache wiped
6. commit                  plain `git commit` - identity from git config, never set by hand
7. push                    git push -u origin issue-NNNN-<slug>
8. PR                      gh pr create --fill-first --body "... Fixes #NNNN ..."
9. report                  the PR URL, what was verified, what was not
10. clean up               after the PR merges: git worktree remove, delete the .m2 clone
```

### Worktree, not branch switch

The primary checkout may be shared by several concurrent sessions (several Claude sessions, or a
colleague's IDE). Switching its branch, stashing in it, or `git add -A` in it silently destroys someone
else's uncommitted work - this has happened. So:

- **Always work in a dedicated worktree**: `git worktree add ../wt-NNNN -b issue-NNNN-<slug> origin/master`
  from the primary checkout. Branch off **`origin/master`** after a `git fetch`, never off the local
  `master`, which is routinely days stale.
- **Never `git stash`, `git checkout <branch>`, `git reset` or `git add -A` in the primary checkout.**
  If you already edited files there, create the worktree first, `cp` your changed files into it, then
  `git checkout -- <file>` them in the primary. Check `git status` in the primary before touching
  anything - if it is dirty, assume the dirt is someone else's.
- Verify the branch right before every mutating step (`git status -sb`, `git rev-parse --abbrev-ref HEAD`):
  `git checkout -b` has printed "Switched to a new branch" while HEAD stayed put, and a commit then
  landed on master.
- A second running Dirigible instance needs its own ports: `DIRIGIBLE_SERVER_PORT` alone is not enough,
  SFTP and FTP both default to 8022 (`DIRIGIBLE_SFTP_PORT`, `DIRIGIBLE_FTP_PORT`). Start long-running
  servers with `nohup ... & disown` in a normal shell call, kill by PID or `lsof -ti :<port>`, never
  `pkill -f dirigible` (it kills the other sessions' instances too).

### Maven repository isolation

Worktrees share `~/.m2`, so an `install` from one worktree replaces the SNAPSHOT jars another
worktree's integration tests are resolving - the classic symptoms are a `NoClassDefFoundError` for a
class that exists only on another branch, an IT failing at an assertion CI does not fail at, or a
generated file that "misses" your feature although `target/classes` has it. When more than one
worktree or session may build at the same time, give each worktree a private repository:

```
# macOS APFS (copy-on-write, seconds):   cp -Rc ~/.m2/repository ~/.m2/repo-wt-NNNN
# Linux (reflink where supported):         cp -r --reflink=auto ~/.m2/repository ~/.m2/repo-wt-NNNN
# anywhere (hard links also work):         cp -Rl ~/.m2/repository ~/.m2/repo-wt-NNNN
rm -rf ~/.m2/repo-wt-NNNN/org/eclipse/dirigible
```

then pass `-Dmaven.repo.local=$HOME/.m2/repo-wt-NNNN` to EVERY Maven command in that worktree (the
path is the `repository`-shaped directory itself, not its parent). Third-party artifacts are immutable
releases, so the clone stays valid; only the dirigible SNAPSHOTs are rebuilt. Alternative that installs
nothing: run the IT through the reactor, `mvn -pl tests/tests-integrations -am -P integration-tests
-Dit.test=<Name> ... verify`, which resolves sibling modules from the worktree's `target/`.

Never `mvn install` while an IT run is in progress in any worktree (`pgrep -fl maven` first).

### Building and testing in a worktree - the known traps

- **The full reactor fails at `dirigible-application`** with `Could not get HEAD Ref` from the
  git-commit-id plugin, which cannot follow a worktree's `.git` file. Environment noise, not your bug:
  add `-Dmaven.gitcommitid.skip=true` to every worktree build. A warm `mvn -T 1C -P quick-build install
  -DskipTests -Dmaven.gitcommitid.skip=true` of the whole reactor takes about a minute.
- **A full build dirties the tree**: `modules/parsers/typescript/**` are committed ANTLR-generated
  sources that the build regenerates unformatted (six files, tens of thousands of lines). They are never
  your change. `git status --short` before staging, stage intended paths explicitly, and
  `git checkout -- modules/parsers/typescript` if they show up. This has reached CI as a `code-style`
  failure on an unrelated PR more than once.
- **Stale H2 breaks IT runs after a killed JVM**: `rm -rf tests/tests-integrations/target/dirigible`
  before re-running locally.
- **Run one IT**: `mvn -o -P integration-tests -pl tests/tests-integrations verify -Dit.test=<Name>
  -Dselenide.headless=true` (after an install), adding `-Dit.groups= -Dit.excludedGroups=` when the
  class carries a `@Tag`. A local run may wedge in Spring context teardown AFTER the tests passed (a
  macOS file-watcher deadlock) and never write a report; read the assertion results out of the log
  before calling it a failure.
- `mvn test` in a worktree may need `-Dlicense.skip=true`; the javadoc release check is
  `mvn -P release -Dgpg.skip=true -DskipTests -Dlicense.skip=true -Dformatter.skip=true install -pl <modules> -am`.

### Verification bar (what "green" means before pushing)

1. `mvn formatter:format` on the changed modules, then **wipe the formatter cache and validate with
   CI's exact command**: `find . -name formatter-maven-cache.properties -path '*/target/*' -delete`,
   then `mvn -T 1C formatter:validate`. The cache can report success on files CI will reject.
2. The changed modules' unit suites green (`mvn -pl <module> -am test`).
3. The integration tests that cover the change green, by name - and for a template or generator change,
   an IT that COMPILES and RUNS the generated output (`IntentEmissionCoverageIT`, `IntentEngineIT`, a
   `*TemplateIT`), not only one that asserts rendered source text. A test that greps the template for
   the sentence you just wrote proves nothing.
4. Read exit codes correctly: `$?` after a pipe is the last command's, so grep the Maven output for
   `BUILD SUCCESS|BUILD FAILURE` or capture `mvn ...; MVN_EXIT=$?` directly.
5. For a template fix, grep the sibling templates and the other branch of the same Velocity template for
   the same code shape (`EntityController` vs `EntityMyController` vs `EntityPartnerController`;
   `Generate` vs `Job` vs `Posting`; the generate vs notify branch of `Job.java.template`; the power vs
   my/partner/admin views). A fix that reaches one of them is not done.
6. Say plainly in the PR what was verified and what was not. Never describe a check you did not run.

### Commit, push, PR

- **Identity comes from git config.** Commit with plain `git commit`. Never pass `-c user.email=...`,
  never set `GIT_AUTHOR_EMAIL`/`GIT_COMMITTER_EMAIL`, never take an address from a tool's session
  context, a profile or a signature. Every contributor's commit email must be the one they signed the
  Eclipse Contributor Agreement with; any other address fails the `eclipsefdn/eca` check on the PR and is
  repaired only by rewriting history on every affected branch. Do not bypass the repository's git hooks
  with `--no-verify` - if a hook refuses the commit, the identity is wrong, not the hook.
- Commit subject in the repo's style: `<area>: <what changed, as a sentence> (#NNNN)`.
- Before any push: `gh pr list --head <branch> --state all` (never push new work onto a branch whose PR
  is merged), `git status -sb`, `git log --oneline -3`. After the push, `git rev-parse HEAD origin/<branch>`
  must match.
- `gh pr create` against `master` with a body that explains the cause, the change, how it was verified,
  and ends with `Fixes #NNNN`. Report the PR URL.

### Cleanup

After the PR is merged (not before - CI may need a follow-up on the branch):

```
cd <primary checkout>
git worktree remove ../wt-NNNN            # add --force only if you are sure nothing there is unpushed
git branch -d issue-NNNN-<slug>
rm -rf ~/.m2/repo-wt-NNNN                 # the clones are the big consumers: orphaned ones filled a disk once
git fetch --prune
```

`git worktree list` and `ls ~/.m2 | grep repo-wt-` show what is left over; a worktree whose branch's PR is
merged can go.
