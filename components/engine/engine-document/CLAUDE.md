# engine-document

Print templates for generated applications: the runtime half of the document-template engine
(the authoring half is `modules/parsers/document` + `engine-intent`'s `PrintIntentGenerator`).
Merged via PR [#6119](https://github.com/eclipse-dirigible/dirigible/pull/6119).

## The contract (read this first)

CMS seed content lives under a project's **`doc/` folder**, laid out **exactly as it must appear in
the CMS**. On publish the generic **`CmsSeedSynchronizer`** mirrors everything under `<project>/doc/`
into the (tenant-scoped) CMS at the same relative path — so

```
<project>/doc/Templates/SalesInvoice/Print/en/standard.print
        → CMS  /Templates/SalesInvoice/Print/en/standard.print
```

The synchronizer is **generic and folder-scoped** — it is not print-specific and not
extension-specific: *any* file under `doc/` (print templates, images, documents) is seeded as opaque
CMS content. `engine-intent`'s `PrintIntentGenerator` produces the print template directly at its CMS
path under `doc/` (the `Templates/<Entity>/Print/en/standard.print` convention now lives in the
generator, not the synchronizer). Do **not** drop model artefacts (`.csvim`, `.bpmn`, …) under `doc/`
expecting their normal engines — under `doc/` they are opaque content.

**Print templates are the exception: they are versions of a catalogue, not plain seeds (#7755).**
`doc/Templates/<Entity>/Print/<lang>/<name>.print` is seeded by `PrintTemplateSynchronizer`
(`SynchronizersOrder.PRINT_TEMPLATE = 525`, multitenant), and `CmsSeedSynchronizer.isAccepted` skips
exactly that shape (`PrintTemplateSynchronizer.isShippedTemplate`), so the two never both seed one
file. See "The print template catalogue" below.

Three rules that must never regress (for the generic seed - the catalogue has its own, below):

1. **Create-if-absent, never overwrite.** The CMS copy is the business user's customization
   surface (download/edit/upload through the Documents perspective). A re-publish or regeneration
   must not clobber it — the seeded file is only the never-customized default.
2. **DELETE never touches the CMS.** Removing the seed file (or the project) removes the `CmsSeed`
   DB row only; uploaded customizations survive.
3. **Per-tenant.** `CmsSeedSynchronizer` is a `MultitenantBaseSynchronizer` because the internal CMS
   root is tenant-scoped (`<CMS root>/<tenantId>/cms`) — each tenant's sync seeds its own copy.
   `SynchronizersOrder.CMS_SEED = 520`.

Multilanguage is folder-based: additional languages are simply more
`doc/Templates/<Entity>/Print/<lang>/` files (only `en` is generated; the others are authored or
uploaded). The Print button asks which to use when several exist.

## The print template catalogue (`PrintTemplateCatalog`)

All print-template knowledge lives in the print engine; **`CmsStore` and the generic CMS seed know
nothing about templates, versions or the active one** - no marker files, no CMIS properties. (The
Documents perspective knows one thing, through the generic `DocumentWriteGuard` SPI of engine-cms:
`ShippedPrintTemplateGuard` refuses an upload over, a rename of or a delete of a shipped version -
and of a folder holding one - 409 with the reason. `DocumentsService` hands the guards the canonical
path - no `//`, no `.`, never `..`, and a name is one segment - because the backends collapse those
themselves, so a guard matching the raw request path could be bypassed by another spelling of it.) A language folder holds two kinds, told apart by the name alone
(`PrintTemplateName`):

```
Templates/SalesInvoice/Print/en/
  standard@1.28.0.print     shipped version - immutable, written only by the synchronizer
  standard@1.30.0.print     shipped version
  acme-blue.print           tenant template - editable, records <!-- derived-from: standard@1.28.0 -->
```

A document whose name is not a valid template name (`Invoice template.print`, `фактура.print`, the
`standard.print-<millis>` an overwrite leaves behind on S3, whose rename does nothing) is **never
dropped**: it is a tenant template under its sanitised name (`Invoice-template`, `template-<hash>`,
`standard`) - the resolution before versions printed such files.

- **Where**: a file is a shipped template when the tail of its path is
  `doc/Templates/<Entity>/Print/<lang>/<file>.print` (a `/doc/` higher up, in the repository root
  or a project name, does not matter), and its project - whose `project.json` gives the version - is
  the first segment of the registry-relative location, however deep the `doc/` folder sits.
- **Version** = the project's `project.json` `version` when valid, else its `package.json` `version`
  (the BusinessIntents `base-*` modules declare it only there), else the first 8 hex digits of the
  content SHA-256 - and then **`PrintTemplateReleases`** makes it immutable against the ledger
  `DIRIGIBLE_PRINT_TEMPLATE_VERSIONS` (every version this instance ever shipped, append-only, with
  the content hash): bytes shipped before get their earlier version back (a rollback or an unchanged
  release adds nothing), and changed bytes under a label already recorded ship as
  its next revision, `<label>_v1`, `<label>_v2`, ... (a WARN; a revision ranks above the version it
  revises and below the next release) instead of overwriting what pinned tenants print. Per tenant the seed adds
  `<name>@<version>.print` when missing, **converges** an existing one whose bytes drifted, adds
  nothing when the newest shipped version already has the same bytes, and never touches a tenant
  template.
- **Order** of shipped versions = the ledger's id, i.e. when the instance first shipped them - the
  only order a content hash has (no base module declares a release version, and the AOT packager
  drops `project.json`), and the release order for release versions published in order. A version
  the ledger never recorded ranks below every recorded one (release versions compare numerically,
  pre-releases identifier by identifier), and `@legacy` below everything.
- **Default** (no selection): of the **primary** template - `standard`, else the first name the
  registry ships - the version the registry ships **now** (`PrintTemplateSeed` rows), else that
  name's newest version present, else the first tenant template. Scoped by name, so a release that
  adds a `compact.print` does not switch every tenant to it, and redeploying an older release
  makes its version the default again.
- **Migration** (`migrate`), before the first version of a template lands in a folder. Everything is
  a copy + read-back + delete (`CmsStore.move`) - **never the CMS rename**, which S3 does not
  implement (silently a no-op) and SharePoint refuses:
  - While the folder has no shipped version at all, the rule before versions still decides what
    printed: the first `.print` document in CMS order. Without a stored selection, a document other
    than the legacy copy that printed (`acme.print`) is selected - moved to its sanitised name first
    when it had none.
  - The legacy `<name>.print` whose bytes the ledger knows becomes that version: an unedited copy,
    however stale, is not a customisation, and the tenant gets the current release. Unselected for
    the primary template; for another one that printed, the version is selected - otherwise the
    outcome would depend on which template happened to seed first.
  - Any other legacy copy becomes `<name>-custom` with `<!-- derived-from: <name>@legacy -->` (BOM
    dropped), so every shipped version is flagged newer, and is selected when it is what printed.
  The ledger only knows releases this instance shipped through the catalogue - plus the bytes the
  generic seed shipped last, recorded when the synchronizer retires that seed's row (below) unless
  they equal the current bytes, which then keep the module's own version. A copy of
  an older release is indistinguishable from a customisation and is kept as one, flagged.
- **Taking over from the generic seed.** On an upgraded instance every `.print` had a `cms-seed`
  definition and `DIRIGIBLE_CMS_SEEDS` row. `PrintTemplateSynchronizer.parseImpl` deletes the rows at
  its location (recording their content in the ledger first), and `SynchronizationProcessor.markDeleted`
  matches definitions by **key** (type + location), so the `cms-seed` definition left at a location the
  print synchronizer now owns goes DELETED - by location it stayed PARSED forever, and after a tenant
  activation blanked its checksum the initialization status reported IN_PROGRESS for every tenant.
- **One failing tenant keeps the artefact FAILED.** The row is shared and the tenants are completed
  one after another, each `registerState` overwriting the last; the synchronizer keeps the set of
  tenants whose seed failed per artefact key and registers FAILED until it is empty, so a later
  tenant's success cannot hide an earlier failure from the START retry.
- **Selection** is the tenant configuration `DIRIGIBLE_PRINT_TEMPLATE_<ENTITY>_<LANG>`
  (`PrintTemplateSelection`; the key policy admits the family by prefix - see `tenants.md` for why
  this one prefix exists - and `/predefined` does not list it, or the shells' Save would write a stale
  selection back). It is read like every tenant override, from the thread-scoped `Configuration` the
  request filter / listener dispatch fill; `PrintFacade` loads it for a render outside both (a
  snapshot on the BPM executor). The migration reads the stored value past the per-node
  `TenantConfigurationCache` (`getStored` → `TenantConfigurationService.readStoredForCurrentTenant`),
  and the delete-the-active guard reads every layer resolution reads, fresh (`getEffective`: runtime,
  stored, environment/deployment/module) - the cache has no TTL and another node's write does not
  invalidate it. Keep
  the GET handlers off the store: reaching its lazy `CREATE TABLE` from a GET is what CodeQL reports
  as an unprotected state-changing request. **Resolution**: `template` request parameter →
  configuration (a missing name logs WARN and falls through) → the default above.
- **A print costs no statement in the steady state**: `PrintTemplateReleases` caches the seed rows'
  name/version projection and the ledger per entity and language for 60 s (invalidated by the
  synchronizer; the expiry covers other nodes). `PerformanceBaselineIT`'s print step counts statements
  and gates on `tests/PERF_BASELINE.json` - a per-print query here is a regression there.
- The artefact table is `DIRIGIBLE_PRINT_TEMPLATE_SEEDS` (content inline binary, as for `CmsSeed`).
  Deleting the shipped file removes the row only; its versions stay in every tenant's catalogue and
  in the ledger.

## Endpoints

- `GET /services/print/{entity}/languages` → `[{"code":"en","name":"English","selectionKey":"DIRIGIBLE_PRINT_TEMPLATE_<ENTITY>_EN"}, ...]` — the child
  folders of `Templates/{entity}/Print`, display names via `Locale.forLanguageTag(code)
  .getDisplayLanguage(Locale.ENGLISH)` (code fallback). Empty array when the folder is missing.
- `POST /services/print/{entity}?lang=en&template=` with `{"document": {...}, "items": [...]}` →
  `application/pdf` (inline). Resolves the template through the catalogue (404 with a clear message
  when absent), then `DocumentParser → DataBinder → XslFoRenderer → PDFFacade.generate(fo, "<data/>")`.
- `GET /services/print/document-types` (the entity folders, one listing - languages are read per
  type); `GET /{entity}/templates?lang=&details=` (the catalogue: name, kind, version, derivedFrom,
  active, newer - `details=true` reads the tenant templates for `derivedFrom`, the Settings page asks
  for it, the print dialog does not); `GET|PUT|DELETE /{entity}/templates/{name}?lang=` (raw content /
  write a tenant template - 409 for a shipped version, 400 when it does not parse / delete - 409 for
  a shipped or the active one); `POST /{entity}/templates/{name}/duplicate?lang=&as=` (the copy is
  parse-checked). Reading a source and every write are ADMINISTRATOR/OPERATOR, **and the CMS access
  grants of the template's path apply on top** (`DocumentAccessEvaluator`, 403), as in the Documents
  perspective. Printing (`POST /{entity}?template=`) and the catalogue listing read templates as the
  engine, for every user: the print dialog offers the tenant's templates to everyone who may print,
  so a READ grant on the folder restricts the source, not the rendered output. The active selection
  is written through `PUT /services/core/configurations/tenant`, not here, and cleared with its
  `DELETE ?key=` ("Use default" on the page) - once a selection is stored, newer releases are only
  flagged `newer`, never applied, until it is cleared. **Known limit:** the selection is read through
  the tenant configuration cache, which is per node and invalidated by the writing node only (see
  `.claude/docs/tenants.md`); on a multi-node instance another node prints the old layout until its
  cache reloads. Only the delete guard reads past the cache. The IDE page is `components/ui/settings-print-templates` (Settings → Print Templates); the tenant-facing one is the shared shell fragment `application-core/shell/views/_print-templates.html` on `stores/printTemplates.js`, mounted by the platform shells and every generated application shell.
  The generated pages share one Print implementation, `application-core/shell/js/components/printActions.js`:
  the dialog opens for more than one language or more than one tenant template, and lists the
  tenant templates, the active one and the shipped default (older shipped versions are pinned in
  Settings, not offered per print; the shipped default alone never opens the dialog, so one custom
  layout still prints directly). A print names its template only when the user picked another than
  the active one, so the server resolves the selection itself. Print waits while a language's
  layouts load and drops a stale response.

**The client feeds this endpoint from a server-side feeder, not from its own screen state.** The
Harmonia document/manage page first GETs the generated `…PrintFeeder/{id}` (client-Java), which
loads the record + its related graph through the generated repositories and returns the nested
`{ document, items }` payload — so `{{document.<Relation>.<Field>}}` resolves and validations, events
and the **multilingual translation overlay** all apply. The page then POSTs that payload here. The
feeder is called as the logged-in user (auth + tenant are the caller's). **Language:** the print
dialog's chosen language drives BOTH halves — it is the `?lang=` that selects the CMS template folder
(labels) AND is sent as the feeder GET's `Accept-Language` (via `api.get(url, { language: lang })`),
because the repositories' multilingual overlay reads `User.getLanguage()`/`Accept-Language`; without
that pin the nomenclature VALUES would translate to the UI locale while the template is in the print
language (dirigible #6945). The JSON body is parsed with a **plain Gson**
(`ToNumberPolicy.LONG_OR_DOUBLE`) — never `JsonHelper`/`GsonHelper` (the `@Expose` trap; and
LONG_OR_DOUBLE keeps integers integral while decimals arrive as `Double`, which `DataBinder`
formats in the form money pattern `### ### ### ##0.00`).

## Structure notes

- `CmsSeedSynchronizer`, `PrintEndpoint`, `CmsStore` and `PrintRenderer` share the root package
  `org.eclipse.dirigible.components.engine.document` so `CmsStore` can stay package-private
  while serving both consumers (Java packages don't nest). `domain`/`repository`/`service` follow
  the engine-openapi sub-package shape.
- **`CmsSeedSynchronizer` matches by folder, not extension** — it overrides `isAccepted(Path, attrs)`
  to accept any regular file whose path contains a `/doc/` segment (and `getFileExtension()` returns
  `""`, unused since the override replaces the default extension match). The CMS path is the
  location from `/doc/` down (`toCmsPath`).
- `CmsStore` is the **only** CMS surface, and a plain file store: **seed** — `seed(cmsPath, bytes)`
  (generic, create-if-absent, `ensureFolder` walks/creates one level at a time since the engine
  `CmisFolder.createFolder` is single-level; a media type is inferred from the file extension); and
  generic `listFolders` / `listDocuments` / `read` / `write` / `move` / `delete` that
  `PrintTemplateCatalog` builds on. `write` replaces as delete + create and writes the previous bytes
  back when the create fails; `move` is copy + read-back + delete (rolled back on failure) - there is
  no `rename`, because no CMS backend but the internal one implements it. A missing path is the
  `IOException` `getObjectByPath` throws (logged at DEBUG with the throwable). Writes go through the
  raw engine-cms interfaces (`CmisSessionFactory.getSession()`), which bypass CMS role checks —
  correct for the seeder; the catalogue's user-facing operations check `DocumentAccessEvaluator`
  themselves.
- `CmsSeed` stores the raw content in a `CMS_SEED_CONTENT` binary column (bytes, so binary seeds
  work) plus the target `CMS_SEED_PATH`, so the seeding phase does not re-read the repository.
  **The column is `@JdbcTypeCode(SqlTypes.LONG32VARBINARY)`, never `@Lob`.** A `@Lob byte[]` is an
  `oid` large object on PostgreSQL, and pgjdbc refuses the large-object API on an auto-commit
  connection — which is exactly the connection `parseImpl` reads and saves the seed on — so every
  seed save on a PostgreSQL SystemDB failed with "Large Objects may not be used in auto-commit
  mode", silently: the failure is in `parseImpl`, before the artefact has a lifecycle, so nothing
  ever shows up as an artefact in error (#7059). The mapping renders `bytea` on PostgreSQL and
  leaves H2 (`blob`) and MSSQL (`varbinary(max)`) exactly as they were;
  `CmsSeedContentMappingTest` pins all three, and the changelog's
  `convert-DIRIGIBLE_CMS_SEEDS_CMS_SEED_CONTENT-to-bytea` converts an already-created `oid` column.

## Images in a template (`PrintImageResolver`)

`<image src="...">` is resolved **here**, not in the parser library and not in the browser:
`PrintImageResolver` (a `@Component`, handed to `XslFoRenderer` by `PrintRenderer`) reads the source
and inlines the bytes as a `data:` URI, so all three render paths - the Harmonia Print button
(`POST /services/print/{entity}`), the `attach: print` mail and the `function: Snapshot` PDF - carry
the same image from the same template. The shape of the source says what it is:

| source | resolution |
| --- | --- |
| `data:...` or any other `scheme:` | emitted unchanged (the data carried the image inline, or FOP addresses it itself) |
| anything else | a path in the tenant CMS - read, size-checked, base64-inlined |

Points worth keeping:

- **Inlining is not an optimization.** The renderer's output is a self-contained stylesheet handed
  to FOP with no session, no credentials and no tenant scope; a source left as a CMS reference could
  only be fetched by opening that content to an unauthenticated read. Resolution happens while the
  caller's own scope still applies. (FOP reads `data:` URIs natively - `InternalResourceResolver` -
  so nothing had to be configured for this; `PDFFacadeTest.generatePdfWithInlineImageTest` pins it.)
- **Every failure is soft.** A missing file, an oversized one, a document whose media type is not a
  plain `image/<subtype>` (matched in full, not by prefix - an attachment's content type is whatever
  the uploading browser claimed, and it lands inside the data URI), a path carrying a `..` segment and
  an unreadable store all resolve to `null`, and the
  renderer then omits the image entirely. A logo that cannot be read must not cost the invoice - and
  a missing logo is the everyday state of a tenant that has not uploaded one yet.
- **The ceiling is `DIRIGIBLE_PRINT_IMAGE_MAX_SIZE`** (2 MB). The bound is checked against the
  declared length first (so an oversized document is never streamed) and again while reading, because
  a CMS backend may report no length or a stale one.
- The generated scaffolds (`PrintIntentGenerator`, `ReportPrintTemplate`) emit one shared logo slot,
  `Templates/Print/logo.png` - **one path for the whole application**, since a company has a logo and
  not an invoice-logo, so branding a deployment is a single upload (Documents perspective) or a single
  `doc/Templates/Print/logo.png` shipped with the project. It is emitted unconditionally exactly
  because a missing image renders nothing: a deployment that never uploads one prints as before, and
  one that does needs no regeneration of a template it may already have customized.
- **A file of the record** works through the same `src`: a `function: Attachment` row's
  `StoragePath` IS a CMS path, so `<image src="{{document.<Relation>.StoragePath}}"/>` renders it
  wherever the print feeder carries that relation (same-model to-one graph, depth 2). No modeling
  construct was added for this - a relation to the attachment row is an ordinary to-one.

## Renderer v1 limits (documented in `XslFoRenderer`, deliberate)

Header/footer render once in-flow (not repeated `fo:static-content` regions); `repeatHeader` and
`pageBreak` are ignored; 1 px = 1 pt; a table with no data rows is skipped entirely (FOP rejects
an empty `fo:table-body`). Lift these in the renderer, not here.

## Registration

`components/pom.xml` (`<module>`), `modules/pom.xml` dependencyManagement (components-tier
artifacts are managed there — the `dirigible-modules-parent` BOM that `components/` imports),
`components/group/group-engines/pom.xml`.
