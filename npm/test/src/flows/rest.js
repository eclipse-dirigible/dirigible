import { makeApi } from '../api.js';
import { expect, test } from '../fixtures.js';
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
    for (const relation of entity.relations ?? []) {
      const target = manifest.entities.find((e) => e.name === relation.to);
      const rows = await client.list(target, 1);
      expect(rows.length, `${relation.to} must have at least one row`).toBeGreaterThan(0);
      payload[relation.name] = rows[0][idProperty];
    }

    const created = await client.create(entity, payload);
    const id = created?.[idProperty];
    expect(id, 'create response carries the generated id').toBeTruthy();
    try {
      const read = await client.get(entity, id);
      expect(read[handle.name]).toBe(payload[handle.name]);

      const updatedValue = payload[handle.name] + '-UPD';
      await client.update(entity, id, { ...read, [handle.name]: updatedValue });
      const reread = await client.get(entity, id);
      expect(reread[handle.name]).toBe(updatedValue);
    } finally {
      await client.remove(entity, id);
    }
    const gone = await client.getResponse(entity, id);
    expect(gone.status()).toBe(404);
  });
}
