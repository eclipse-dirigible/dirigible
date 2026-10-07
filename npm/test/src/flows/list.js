import { expect, test } from '../fixtures.js';
import { labelOf } from '../sample-values.js';

// The calendar / slot-picker view root. The slots page renders `x-h-slot-picker.responsive`, and an
// attribute selector matches an attribute NAME exactly, so the modifier form is listed with its dot
// escaped (dirigible #7545).
export const CALENDAR_OR_SLOTS = '[x-h-calendar], [x-h-slot-picker], [x-h-slot-picker\\.responsive]';

// The shell breadcrumb's current crumb - the one page element every list layout shares that names
// the entity (the sidebar label, which the manifest's labelPlural mirrors).
export function listCrumb(page, entity) {
  return page.locator('[x-h-breadcrumb-page]', { hasText: entity.labelPlural }).first();
}

// The list page renders under the shell breadcrumb naming it by its plural label (the #7491
// manage list carries no title of its own), then one of three bodies -
// a tree (hierarchy entities render role=treeitem nodes, no table), the table with one
// column header per major field, or (when the entity has no rows yet) the empty state.
// When seed data is expected, rows/nodes must actually be there.
export function listFlow(manifest, entity) {
  // no power page: a composition child is reached through its parent (dirigible #7545)
  if (!entity.route) return;
  test(`${entity.name}: list page renders the declared columns`, async ({ page }) => {
    await page.goto(manifest.standaloneShell + entity.route);
    await expect(listCrumb(page, entity)).toBeVisible();
    if (entity.hierarchy) {
      if (entity.expectSeedData) {
        await expect(page.getByRole('treeitem').first()).toBeVisible();
      }
      return;
    }
    if (entity.layout === 'calendar' || entity.layout === 'slots') {
      // the view family renders a calendar / slot picker instead of the table
      await expect(page.locator(CALENDAR_OR_SLOTS).first()).toBeVisible();
      return;
    }
    // filter({ visible: true }): the empty-state markup stays in the DOM (x-show) above the
    // table, so an unfiltered union's .first() would pick the hidden element and always fail
    const firstHeader = page.getByRole('columnheader').filter({ visible: true }).first();
    const emptyState = page.getByText('Get started by creating the first record').filter({ visible: true }).first();
    await expect(firstHeader.or(emptyState).first()).toBeVisible();
    if (entity.expectSeedData || (await firstHeader.isVisible())) {
      for (const label of declaredColumns(entity)) {
        await expect(page.getByRole('columnheader').filter({ hasText: label }).first()).toBeVisible();
      }
      for (const label of hiddenColumns(entity)) {
        await expect(page.getByRole('columnheader').filter({ hasText: label })).toHaveCount(0);
      }
      await expect(page.locator('tbody tr:visible').first()).toBeVisible();
    }
  });
}

// The headers the list is expected to render, in order (dirigible #7664). `list:` curates the list
// INDEPENDENTLY of each field's own `major`, so an entity that declares it renders exactly those
// columns and the `major` rule says nothing about it - which is why asserting the rule against a
// curated entity failed on the first instance that held a row, and passed on every empty one,
// where the assertion never runs at all.
function declaredColumns(entity) {
  if (entity.list?.length) return entity.list.map((name) => columnLabel(entity, name));
  return (entity.fields ?? []).filter((f) => f.major !== false && !f.primaryKey).map(labelOf);
}

// ...and the ones it must NOT render. A curated list is the whole statement, so a field left off it
// is absent from the table however its own `major` reads - the half that catches a `list:` the page
// ignored.
function hiddenColumns(entity) {
  if (!entity.list?.length) return [];
  const listed = new Set(entity.list);
  return (entity.fields ?? [])
    .filter((f) => !f.primaryKey && !listed.has(f.name))
    .map(labelOf);
}

// A `list:` entry names a property - a field, or a to-one relation - and the header carries that
// property's own label, which is what the manifest's `label` on the field already is.
function columnLabel(entity, name) {
  const field = (entity.fields ?? []).find((f) => f.name === name);
  if (field) return labelOf(field);
  const relation = (entity.relations ?? []).find((r) => r.name === name);
  return relation?.label ?? name;
}
