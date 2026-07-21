import fs from 'node:fs';
import { test } from './fixtures.js';
import { crudFlow } from './flows/crud.js';
import { listFlow } from './flows/list.js';
import { multilingualFlow } from './flows/multilingual.js';
import { myFlow } from './flows/my.js';
import { restFlow } from './flows/rest.js';
import { shellFlow } from './flows/shell.js';

// Entry point: execute a module's generated <name>.test manifest. Accepts the parsed
// manifest object or a path to the file. opts.extend hosts the custom-UI hooks (widget
// fillers, per-entity skip lists, before/afterCreate).
export function runTest(manifestRef, opts = {}) {
  const manifest = typeof manifestRef === 'string' ? JSON.parse(fs.readFileSync(manifestRef, 'utf8')) : manifestRef;
  for (const entity of manifest.entities ?? []) {
    test.describe(`${manifest.module} / ${entity.name}`, () => {
      listFlow(manifest, entity, opts);
      crudFlow(manifest, entity, opts);
      restFlow(manifest, entity, opts);
      myFlow(manifest, entity, opts);
      multilingualFlow(manifest, entity, opts);
      shellFlow(manifest, entity, opts);
    });
  }
}

// Deprecated alias - the pre-subpath name; use runTest.
export const runAppTest = runTest;
