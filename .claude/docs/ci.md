## CI reference

`.github/workflows/build.yml` is the source of truth for "does this build pass" on **push to master**:

- `code-style`: `mvn -T 1C formatter:validate`
- `tests` (ubuntu + windows matrix): `mvn clean install -P unit-tests`
- `integration-tests-h2` / `-postgresql`: the **full** Selenide IT suite, `mvn clean install -P integration-tests` with the matching `DIRIGIBLE_DATASOURCE_DEFAULT_*` env vars (MSSQL is no longer a CI leg — removed in #6150). Each DB leg is **sharded into four parallel matrix jobs** selected by tag expression — `api` (`!ui & !slow`), `ui` (`ui & !slow & !sample & !camel`), `samples` (`ui & !slow & (sample | camel)`), `slow` (`slow`) — so the run's wall clock is the slowest shard (~35 min, ~40 on PostgreSQL), not the whole ~1h40m suite. The shards partition the suite; keep them disjoint and complete when adding tags.
- `build-deploy`: `mvn clean install -P quick-build` then Docker buildx multi-arch image push to `dirigiblelabs/dirigible`

### PR gate vs full suite (smoke / nightly split)

The full Selenide UI suite takes ~1.5h per DB, so it does **not** run on every PR:

- **`pull-request.yml`** (every PR) runs `code-style`, unit `tests`, `docker-build`, and a single fast **`smoke-tests`** job on H2: `mvn clean install -P integration-tests -Dit.groups="!ui | smoke"`. The tag expression selects the HTTP-level ITs (untagged, so `!ui`) plus the few UI journeys explicitly marked `@Tag("smoke")` - including one full clone->generate->validate app lifecycle (`IntentEditorLoadsIT`, the intent Generate flow). Keep the smoke set small so the PR gate stays fast.
- **`nightly.yml`** (cron `0 2 * * *` + `workflow_dispatch`) and **push to master** (`build.yml`) run the **full** suite on H2 + PostgreSQL.

**Test tagging convention (JUnit 5 `@Tag`, wired to failsafe via the `${it.groups}` / `${it.excludedGroups}` properties in the root `pom.xml`):**
- Every browser-driven IT is `@Tag("ui")` - inherited from the `UserInterfaceIntegrationTest` base (and thus by `SampleProjectsIT` and all sample-project ITs). Do not tag these individually.
- HTTP-level ITs (`extends IntegrationTest` directly) carry no tag, so they are always in the smoke set.
- To force a specific UI IT to run on every PR, add `@Tag("smoke")` to that class (keep the list small - smoke must stay fast).
- Shard-routing tags: `@Tag("sample")` sits on the `SampleProjectsIT` base (inherited by every sample-project IT); `@Tag("camel")` sits on each IT in `ui/tests/camel` (their `PredefinedProjectIT` base is shared with non-camel tests, so the base cannot carry it — tag new camel ITs individually). These route classes into the `samples` CI shard; everything else UI stays in the `ui` shard.
- `@Tag("slow")` is the **fourth shard**, and it is a *balancing* tag, not a semantic one: it holds the long poles of both families (currently the api classes above ~55 s and the browser journeys above ~110 s), because without them `api` and `ui` are the critical path while `samples` idles. Membership is a judgement about measured CI time — re-check it when the shard times drift apart, and note that mistagging can only unbalance the shards, never drop an IT (`api` is the untagged complement). It does **not** affect the PR smoke gate: a `slow` api IT is still untagged-`ui`, so `!ui | smoke` still selects it.

`codeql.yml`, `release.yml` cover CodeQL and Maven Central release respectively.
