import { makeApi } from '../api.js';
import { expect, test } from '../fixtures.js';
import { resolveRelationSamples } from '../form.js';
import { handleField, sampleRecord } from '../sample-values.js';

// The same contract over the generated REST controllers, no browser: isolates backend
// failures from UI failures and verifies the manifest's field names bind.
export function restFlow(manifest, entity, opts = {}) {
  const cfg = opts.extend?.entities?.[entity.name] ?? {};
  if (new Set(cfg.skip ?? []).has('rest')) return;
  const idProperty = manifest.idProperty ?? 'Id';

  test(`${entity.name}: REST create, read, update and delete`, async ({ api }) => {
    const client = makeApi(api, manifest);
    const payload = sampleRecord(entity);
    const handle = handleField(entity);
    // same resolution the UI flow uses: cross-model targets via apiAbsolute, entityStatus
    // relations left to their init: DB default
    for (const sample of await resolveRelationSamples(api, manifest, entity)) {
      payload[sample.relation.name] = sample.id;
    }

    const created = await client.create(entity, payload);
    const id = created?.[idProperty];
    expect(id, 'create response carries the generated id').toBeTruthy();
    try {
      const read = await client.get(entity, id);
      expect(read[idProperty]).toBe(id);
      // the update round-trip needs a writable string field to flip; skip it when there is none
      if (handle) {
        expect(read[handle.name]).toBe(payload[handle.name]);

        const updatedValue = payload[handle.name] + '-UPD';
        await client.update(entity, id, { ...read, [handle.name]: updatedValue });
        const reread = await client.get(entity, id);
        expect(reread[handle.name]).toBe(updatedValue);
      }
    } finally {
      await client.remove(entity, id);
    }
    const gone = await client.getResponse(entity, id);
    expect(gone.status()).toBe(404);
  });
}
