import { makeApi } from './api.js';
import { editableFields, handleField } from './sample-values.js';

// Generated form controls: every field input has id="f_<Name>"; a to-one relation is a
// Harmonia x-h-select whose input holds the value and whose options carry the label.
export async function fillField(page, field, value, opts = {}) {
  const custom = opts.extend?.widgets?.[field.widget];
  if (custom) return custom(page, field, value);
  const input = page.locator('#f_' + field.name);
  if (field.type === 'boolean') return input.setChecked(!!value);
  await input.fill(String(value));
}

// The x-h-select directive hides its input and builds a span[role=combobox] trigger
// labelled by the field label; options carry role=option.
export async function pickDropdown(page, relation, optionText) {
  // exact: a substring match collides with longer sibling labels ("Type" vs "Chart Type")
  await page.getByRole('combobox', { name: relation.label ?? relation.name, exact: true }).click();
  await page.getByRole('option', { name: optionText }).first().click();
}

// Resolve a live option for each to-one relation: take the first existing row of the
// target entity and use its label field's value as the visible option text.
export async function resolveRelationSamples(request, manifest, entity) {
  const api = makeApi(request, manifest);
  const samples = [];
  for (const relation of entity.relations ?? []) {
    const target = manifest.entities.find((e) => e.name === relation.to);
    if (!target) throw new Error(`Relation ${entity.name}.${relation.name}: target ${relation.to} not in manifest`);
    const rows = await api.list(target, 1);
    if (!rows?.length) throw new Error(`Relation ${entity.name}.${relation.name}: no ${relation.to} rows to pick from`);
    const labelFrom = relation.labelFrom ?? handleField(target).name;
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
