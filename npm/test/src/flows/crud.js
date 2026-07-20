import { expect, test } from '../fixtures.js';
import { fillField, fillForm, resolveRelationSamples } from '../form.js';
import { handleField, sampleRecord } from '../sample-values.js';

// Server-side per-column filter (the documented POST /search path). The handle field is
// the first major column, so its filter input is the first one in the filter row.
async function filterBy(page, value) {
  const filter = page.getByPlaceholder('Filter…').first();
  await filter.fill(value);
}

function dataRow(page, text) {
  return page.locator('tbody tr', { hasText: text });
}

export function crudFlow(manifest, entity, opts = {}) {
  const cfg = opts.extend?.entities?.[entity.name] ?? {};
  const skip = new Set(cfg.skip ?? []);
  if (skip.has('crud')) return;
  // A hierarchy entity lists as a tree, and a calendar/slots entity replaces the table page
  // entirely - no filter row / data rows to drive the walk below; create/read/update/delete
  // stays covered by the REST flow. Same for an entity without a string handle field (nothing
  // searchable identifies the created row in the table).
  if (entity.hierarchy || entity.layout === 'calendar' || entity.layout === 'slots') return;
  if (!handleField(entity)) return;

  test(`${entity.name}: create, edit and delete through the UI`, async ({ page, api }) => {
    const record = sampleRecord(entity);
    const handle = handleField(entity);
    const relationSamples = await resolveRelationSamples(api, manifest, entity);

    // create
    await page.goto(manifest.standaloneShell + entity.route);
    // exact: the empty state adds a second "New <Entity>" button that a substring match also hits
    await page.getByRole('button', { name: 'New', exact: true }).click();
    await expect(page).toHaveURL(/\/create$/);
    await cfg.beforeCreate?.(page, record);
    await fillForm(page, manifest, entity, record, relationSamples, opts);
    await page.getByRole('button', { name: 'Create', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(entity.route.replace(/[#/]/g, '\\$&') + '$'));
    await filterBy(page, record[handle.name]);
    await expect(dataRow(page, record[handle.name])).toHaveCount(1);
    await cfg.afterCreate?.(page, record);

    // edit (via the detail pane the row selection reveals)
    if (!skip.has('edit')) {
      const updated = record[handle.name] + '-UPD';
      await dataRow(page, record[handle.name]).click();
      // exact: substring name matching would also hit e.g. a "Credit Notes" sidebar item
      await page.getByRole('button', { name: 'Edit', exact: true }).first().click();
      await expect(page).toHaveURL(/\/edit$/);
      await fillField(page, handle, updated, opts);
      await page.getByRole('button', { name: 'Save', exact: true }).click();
      await expect(page).toHaveURL(new RegExp(entity.route.replace(/[#/]/g, '\\$&') + '$'));
      await filterBy(page, updated);
      await expect(dataRow(page, updated)).toHaveCount(1);
      record[handle.name] = updated;
    }

    // delete (detail pane trash button, then the confirm dialog)
    if (!skip.has('delete')) {
      await dataRow(page, record[handle.name]).click();
      await page.getByRole('button', { name: 'Delete', exact: true }).first().click();
      const dialog = page.locator('[x-h-dialog-overlay][data-open]');
      await dialog.getByRole('button', { name: 'Delete', exact: true }).click();
      await expect(dialog).toHaveCount(0);
      await filterBy(page, record[handle.name]);
      await expect(dataRow(page, record[handle.name])).toHaveCount(0);
    }
  });
}
