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
  await page.getByRole('option', { name: optionText }).first().click();
}

// Resolve a live option for each to-one relation: take the first existing row of the
// target entity and use its label field's value as the visible option text.
// - an entityStatus relation is skipped: it renders as a status pill (not an editable
//   input in any form) and its value comes from the init: DB default;
// - a cross-model relation's rows come from its apiAbsolute controller URL (the target
//   lives in another module and is not in this manifest).
export async function resolveRelationSamples(request, manifest, entity) {
  const api = makeApi(request, manifest);
  const samples = [];
  for (const relation of entity.relations ?? []) {
    if (relation.entityStatus) continue;
    let rows;
    let labelFrom = relation.labelFrom;
    if (relation.apiAbsolute) {
      rows = await api.listPath(relation.apiAbsolute, 1);
      labelFrom = labelFrom ?? 'Name';
    } else if (relation.api) {
      // same-model target via its relative controller path (works even for a composition
      // detail excluded from the manifest's entities list)
      rows = await api.listPath(manifest.restBase + relation.api, 1);
      labelFrom = labelFrom ?? 'Name';
    } else {
      const target = manifest.entities.find((e) => e.name === relation.to);
      if (!target) throw new Error(`Relation ${entity.name}.${relation.name}: target ${relation.to} not in manifest`);
      rows = await api.list(target, 1);
      labelFrom = labelFrom ?? handleField(target)?.name ?? 'Name';
    }
    if (!rows?.length) {
      // a required FK cannot be satisfied - fail loudly; an optional one is simply left unset
      if (relation.required) throw new Error(`Relation ${entity.name}.${relation.name}: no ${relation.to} rows to pick from`);
      continue;
    }
    samples.push({
      relation,
      id: rows[0][manifest.idProperty ?? 'Id'],
      label: rows[0][labelFrom],
    });
  }
  return samples;
}

export async function fillForm(page, manifest, entity, record, relationSamples, opts = {}) {
  for (const field of editableFields(entity)) await fillField(page, field, record[field.name], opts);
  for (const sample of relationSamples) await pickDropdown(page, sample.relation, sample.label);
}
