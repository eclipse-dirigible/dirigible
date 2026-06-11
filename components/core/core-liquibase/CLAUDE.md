# CLAUDE.md — components/core/core-liquibase

Source of truth for the **SystemDB schema** (the `DIRIGIBLE_*` platform tables JPA-scanned out of `org.eclipse.dirigible.components` + `org.eclipse.dirigible.engine`). Every CREATE TABLE / ALTER TABLE / index / FK that ends up in SystemDB is declared as a Liquibase JSON changeset in `src/main/resources/db/changelog/dirigible-system.json`.

## Why this module exists

Before this module existed, the platform schema was bootstrapped by Hibernate's `hbm2ddl=update` on the SystemDB EntityManagerFactory. That had two structural problems:

- **The SYS-lock race.** When several Spring contexts in one JVM (e.g. consecutive `@SpringBootTest`s in the integration suite) each started Hibernate against the same file-backed H2, their CREATE TABLE batches collided over H2's `SYS` catalog lock and one or more contexts timed out. Once that started cascading, every IT after the first crashed in 0.001s with `Failed to load ApplicationContext`. See [run 27256937543](https://github.com/eclipse-dirigible/dirigible/actions/runs/27256937543) — `DependsOnScenariosIT` racked up 468 `Timeout trying to lock table "SYS"` errors before giving up.
- **No versioning.** Hibernate's `update` is purely a forward-merge of the current entity model into whatever is in the DB. There's no record of *when* a column appeared, no review path for schema changes, no rollback, and no way to make Postgres/MSSQL emit different DDL than H2 in the same release.

`core-liquibase` replaces that bootstrap. Liquibase applies the changelog under its own `DATABASECHANGELOGLOCK`, so even with N concurrent Spring contexts only one ever issues DDL. Hibernate's `update` pass then sees a fully-populated schema and emits zero CREATE statements — the race window is gone.

## Architecture

```
LiquibaseSystemConfig (@Configuration)
├── liquibaseSystemDB             (SpringLiquibase bean, applies the changelog at boot)
│   └── LegacyAwareSpringLiquibase (extends SpringLiquibase)
│         performUpdate() → if sentinel table (DIRIGIBLE_SECURITY_ACCESS) is present AND
│                           DATABASECHANGELOG is missing OR empty, call changeLogSync()
│                           to mark every changeset applied without re-running its DDL,
│                           then fall through to super.performUpdate()
└── LiquibaseEntityManagerFactoryDependsOnPostProcessor (BFPP)
      extends Spring Boot's EntityManagerFactoryDependsOnPostProcessor("liquibaseSystemDB")
      → dynamically adds dependsOn to every EntityManagerFactory bean at startup,
        but ONLY when core-liquibase is on the classpath
```

**Why a BFPP and not `@DependsOn` on the EMF bean.** A literal `@DependsOn("liquibaseSystemDB")` in `core-database/DataSourceSystemConfig` makes Liquibase a *required* dependency of the EMF. Every JPA-using module's unit tests (engine-javascript, data-sources, ide-problems, ...) boot a slim Spring context that doesn't pull `core-liquibase`. With a hard `@DependsOn`, the missing bean fails the entire context. Spring Boot's `LiquibaseAutoConfiguration` already solves this exact problem with `EntityManagerFactoryDependsOnPostProcessor` — we reuse it. Lives in this module → only registered when this module is loaded → slim test contexts boot clean.

**Hibernate's `hbm2ddl=update` stays the default** (`DIRIGIBLE_DATABASE_SYSTEM_DDL_AUTO=update` in `DataSourceSystemConfig`). The BFPP forces Liquibase to run first, so in the full app Hibernate's `update` finds every table already created and emits no DDL. In unit-test contexts without Liquibase, `update` still works as the bootstrap. Switching the default to `validate`/`none` is reserved for a future PR once every JPA test in the repo grows a Liquibase-aware setup — flipping it now blows up every `@SpringBootTest` repo test that boots its own context (see commit `7244f92f` for the regression that justified reverting that part).

## ⚠️ Adding a new system artefact / table / column — MANDATORY

**Every new JPA entity (or `@Column` on an existing one) that goes into SystemDB MUST be accompanied by a new changeset appended to `db/changelog/dirigible-system.json`.** Hibernate's `hbm2ddl=update` will silently create the new column on a fresh local H2 in development, hiding the omission — but **production / Postgres / MSSQL deployments may run with Liquibase strictly authoritative** (`hbm2ddl=validate`), in which case the missing changeset is a deployment outage.

The checklist for any PR that touches a `@Entity` class under `components/*` or `modules/*`:

- [ ] **Added or modified a `@Entity` class** under one of the JPA scan packages? You owe a changeset.
- [ ] **Added a `@Column`** (even with a default Hibernate type)? You owe a changeset (`addColumn`).
- [ ] **Changed an existing column's name, length, nullability, type, or default**? You owe a changeset (`renameColumn` / `modifyDataType` / `dropNotNullConstraint` / `addNotNullConstraint` / `addDefaultValue` / `dropDefaultValue`).
- [ ] **Added a `@JoinColumn` / FK / `@OneToMany`** between system entities? `addForeignKeyConstraint` changeset.
- [ ] **Added a unique constraint or index**? `addUniqueConstraint` / `createIndex` changeset.
- [ ] **Deleted an entity or column**? `dropTable` / `dropColumn` changeset (irreversible in production data — coordinate with whoever owns the upgrade story).
- [ ] **The changeset id is descriptive and unique** (`add-DIRIGIBLE_NEW_TABLE_NEW_COLUMN`, `rename-DIRIGIBLE_FOO_BAR`, ...) — not `1781080528852-1`-style timestamps.
- [ ] **The changeset author is `dirigible`** (we don't track individual authorship in the changelog).
- [ ] **Types are vendor-portable** — see "Portability gotchas" below.
- [ ] **The changeset is APPENDED, never inserted in the middle.** Existing changesets ship to customers' DBs with a content checksum; reordering or editing an applied changeset trips Liquibase's checksum validator on next startup. New work always goes at the end of the file.

A typical follow-up changeset looks like this — it's tiny:

```json
{
  "changeSet": {
    "id": "add-DIRIGIBLE_BPMN_BPMN_PRIORITY",
    "author": "dirigible",
    "changes": [
      {
        "addColumn": {
          "tableName": "DIRIGIBLE_BPMN",
          "columns": [
            { "column": { "name": "BPMN_PRIORITY", "type": "INT" } }
          ]
        }
      }
    ]
  }
}
```

If you're not sure what DDL Hibernate would emit, the fastest way to find out is to boot the app once locally with hbm2ddl=update, then run `H2 SCRIPT` on `target/dirigible/h2/SystemDB.mv.db` and transcribe the `CREATE TABLE` / `ALTER TABLE` it produced. The very first changelog in this repo was generated exactly that way.

## Portability gotchas (H2 + Postgres + MSSQL)

Liquibase translates `type` strings per-vendor, but only for a fixed alias set. Use these conventions:

- **`VARCHAR(n)`** — portable on all three. Use for bounded text. Always bounded when a column participates in any index/UK/FK/PK — H2 cannot index a CLOB, so `VARCHAR(2000)` is the right ceiling for artefact keys/locations/names/paths.
- **`CLOB`** — for unbounded content (`*_CONTENT`, `*_DESCRIPTION` when truly large). Liquibase emits `CLOB` on H2, `TEXT` on Postgres, `NVARCHAR(MAX)` on MSSQL. **Do not put CLOB columns in indexes / UKs / FKs**, H2 will reject the index DDL.
- **`BIGINT`, `INT`, `BOOLEAN`, `TIMESTAMP`, `CHAR(n)`** — portable, no surprises.
- **`ENUM(...)`** — DO NOT USE. H2-only; Postgres needs `CREATE TYPE`, MSSQL has no equivalent. Persist enums as `VARCHAR(50)` (and optionally enforce values via a CHECK constraint in a separate changeset).
- **Auto-increment**: `"autoIncrement": true` + `"type": "BIGINT"` translates to H2 `IDENTITY`, Postgres `GENERATED BY DEFAULT AS IDENTITY`, MSSQL `IDENTITY(1,1)`. Always set this on the primary key column rather than emitting raw DDL.
- **Constraint names matter** — they end up in error messages and migration logs. Use `PK_<table>`, `UK_<table>_<col>`, `FK_<base>__<col>`, `IX_<table>_<col>`. The first baseline was post-processed to replace H2's `CONSTRAINT_18`/`CONSTRAINT_CB` autonames; don't reintroduce them.

When you change anything non-trivial, the CI matrix is your safety net: integration-tests run against H2, Postgres, and MSSQL with the same changelog (see `.github/workflows/build.yml`). A change that lands green on H2 locally still has to survive the three-DB matrix on the PR — keep the changeset minimal so any vendor-specific failure is easy to pin.

## Legacy-deployment path (`LegacyAwareSpringLiquibase`)

`LegacyAwareSpringLiquibase` handles two non-pristine startup states that would otherwise make Liquibase throw "Table already exists" trying to recreate tables still on disk:

1. **Legacy production upgrade.** Existing deployments built up via the old `hbm2ddl=update` have every `DIRIGIBLE_*` table but no `DATABASECHANGELOG` ledger. The handler detects sentinel `DIRIGIBLE_SECURITY_ACCESS` exists + `DATABASECHANGELOG` is absent, calls `Liquibase.changeLogSync(...)` so every changeset is recorded as applied without re-running its DDL, and the next startup is indistinguishable from a fresh install.
2. **Integration-test partial-cleanup state.** Between IT classes the test framework's `DirigibleCleaner` runs H2 `DROP ALL OBJECTS` against the SystemDB, which drops every schema object including `DATABASECHANGELOG`. If a different connection holding stale entries races the drop, or a previous test left orphan tables on disk, the next test boots a fresh Liquibase against a DB where the ledger has been re-created (empty) while some artefact tables are still present. The same `changeLogSync` recovery applies — the trigger is sentinel exists + `DATABASECHANGELOG` exists-but-empty. Hibernate's downstream `hbm2ddl=update` pass fills in any genuinely-missing tables in the rare case the orphans are incomplete. (This case nearly emptied a `db-sys-liquibase` PR re-run before the empty-ledger arm was added — every test after `LocalNativeAppLifecycleIT` cascade-failed on `create-DIRIGIBLE_BPMN`.)

If you change the sentinel-detection logic (e.g. add a new precondition, change the table name), audit existing deployments — there is no second chance to recover from a misdetection that re-runs a CREATE TABLE on a populated DB.

## Regenerating the baseline

You shouldn't need to regenerate the existing 131 changesets — appending new ones is the workflow. If you ever do (e.g. for a schema rewrite), the process is:

1. Boot the app fresh against an empty H2 with the current `@Entity` set and `hbm2ddl=update` (no `core-liquibase` on the classpath, or temporarily skip the BFPP).
2. Run `liquibase generate-changelog --url=jdbc:h2:file:./target/dirigible/h2/SystemDB --changelog-file=baseline.json --include-objects='table:DIRIGIBLE_.*' --default-schema-name=PUBLIC`.
3. Post-process to: rename `CONSTRAINT_xx` → `PK_/UK_/FK_/IX_<table>_<col>`, replace `VARCHAR(1000000000)` → `VARCHAR(2000)` for indexed columns or `CLOB` for content, replace `ENUM(...)` → `VARCHAR(50)`, give each changeset a descriptive id and `dirigible` author.
4. The transformer script that produced the current baseline is documented in PR #6003's commit history — start from that rather than reinventing.

## Wiring summary (where this module touches the rest of the build)

- `components/pom.xml` lists `core/core-liquibase` as a module and pins its version in `<dependencyManagement>`.
- `components/group/group-core/pom.xml` depends on `core-liquibase` so the full app picks it up (the BFPP only does its work when the bean is on the classpath, so this is the gate).
- No source-level dependency on `core-database` — only the bean name `SystemDB` is referenced via `@Qualifier`. The two modules are independent; the wiring happens at Spring boot time via bean name resolution.
- User-data stores (`components/data/data-store` for TS `@Entity`, `components/data/data-store-java` for Java `@Entity`) are out of scope. Those are dynamic, user-authored entities — they correctly keep `hbm2ddl=update` because their schema comes from user code at runtime, not from versioned changelogs.
