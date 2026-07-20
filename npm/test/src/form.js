import { makeApi } from './api.js';
import { editableFields, handleField } from './sample-values.js';

// Generated form controls: every field input has id="f_<Name>"; a to-one relation is a
// Harmonia x-h-select whose input holds the value and whose options carry the label.
export async function fillField(page, field, value, opts = {}) {
  const custom = opts.extend?.widgets?.[field.widget];
  if (custom) return custom(page, field, value);
  const input = page.locator('#f_' + field.name);
  if (field.type === 'boolean') return input.setChecked(!!value);
  if (field.type === 'timestamp' || field.type === 'datetime') {
    // the sample is a full ISO instant (what the REST layer binds); a datetime-local input
    // takes the zone-less YYYY-MM-DDTHH:mm prefix
    return input.fill(String(value).slice(0, 16));
  }
  await input.fill(String(value));
}

// The x-h-select directive hides its input and builds a span[role=combobox] trigger
// labelled by the field label; options carry role=option.
export async function pickDropdown(page, relation, optionText) {
  // Anchored prefix match: the combobox accessible name is the label plus the placeholder or
  // selected value ("Country Select a Country..."), so exact matching finds nothing - while a
  // bare substring match collides with longer sibling labels ("Type" also hits "Chart Type").
  const label = relation.label ?? relation.name;
  const anchored = new RegExp('^' + label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b');
  await page.getByRole('combobox', { name: anchored }).first().click();
  const option = page.getByRole('option', { name: optionText }).first();
  try {
    await option.click({ timeout: 10_000 });
  } catch {
    // the option list re-rendered mid-click (async option load reflow) - reopen and retry
    await page.getByRole('combobox', { name: anchored }).first().click();
    await option.click({ force: true });
  }
}

// Resolve a live option for each to-one relation: take the first suitable row of the
// target entity and use its label field's value as the visible option text.
// - an entityStatus relation is skipped: it renders as a status pill (not an editable
//   input in any form) and its value comes from the init: DB default;
// - a cross-model relation's rows come from its apiAbsolute controller URL (the target
//   lives in another module and is not in this manifest);
// - a where: option filter narrows the candidate rows to matching ones;
// - a dependsOn cascade forces CONSISTENT samples: the dependent row is chosen first and
//   its filterBy FK becomes the trigger sibling's sample (independent first rows would
//   pick e.g. Country=Afghanistan + City=Sofia, and the cascade then offers no options).
export async function resolveRelationSamples(request, manifest, entity) {
  const api = makeApi(request, manifest);
  const idProperty = manifest.idProperty ?? 'Id';

  async function fetchRows(relation, limit) {
    if (relation.apiAbsolute) return { rows: await api.listPath(relation.apiAbsolute, limit), labelFrom: relation.labelFrom ?? 'Name' };
    if (relation.api) return { rows: await api.listPath(manifest.restBase + relation.api, limit), labelFrom: relation.labelFrom ?? 'Name' };
    const target = manifest.entities.find((e) => e.name === relation.to);
    if (!target) throw new Error(`Relation ${entity.name}.${relation.name}: target ${relation.to} not in manifest`);
    return { rows: await api.list(target, limit), labelFrom: relation.labelFrom ?? handleField(target)?.name ?? 'Name' };
  }

  const samples = [];
  for (const relation of entity.relations ?? []) {
    if (relation.entityStatus) continue;
    // a filtered picker needs a matching candidate, so fetch a page and filter client-side
    const wide = relation.where || relation.leafOnly;
    const { rows: fetched, labelFrom } = await fetchRows(relation, wide ? 200 : 1);
    let rows = relation.where ? fetched?.filter((r) => String(r[relation.where.by]) === String(relation.where.value)) : fetched;
    if (relation.leafOnly && rows?.length) {
      // the generated validation rejects a non-leaf target - pick a row no other row parents
      const prop = relation.leafOnly.hierarchyProperty;
      const idProp = manifest.idProperty ?? 'Id';
      rows = rows.filter((row) => !fetched.some((other) => other[prop] === row[idProp]));
    }
    if (!rows?.length) {
      // a required FK cannot be satisfied - fail loudly; an optional one is simply left unset
      if (relation.required) throw new Error(`Relation ${entity.name}.${relation.name}: no ${relation.to} rows to pick from`);
      continue;
    }
    const label = rows[0][labelFrom];
    if (label == null && !relation.required) {
      // no display label to pick by (the target has no name-like field) - leave the optional
      // relation unset rather than clicking blind
      continue;
    }
    samples.push({
      relation,
      row: rows[0],
      id: rows[0][idProperty],
      label: label ?? String(rows[0][idProperty]),
    });
  }

  // cascade consistency: re-point each dependsOn trigger at the row the dependent's choice implies
  for (const sample of samples) {
    const dependsOn = sample.relation.dependsOn;
    if (!dependsOn?.filterBy) continue;
    const trigger = samples.find((s) => s.relation.name === dependsOn.relation);
    const impliedId = sample.row[dependsOn.filterBy];
    if (!trigger || impliedId == null || trigger.id === impliedId) continue;
    const path = trigger.relation.apiAbsolute ?? (trigger.relation.api ? manifest.restBase + trigger.relation.api : null);
    if (!path) continue;
    const row = await api.getPath(`${path}/${impliedId}`);
    if (!row) continue;
    trigger.row = row;
    trigger.id = impliedId;
    trigger.label = row[trigger.relation.labelFrom ?? 'Name'];
  }
  return samples;
}

export async function fillForm(page, manifest, entity, record, relationSamples, opts = {}) {
  for (const field of editableFields(entity)) {
    if (record[field.name] === undefined) continue;
    await fillField(page, field, record[field.name], opts);
  }
  // cascade order: a dependsOn trigger must be picked BEFORE its dependent, so the narrowed
  // option list is the one the dependent's sample was chosen from
  const triggers = relationSamples.filter((s) => !s.relation.dependsOn);
  const dependents = relationSamples.filter((s) => s.relation.dependsOn);
  for (const sample of [...triggers, ...dependents]) await pickDropdown(page, sample.relation, sample.label);
}
