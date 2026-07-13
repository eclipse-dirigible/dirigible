import { expect, test } from '../fixtures.js';

// Shared application shell (/services/web/application/): the entity's menu item exists
// under its nav group and opens the module SPA in the embedded iframe. Requires the
// navigation module that defines the groups, so it is opt-in: APPTEST_SHELL=1.
export function shellFlow(manifest, entity) {
  test(`${entity.name}: shared shell menu item opens the module`, async ({ page }) => {
    test.skip(!process.env.APPTEST_SHELL, 'APPTEST_SHELL=1 enables the shared shell flow');
    await page.goto('/services/web/application/');
    const item = page.getByText(entity.labelPlural, { exact: true }).first();
    await expect(item).toBeVisible();
    await item.click();
    const app = page.frameLocator('iframe').first();
    await expect(app.getByText(entity.labelPlural, { exact: true }).first()).toBeVisible();
  });
}
