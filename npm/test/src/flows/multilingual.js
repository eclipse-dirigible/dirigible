import { expect, test } from '../fixtures.js';
import { listCrumb } from './list.js';

const LANGUAGE_KEY = 'codbex.harmonia.language';
// A reload in another language is three round trips on a cold instance - the page, the list's
// data GET and the translation overlay behind it - so the 5 s default flaked on slower runners.
const OVERLAY_TIMEOUT = 15_000;

// The read-time translation overlay: switch the shared language key (what the Region &
// Language setting writes), reload, and a known seed row shows its translated name.
// Needs a concrete sample in the manifest: { language, base, translated }.
export function multilingualFlow(manifest, entity) {
  const sample = entity.multilingualSample;
  if (!entity.multilingual || !sample) return;

  test(`${entity.name}: ${sample.language} translation overlays on read`, async ({ page }) => {
    await page.goto(manifest.standaloneShell + entity.route);
    await expect(listCrumb(page, entity)).toBeVisible();
    const translatedRead = await reloadIn(page, manifest, entity, sample.language);
    await expectRowShowing(page, sample.translated, translatedRead);
    const baseRead = await reloadIn(page, manifest, entity, 'en');
    await expectRowShowing(page, sample.base, baseRead);
  });
}

// Set the language key and reload, waiting for the list's own data GET (the paged getAll on the
// entity's controller) so the assertion starts once the rows are on their way, and answering what
// that read asked for and got back - which is what tells a slow render from a missing translation.
async function reloadIn(page, manifest, entity, language) {
  await page.evaluate(([key, lang]) => localStorage.setItem(key, lang), [LANGUAGE_KEY, language]);
  const controllerPath = manifest.restBase + entity.api;
  const listRead = page.waitForResponse(
    (response) => response.request().method() === 'GET' && new URL(response.url()).pathname === controllerPath,
    { timeout: OVERLAY_TIMEOUT },
  );
  await page.reload();
  const response = await listRead;
  return {
    language,
    acceptLanguage: await response.request().headerValue('accept-language'),
    status: response.status(),
    body: await response.text(),
  };
}

async function expectRowShowing(page, text, read) {
  const rows = page.locator('tbody tr');
  try {
    await expect(rows.filter({ hasText: text }).first()).toBeVisible({ timeout: OVERLAY_TIMEOUT });
  } catch (error) {
    const firstRow = (await rows.allInnerTexts())[0]?.replace(/\s+/g, ' ').trim();
    throw new Error(
      `no list row shows '${text}' after reloading in '${read.language}': the list GET sent Accept-Language ` +
        `${read.acceptLanguage ?? '(none)'} and answered ${read.status}, ` +
        `${read.body.includes(text) ? 'WITH' : 'WITHOUT'} '${text}' in its body; the table's first row reads ` +
        `${firstRow ? `'${firstRow}'` : '(no rows)'}`,
      { cause: error },
    );
  }
}
