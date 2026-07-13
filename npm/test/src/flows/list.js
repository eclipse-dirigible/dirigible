import { expect, test } from '../fixtures.js';
import { labelOf } from '../sample-values.js';

// The list page renders: plural title in the toolbar, one column header per major
// field, and (when seed data is expected) at least one data row.
export function listFlow(manifest, entity) {
  test(`${entity.name}: list page renders the declared columns`, async ({ page }) => {
    await page.goto(manifest.standaloneShell + entity.route);
    await expect(page.locator('[x-h-toolbar-title]', { hasText: entity.labelPlural }).first()).toBeVisible();
    for (const field of (entity.fields ?? []).filter((f) => f.major !== false && !f.primaryKey)) {
      await expect(page.getByRole('columnheader').filter({ hasText: labelOf(field) }).first()).toBeVisible();
    }
    if (entity.expectSeedData) {
      await expect(page.locator('tbody tr:visible').first()).toBeVisible();
    }
  });
}
