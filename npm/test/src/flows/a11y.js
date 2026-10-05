import { expect, test } from '../fixtures.js';
import { CALENDAR_OR_SLOTS, listCrumb } from './list.js';

// The WCAG 2.1 level AA conformance rules, and the level A ones it includes (dirigible #7645).
const WCAG_21_AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];
const ENFORCED_IMPACTS = new Set(['serious', 'critical']);

// report (default): attach every page's axe result to the test report and pass - the first
// release of a module. strict: also fail on a serious or critical violation. off: no scan.
const MODE = process.env.APPTEST_A11Y ?? 'report';

// Harmonia's default palette fails contrast against its own --primary (white on it, and it as link
// text in dark mode); that is fixed in the component library, so strict mode does not fail an app
// for it. Any other contrast failure still counts.
const PRIMARY_COLOR = () => {
  const probe = document.createElement('div');
  probe.style.color = 'var(--primary)';
  document.body.appendChild(probe);
  const color = getComputedStyle(probe).color;
  probe.remove();
  const context = document.createElement('canvas').getContext('2d');
  context.fillStyle = color;
  context.fillRect(0, 0, 1, 1);
  const [r, g, b] = context.getImageData(0, 0, 1, 1).data;
  return '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('');
};

function sameColor(measured, expected) {
  if (typeof measured !== 'string' || measured.length !== 7 || !expected) return false;
  for (let channel = 1; channel < 7; channel += 2) {
    const a = parseInt(measured.slice(channel, channel + 2), 16);
    const b = parseInt(expected.slice(channel, channel + 2), 16);
    if (Math.abs(a - b) > 2) return false;
  }
  return true;
}

function onPrimary(node, primary) {
  return (node.any ?? []).some((check) => sameColor(check.data?.fgColor, primary) || sameColor(check.data?.bgColor, primary));
}

async function axeBuilder() {
  try {
    return (await import('@axe-core/playwright')).default;
  } catch {
    return null;
  }
}

// The list page and the create form of every entity with a page of its own, scanned by axe-core.
export function a11yFlow(manifest, entity) {
  if (MODE === 'off' || !entity.route) return;
  test(`${entity.name}: list and form pages pass axe-core (WCAG 2.1 AA)`, async ({ page }, testInfo) => {
    const AxeBuilder = await axeBuilder();
    test.skip(!AxeBuilder, 'add @axe-core/playwright to the harness devDependencies to run the accessibility flow');

    const calendar = entity.layout === 'calendar' || entity.layout === 'slots';
    const pages = [['list', entity.route, () => (calendar ? page.locator(CALENDAR_OR_SLOTS).first() : listCrumb(page, entity))]];
    if (!calendar && !entity.hierarchy) {
      pages.push(['form', entity.route + '/create', () => page.locator('input, select, textarea').filter({ visible: true }).first()]);
    }

    const findings = [];
    for (const [name, route, ready] of pages) {
      await page.goto(manifest.standaloneShell + route);
      await expect(ready()).toBeVisible();
      const results = await new AxeBuilder({ page }).withTags(WCAG_21_AA).analyze();
      await testInfo.attach(`axe-${entity.name}-${name}.json`, {
        body: JSON.stringify(results, null, 2),
        contentType: 'application/json',
      });
      const primary = await page.evaluate(PRIMARY_COLOR);
      for (const violation of results.violations) {
        if (!ENFORCED_IMPACTS.has(violation.impact)) continue;
        const nodes = violation.nodes.filter((node) => !(violation.id === 'color-contrast' && onPrimary(node, primary)));
        if (nodes.length) {
          const targets = nodes.slice(0, 5).map((node) => node.target.join(' ')).join(', ');
          findings.push(`${name}: [${violation.impact}] ${violation.id} - ${violation.help} (${nodes.length} elements: ${targets})`);
        }
      }
    }
    if (MODE === 'strict') {
      expect(findings, 'serious or critical WCAG 2.1 AA violations:\n' + findings.join('\n')).toEqual([]);
    }
  });
}
