import { expect, test } from '../fixtures.js';

// The read-time translation overlay: switch the shared language key (what the Region &
// Language setting writes), reload, and a known seed row shows its translated name.
// Needs a concrete sample in the manifest: { language, base, translated }.
export function multilingualFlow(manifest, entity) {
  const sample = entity.multilingualSample;
  if (!entity.multilingual || !sample) return;

  test(`${entity.name}: ${sample.language} translation overlays on read`, async ({ page }) => {
    await page.goto(manifest.standaloneShell + entity.route);
    await expect(page.locator('[x-h-toolbar-title]', { hasText: entity.labelPlural }).first()).toBeVisible();
    await page.evaluate((lang) => localStorage.setItem('codbex.harmonia.language', lang), sample.language);
    await page.reload();
    await expect(page.locator('tbody tr', { hasText: sample.translated }).first()).toBeVisible();
    await page.evaluate(() => localStorage.setItem('codbex.harmonia.language', 'en'));
    await page.reload();
    await expect(page.locator('tbody tr', { hasText: sample.base }).first()).toBeVisible();
  });
}
