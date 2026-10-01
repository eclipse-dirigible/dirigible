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
  if (!entity.multilingual || !sample || !entity.route) return;

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
// The wait is registered only once the reload has committed: registered before it, it also matched
// a list GET still in flight from the document the reload replaced, whose body is gone (#7584). The
// new document issues its own GET only after its scripts have loaded, so it cannot slip past.
async function reloadIn(page, manifest, entity, language) {
  await page.evaluate(([key, lang]) => localStorage.setItem(key, lang), [LANGUAGE_KEY, language]);
  const controllerPath = manifest.restBase + entity.api;
  await page.reload({ waitUntil: 'commit' });
  const response = await page.waitForResponse(
    (candidate) => candidate.request().method() === 'GET' && new URL(candidate.url()).pathname === controllerPath,
    { timeout: OVERLAY_TIMEOUT },
  );
  return {
    language,
    acceptLanguage: await response.request().headerValue('accept-language'),
    status: response.status(),
    // Only the diagnostic reads the body, so a body the browser no longer holds must not fail the flow.
    body: await response.text().catch(() => null),
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
        `${describeBody(read.body, text)}; the table's first row reads ` +
        `${firstRow ? `'${firstRow}'` : '(no rows)'}`,
      { cause: error },
    );
  }
}

function describeBody(body, text) {
  if (body === null) return 'its body unavailable';
  return `${body.includes(text) ? 'WITH' : 'WITHOUT'} '${text}' in its body`;
}
