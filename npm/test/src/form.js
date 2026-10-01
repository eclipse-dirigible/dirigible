import { makeApi } from './api.js';
import { expect } from './fixtures.js';
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

// The x-h-select directive hides its input and appends a button[role=combobox] trigger beside it;
// options carry role=option.
// The generated manage/document forms give that input id="f_<Name>", so the trigger is reached
// through its sibling - independent of how the field label reads. The accessible name is the
// fallback for a form whose select input carries no id (the my/partner surfaces).
async function relationTrigger(page, relation) {
  const input = page.locator('#f_' + relation.name);
  if (await input.count()) return input.locator('xpath=../button[@role="combobox"]');
  // Anchored prefix match: the combobox accessible name is the label plus the placeholder or
  // selected value ("Country Select a Country..."), so exact matching finds nothing - while a
  // bare substring match collides with longer sibling labels ("Type" also hits "Chart Type").
  const label = relation.label ?? relation.name;
  const anchored = new RegExp('^' + label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\b');
  return page.getByRole('combobox', { name: anchored }).first();
}

export async function pickDropdown(page, relation, optionText) {
  const trigger = await relationTrigger(page, relation);
  const input = page.locator('#f_' + relation.name);
  // visible only: an option list that is closing, or another select's, must not be the one clicked
  const option = page.getByRole('option', { name: optionText }).filter({ visible: true }).first();
  for (let attempt = 0; ; attempt++) {
    await trigger.click();
    try {
      await option.click({ timeout: 10_000 });
    } catch {
      // the option list re-rendered mid-click (async option load reflow) - reopen and retry
      await trigger.click();
      await option.click({ force: true });
    }
    // The list must be closed before the next relation's trigger is clicked: a click landing while
    // it is still open only dismisses it, and that select stays empty - the second of two relations
    // to the same entity was left at its placeholder that way (dirigible #7545).
    await expect(option).toBeHidden();
    if (!(await input.count()) || (await input.inputValue()) !== '' || attempt > 0) break;
  }
  if (await input.count()) await expect(input, `${relation.name} must hold the picked "${optionText}"`).not.toHaveValue('');
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
//   pick e.g. Country=Afghanistan + City=Sofia, and the cascade then offers no options);
// - a relation an agree check or a composite unique key names keeps a page of candidate rows, so
//   agreeing rows can be chosen here and a free key combination by freshUniqueKeys.
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

  const constrained = new Set([...(entity.agree ?? []).flatMap((check) => check.relations), ...(entity.uniqueKeys ?? []).flat()]);
  const samples = [];
  for (const relation of entity.relations ?? []) {
    if (relation.entityStatus) continue;
    // a filtered picker needs a matching candidate, so fetch a page and filter client-side; a
    // constrained relation needs alternatives to choose from
    const wide = relation.where || relation.leafOnly || constrained.has(relation.name);
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
    const sample = {
      relation,
      rows,
      // re-point the sample at another of its candidate rows
      use(row) {
        this.row = row;
        this.id = row[idProperty];
        this.label = row[labelFrom] ?? String(row[idProperty]);
      },
    };
    sample.use(rows[0]);
    samples.push(sample);
  }

  // cascade consistency: re-point each dependsOn trigger at the row the dependent's choice implies
  for (const sample of samples) {
    const dependsOn = sample.relation.dependsOn;
    if (!dependsOn?.filterBy) continue;
    const trigger = samples.find((s) => s.relation.name === dependsOn.relation);
    // a cascade pair is chosen together - neither side may be swapped for a candidate later
    sample.rows = [sample.row];
    if (trigger) trigger.rows = [trigger.row];
    const impliedId = sample.row[dependsOn.filterBy];
    if (!trigger || impliedId == null || trigger.id === impliedId) continue;
    const path = trigger.relation.apiAbsolute ?? (trigger.relation.api ? manifest.restBase + trigger.relation.api : null);
    if (!path) continue;
    const row = await api.getPath(`${path}/${impliedId}`);
    if (!row) continue;
    trigger.row = row;
    trigger.rows = [row];
    trigger.id = impliedId;
    trigger.label = row[trigger.relation.labelFrom ?? 'Name'];
  }
  alignAgreeing(entity, samples);
  return samples;
}

// An agree check refuses a record whose two relations point at targets that differ on onProperty
// (a transfer between stores of two companies), and the first row of each target is an arbitrary
// pair. Choose the first lead row for which every other side has a candidate carrying the same value.
function alignAgreeing(entity, samples) {
  for (const check of entity.agree ?? []) {
    const sides = check.relations.map((name) => samples.find((s) => s.relation.name === name));
    if (sides.some((side) => !side)) continue; // an unset side has nothing to disagree about
    const [lead, ...others] = sides;
    for (const row of lead.rows) {
      const value = row[check.onProperty];
      if (value == null) continue;
      const matches = others.map((side) => side.rows.find((candidate) => String(candidate[check.onProperty]) === String(value)));
      if (matches.some((match) => !match)) continue;
      lead.use(row);
      others.forEach((side, i) => side.use(matches[i]));
      break;
    }
  }
}

export async function fillForm(page, manifest, entity, record, relationSamples, opts = {}) {
  for (const field of editableFields(entity)) {
    if (record[field.name] === undefined) continue;
    await fillField(page, field, record[field.name], opts);
  }
  // a readOnly relation renders as a disabled combobox (an update-time calculated action, a
  // platform-owned FK) - the server sets it, so there is nothing to pick (dirigible #7554); the
  // REST flows still post its sample, which the calculated action overwrites or checks
  const pickable = relationSamples.filter((s) => !s.relation.readOnly);
  // cascade order: a dependsOn trigger must be picked BEFORE its dependent, so the narrowed
  // option list is the one the dependent's sample was chosen from
  const triggers = pickable.filter((s) => !s.relation.dependsOn);
  const dependents = pickable.filter((s) => s.relation.dependsOn);
  for (const sample of [...triggers, ...dependents]) await pickDropdown(page, sample.relation, sample.label);
}
