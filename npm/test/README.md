# @aerokit/test (published as the `@aerokit/sdk/test` subpath)

> **Publishing note:** this package is NOT published standalone - the release workflow bundles
> `src/` into the `@aerokit/sdk` npm package as its `./test` subpath export (the npm token cannot
> create new packages in the scope). Consumers depend on `@aerokit/sdk` and import
> `@aerokit/sdk/test` / `@aerokit/sdk/test/fixtures`; this folder stays the source of truth and the
> local-development identity (`file:` installs during development resolve `@aerokit/test` directly).

Generic [Playwright](https://playwright.dev) runner that executes an intent module's generated
`<name>.test` manifest against a running Eclipse Dirigible instance. The manifest (emitted by the
`AppTestIntentGenerator` in `engine-intent`, alongside `.edm`/`.model`/`.bpmn`/...) says WHAT the
app promises — entities, layouts, fields, seeds, languages; this package knows HOW to verify a
promise against the generated Harmonia UI and the reused REST controllers. So one runner, versioned
alongside the templates whose markup it drives, checks every generated app the same way.

This is the app-integration-test surface (drives a browser + the REST layer from outside the
instance). It is distinct from `@aerokit/sdk/junit`, the in-instance server-side unit-test facade.

## Use from a module

A module keeps a tiny `test/` harness at its **repo root** (outside the published project folder, so
it never enters the registry or the npm package):

```js
// test/app.spec.js
import { fileURLToPath } from 'node:url';
import { runTest } from '@aerokit/sdk/test';

runTest(fileURLToPath(new URL('../<project>/<name>.test', import.meta.url)));
```

```json
// test/package.json
{
  "type": "module",
  "scripts": { "test": "playwright test" },
  "devDependencies": { "@aerokit/test": "*", "@playwright/test": "^1.45" }
}
```

```
BASE_URL=http://localhost:8080 npm test        # defaults: admin/admin, installed Chrome
APPTEST_SHELL=1 npm test                       # also assert the shared-shell menu item
```

Env: `BASE_URL` (default `http://localhost:8080`), `APPTEST_USERNAME`/`APPTEST_PASSWORD`
(default `admin`/`admin`), `APPTEST_SHELL` (opt-in shared-shell flow).

## Flows per entity

- **list** — the plural title, a column header per major field, and (when `expectSeedData`) a row.
- **crud** — UI create → filter → row appears; edit via the detail pane → save; delete → confirm → gone.
- **rest** — the same CRUD over the generated Java controllers via `APIRequestContext` (isolates
  backend vs UI failures), asserting the manifest's field names bind and delete yields 404.
- **multilingual** — switch the shared language key, reload, a seeded row shows its translated name.
- **my** — the personal (my) surface WIRE contract, when the manifest marks an entity `personal`:
  create through the scoped `<Entity>MyController` (owner FK forced server-side), every `sensitive`
  field null on the personal wire, the own row in the personal list, foreign rows 404 (when the
  owner relation is optional), own-row delete. Skips with a pointer when the test user has no
  identity mapping (seed the dev identity row). Personal *UI* parity (resolved labels, chat,
  calendar on the My shell) is deliberately not asserted yet — it tracks the personal-template
  parity fixes.
- **shell** (opt-in) — the shared application shell's nav item opens the module SPA in its iframe.

Test records carry an `APPTEST-` prefix and are removed in teardown; seed data is never mutated.

## Custom UI hooks

`runTest(manifest, { extend })` (`runAppTest` remains a deprecated alias):

- `widgets: { '<widget>': async (page, field, value) => ... }` — custom widget fillers.
- `entities: { <Entity>: { skip: ['delete'], beforeCreate, afterCreate } }` — per-entity hooks.

Hand-written specs live in `test/custom/*.spec.js` and import `@aerokit/test/fixtures`
(pre-logged-in `test`, an `api` client, `expect`) so login/baseURL/cleanup come free.
