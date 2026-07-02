# engine-document

Print templates for generated applications: the runtime half of the document-template engine
(the authoring half is `modules/parsers/document` + `engine-intent`'s `PrintIntentGenerator`).
Merged via PR [#6119](https://github.com/eclipse-dirigible/dirigible/pull/6119).

## The contract (read this first)

A `.print` file at a project root (the DSL of `dirigible-parsers-document`; normally generated
by the Intent Editor's Generate, one per document entity) is a **runtime artefact**: on publish,
`PrintTemplateSynchronizer` seeds it into the CMS at

```
Templates/<EntityName>/Print/en/standard.print
```

where `<EntityName>` is the `.print` file's base name (`SalesInvoice.print` → `SalesInvoice`).
Three rules that must never regress:

1. **Create-if-absent, never overwrite.** The CMS copy is the business user's customization
   surface (download/edit/upload through the Documents perspective). A re-publish or regeneration
   must not clobber it — the generated file is only the never-customized default.
2. **DELETE never touches the CMS.** Removing the `.print` artefact (or the project) removes the
   DB row only; uploaded customizations survive.
3. **Per-tenant.** The synchronizer is a `MultitenantBaseSynchronizer` because the internal CMS
   root is tenant-scoped (`<CMS root>/<tenantId>/cms`) — each tenant's sync seeds its own copy.
   `SynchronizersOrder.PRINT = 520`.

Multilanguage is folder-based: additional languages are simply more `Print/<lang>/` folders,
uploaded by the user (only `en` is seeded). The Print button asks which to use when several exist.

## Endpoints

- `GET /services/print/{entity}/languages` → `[{"code":"en","name":"English"}, ...]` — the child
  folders of `Templates/{entity}/Print`, display names via `Locale.forLanguageTag(code)
  .getDisplayLanguage(Locale.ENGLISH)` (code fallback). Empty array when the folder is missing.
- `POST /services/print/{entity}?lang=en` with `{"document": {...}, "items": [...]}` →
  `application/pdf` (inline). Resolves the first `*.print` under `Templates/{entity}/Print/{lang}/`
  (404 with a clear message when absent), then `DocumentParser → DataBinder → XslFoRenderer →
  PDFFacade.generate(fo, "<data/>")`.

**The client supplies the data.** The Harmonia document page POSTs the record + items it already
loaded, with dropdown FKs resolved to display labels (`printPayload()` in
`document-page.js.template`). Deliberate: no server-side entity fetching, no auth forwarding, and
the printout matches exactly what the user sees. The JSON body is parsed with a **plain Gson**
(`ToNumberPolicy.LONG_OR_DOUBLE`) — never `JsonHelper`/`GsonHelper` (the `@Expose` trap; and
LONG_OR_DOUBLE keeps integers integral while decimals arrive as `Double`, which `DataBinder`
formats in the form money pattern `### ### ### ##0.00`).

## Structure notes

- The synchronizer, endpoint, `PrintTemplateCmsStore` and `PrintRenderer` share the root package
  `org.eclipse.dirigible.components.engine.document` so the CMS store can stay package-private
  while serving both consumers (Java packages don't nest). `domain`/`repository`/`service` follow
  the engine-openapi sub-package shape.
- `PrintTemplateCmsStore` is the **only** CMS surface: `seedDefaultTemplate` / `listLanguages` /
  `findTemplate`; `ensureFolder` walks/creates one level at a time (the engine `CmisFolder
  .createFolder` is single-level); a missing path is the `IOException` `getObjectByPath` throws
  (logged at DEBUG with the throwable). Writes go through the raw engine-cms interfaces
  (`CmisSessionFactory.getSession()`), which bypass CMS role checks — correct for a server-side
  seeder, same as `data-processes`' `BaseExportTask`.
- `PrintTemplate` stores the template source in a `PRINT_CONTENT` CLOB so the seeding phase does
  not re-read the repository.

## Renderer v1 limits (documented in `XslFoRenderer`, deliberate)

Header/footer render once in-flow (not repeated `fo:static-content` regions); `repeatHeader` and
`pageBreak` are ignored; 1 px = 1 pt; a table with no data rows is skipped entirely (FOP rejects
an empty `fo:table-body`). Lift these in the renderer, not here.

## Registration

`components/pom.xml` (`<module>`), `modules/pom.xml` dependencyManagement (components-tier
artifacts are managed there — the `dirigible-modules-parent` BOM that `components/` imports),
`components/group/group-engines/pom.xml`.
