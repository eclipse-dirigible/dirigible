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

Three rules that must never regress:

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

## Endpoints

- `GET /services/print/{entity}/languages` → `[{"code":"en","name":"English"}, ...]` — the child
  folders of `Templates/{entity}/Print`, display names via `Locale.forLanguageTag(code)
  .getDisplayLanguage(Locale.ENGLISH)` (code fallback). Empty array when the folder is missing.
- `POST /services/print/{entity}?lang=en` with `{"document": {...}, "items": [...]}` →
  `application/pdf` (inline). Resolves the first `*.print` under `Templates/{entity}/Print/{lang}/`
  (404 with a clear message when absent), then `DocumentParser → DataBinder → XslFoRenderer →
  PDFFacade.generate(fo, "<data/>")`.

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
- `CmsStore` is the **only** CMS surface, with two sides: **seed** — `seed(cmsPath, bytes)` (generic,
  create-if-absent, `ensureFolder` walks/creates one level at a time since the engine
  `CmisFolder.createFolder` is single-level; a media type is inferred from the file extension); and
  **print reads** — `listLanguages` / `findTemplate` (used by `PrintEndpoint`). A missing path is the
  `IOException` `getObjectByPath` throws (logged at DEBUG with the throwable). Writes go through the
  raw engine-cms interfaces (`CmisSessionFactory.getSession()`), which bypass CMS role checks —
  correct for a server-side seeder, same as `data-processes`' `BaseExportTask`.
- `CmsSeed` stores the raw content in a `CMS_SEED_CONTENT` BLOB (bytes, so binary seeds work) plus
  the target `CMS_SEED_PATH`, so the seeding phase does
  not re-read the repository.

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
