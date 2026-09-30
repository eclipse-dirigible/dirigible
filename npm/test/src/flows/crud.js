import { expect, test } from '../fixtures.js';
import { makeApi } from '../api.js';
import { fillField, fillForm, resolveRelationSamples } from '../form.js';
import { freshUniqueKeys, freshUniques, handleField, sampleRecord } from '../sample-values.js';

// Server-side row lookup via the toolbar "Search <Entity>..." box - present on every list
// layout (manage-list, master-detail, document) and searching the string columns server-side.
// The per-column filter row is NOT used: its first input can belong to an FK or date column
// (hidden or non-text), which made a blind fill hang.
async function filterBy(page, value) {
  await page.getByPlaceholder(/^Search /).first().fill(value);
}

function dataRow(page, text) {
  return page.locator('tbody tr', { hasText: text });
}

// Edit and Delete of a row live in its "Row actions" menu - the one place every list layout
// (manage, document, master-detail) offers both; the manage list's record sheet puts Delete behind
// an overflow menu and its primary button may be a process task rather than Edit (#7491).
async function rowAction(page, text, action) {
  await dataRow(page, text).getByRole('button', { name: 'Row actions' }).click();
  await page.getByRole('menuitem', { name: action, exact: true }).click();
}

// A walk that fails halfway leaves its APPTEST- row behind, and the next run's REST flow then hits
// a unique: column with it (409). Remove whatever the walk created - the edited name starts with
// the created one - unless the UI delete already did.
async function removeLeftovers(client, manifest, entity, handle, created) {
  const idProperty = manifest.idProperty ?? 'Id';
  const rows = await client.search(entity, [{ propertyName: handle.name, operator: 'LIKE', value: created + '%' }]);
  for (const row of rows ?? []) await client.remove(entity, row[idProperty]);
}

export function crudFlow(manifest, entity, opts = {}) {
  const cfg = opts.extend?.entities?.[entity.name] ?? {};
  const skip = new Set(cfg.skip ?? []);
  if (skip.has('crud')) return;
  // no power page to walk: a composition child is reached through its parent (dirigible #7545)
  if (!entity.route) return;
  // A hierarchy entity lists as a tree, and a calendar/slots entity replaces the table page
  // entirely - no filter row / data rows to drive the walk below; create/read/update/delete
  // stays covered by the REST flow. Same for an entity without a string handle field (nothing
  // searchable identifies the created row in the table).
  if (entity.hierarchy || entity.layout === 'calendar' || entity.layout === 'slots') return;
  if (!handleField(entity)) return;

  test(`${entity.name}: create, edit and delete through the UI`, async ({ page, api }) => {
    const record = await freshUniques(makeApi(api, manifest), entity, sampleRecord(entity));
    const handle = handleField(entity);
    const relationSamples = await resolveRelationSamples(api, manifest, entity);
    const taken = await freshUniqueKeys(makeApi(api, manifest), entity, record, relationSamples);
    test.skip(!!taken, `every ${entity.name} combination of the unique key [${taken}] tried already has a row`);

    const created = record[handle.name];
    const listRoute = entity.route.replace(/[#/]/g, '\\$&');
    try {
      // create
      await page.goto(manifest.standaloneShell + entity.route);
      // exact: the empty state adds a second "New <Entity>" button that a substring match also hits
      await page.getByRole('button', { name: 'New', exact: true }).click();
      await expect(page).toHaveURL(/\/create$/);
      await cfg.beforeCreate?.(page, record);
      await fillForm(page, manifest, entity, record, relationSamples, opts);
      await page.getByRole('button', { name: 'Create', exact: true }).click();
      // A create lands on the NEW record's page when there is more to do there - a document's
      // line items, a manage record's detail collections - and on the list otherwise. Saving an
      // edit returns to the list, except on a document, whose Save stays on it (#7359).
      const recordPage = entity.layout === 'document' ? '/[^/]+/edit' : '(/[^/]+/edit)?';
      await expect(page).toHaveURL(new RegExp(listRoute + recordPage + '$'));
      if (page.url().endsWith('/edit')) await page.goto(manifest.standaloneShell + entity.route);
      await filterBy(page, created);
      await expect(dataRow(page, created)).toHaveCount(1);
      await cfg.afterCreate?.(page, record);

      // edit
      if (!skip.has('edit')) {
        const updated = created + '-UPD';
        await rowAction(page, created, 'Edit');
        await expect(page).toHaveURL(/\/edit$/);
        // the record loads async after the form renders - filling before the fetch completes
        // gets overwritten by the load and Save persists the OLD value
        await expect(page.locator('#f_' + handle.name)).toHaveValue(created);
        await fillField(page, handle, updated, opts);
        if (entity.layout === 'document') {
          // A document Save STAYS on the document and re-reads it (#7359): wait for the update to
          // land, then leave for the list the way a user does (dirigible #7545).
          const save = page.getByRole('button', { name: 'Save', exact: true });
          const saved = page.waitForResponse((response) => response.request().method() === 'PUT' && response.ok());
          await save.click();
          await saved;
          // Save is disabled again once the re-read header is the new pristine state - leaving before
          // that would meet the unsaved-changes guard
          await expect(save).toBeDisabled();
          await page.getByRole('button', { name: 'Back to list', exact: true }).click();
        } else {
          await page.getByRole('button', { name: 'Save', exact: true }).click();
        }
        await expect(page).toHaveURL(new RegExp(listRoute + '$'));
        await filterBy(page, updated);
        await expect(dataRow(page, updated)).toHaveCount(1);
        record[handle.name] = updated;
      }

      // delete (row menu, then the confirm dialog). An entity whose process declares
      // `whenDeleted: refuse` answers the delete with an error toast while its instance runs - the
      // create above started it - so the walk stops here and the REST flow asserts that refusal.
      if (!skip.has('delete') && !entity.deleteGuardedByProcess?.length) {
        await rowAction(page, record[handle.name], 'Delete');
        const dialog = page.locator('[x-h-dialog-overlay][data-open]');
        await dialog.getByRole('button', { name: 'Delete', exact: true }).click();
        await expect(dialog).toHaveCount(0);
        await filterBy(page, record[handle.name]);
        await expect(dataRow(page, record[handle.name])).toHaveCount(0);
      }
    } finally {
      await removeLeftovers(makeApi(api, manifest), manifest, entity, handle, created);
    }
  });
}
