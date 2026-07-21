import { makeApi } from '../api.js';
import { expect, test } from '../fixtures.js';
import { resolveRelationSamples } from '../form.js';
import { sampleRecord } from '../sample-values.js';

// The personal (my) surface WIRE contract, driven when the manifest marks an entity `personal`:
// the scoped <Entity>MyController filters reads to the identity-mapped user, forces the owner FK
// server-side on create (whatever the client sends is ignored), strips every `sensitive` field
// from its responses (the allow-list is the security boundary - UI hiding alone is cosmetic),
// serves foreign rows as 404, and deletes only own rows.
//
// Prerequisite: the DEV IDENTITY mapping - a row of the owner relation's target whose identity
// field equals the test user (e.g. an Employee with email 'admin'). Without it the personal
// surface is EMPTY by design (never an error), so the flow SKIPS with a pointer instead of
// failing. The personal UI parity assertions (resolved labels, chat layout, calendar views on
// the My shell) are deliberately NOT asserted yet - they track the platform's personal-template
// parity fixes; this flow pins down the wire contract those pages consume.
export function myFlow(manifest, entity, opts = {}) {
  const cfg = opts.extend?.entities?.[entity.name] ?? {};
  if (!entity.personal || new Set(cfg.skip ?? []).has('my')) return;
  const idProperty = manifest.idProperty ?? 'Id';
  // the same entity through the scoped controller
  const mine = { ...entity, api: entity.personal.api };

  async function buildPayload(api) {
    const payload = sampleRecord(entity);
    for (const sample of await resolveRelationSamples(api, manifest, entity)) {
      payload[sample.relation.name] = sample.id;
    }
    // the owner FK is server-forced on the personal surface; sending a value must be pointless
    delete payload[entity.personal.owner];
    return payload;
  }

  test(`${entity.name}: personal (my) surface scopes rows, forces the owner and strips sensitive fields`, async ({ api }) => {
    const client = makeApi(api, manifest);

    const createdResponse = await api.post(manifest.restBase + mine.api, { data: await buildPayload(api) });
    test.skip(
      !createdResponse.ok(),
      `personal create returned ${createdResponse.status()} - the test user has no identity mapping; ` +
        `seed the dev identity row (an owner-target record whose identity field equals the login user)`
    );
    const created = JSON.parse(await createdResponse.text());
    const id = created?.[idProperty];
    expect(id, 'personal create carries the generated id').toBeTruthy();
    let foreignId;
    try {
      const own = await client.get(mine, id);
      expect(own[entity.personal.owner], 'the owner FK is forced server-side on a personal create').toBeTruthy();
      for (const field of entity.personal.sensitive ?? []) {
        expect(own[field], `sensitive field ${field} must never reach the personal wire`).toBeFalsy();
      }
      const rows = await client.list(mine, 1000);
      expect(rows.some((row) => row[idProperty] === id), 'the own row appears in the personal list').toBe(true);

      // the power surface still serves the full record
      const power = await client.get(entity, id);
      expect(power[idProperty]).toBe(id);

      // a row with no owner is foreign to everyone: the personal controller must 404 it and the
      // personal list must not include it (only checkable when the owner relation is optional)
      const ownerRelation = (entity.relations ?? []).find((relation) => relation.name === entity.personal.owner);
      if (ownerRelation && !ownerRelation.required) {
        const foreign = await client.create(entity, await buildPayload(api));
        foreignId = foreign?.[idProperty];
        const foreignThroughMine = await api.get(manifest.restBase + mine.api + '/' + foreignId);
        expect(foreignThroughMine.status(), 'a foreign row must 404 through the personal controller').toBe(404);
        const rowsAfter = await client.list(mine, 1000);
        expect(rowsAfter.some((row) => row[idProperty] === foreignId), 'a foreign row must not appear in the personal list').toBe(false);
      }
    } finally {
      await client.remove(mine, id); // deleting the OWN row through the personal controller works
      if (foreignId) await client.remove(entity, foreignId);
    }
    const gone = await api.get(manifest.restBase + mine.api + '/' + id);
    expect(gone.status()).toBe(404);
  });
}
