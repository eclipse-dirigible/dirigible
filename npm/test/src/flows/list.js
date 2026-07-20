import { expect, test } from '../fixtures.js';
import { labelOf } from '../sample-values.js';

// The list page renders: plural title in the toolbar, then one of three bodies -
// a tree (hierarchy entities render role=treeitem nodes, no table), the table with one
// column header per major field, or (when the entity has no rows yet) the empty state.
// When seed data is expected, rows/nodes must actually be there.
export function listFlow(manifest, entity) {
  test(`${entity.name}: list page renders the declared columns`, async ({ page }) => {
    await page.goto(manifest.standaloneShell + entity.route);
    await expect(page.locator('[x-h-toolbar-title]', { hasText: entity.labelPlural }).first()).toBeVisible();
    if (entity.hierarchy) {
      if (entity.expectSeedData) {
        await expect(page.getByRole('treeitem').first()).toBeVisible();
      }
      return;
    }
    if (entity.layout === 'calendar' || entity.layout === 'slots') {
      // the view family renders a calendar / slot picker instead of the table
      await expect(page.locator('[x-h-calendar], [x-h-slot-picker]').first()).toBeVisible();
      return;
    }
    // filter({ visible: true }): the empty-state markup stays in the DOM (x-show) above the
    // table, so an unfiltered union's .first() would pick the hidden element and always fail
    const firstHeader = page.getByRole('columnheader').filter({ visible: true }).first();
    const emptyState = page.getByText('Get started by creating the first record').filter({ visible: true }).first();
    await expect(firstHeader.or(emptyState).first()).toBeVisible();
    if (entity.expectSeedData || (await firstHeader.isVisible())) {
      for (const field of (entity.fields ?? []).filter((f) => f.major !== false && !f.primaryKey)) {
        await expect(page.getByRole('columnheader').filter({ hasText: labelOf(field) }).first()).toBeVisible();
      }
      await expect(page.locator('tbody tr:visible').first()).toBeVisible();
    }
  });
}
