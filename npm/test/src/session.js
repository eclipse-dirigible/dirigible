import { BASE_URL, PASSWORD, USERNAME } from './env.js';

// Dirigible uses form login (basic auth is not accepted by the login flow). Logs in once
// per worker and returns a storageState (session cookie) reused by every test and by the
// REST client.
export async function login(browser) {
  const context = await browser.newContext({ baseURL: BASE_URL });
  const page = await context.newPage();
  await page.goto('/login');
  await page.fill('input[name="username"]', USERNAME);
  await page.fill('input[name="password"]', PASSWORD);
  await Promise.all([
    page.waitForURL((url) => !url.pathname.includes('/login')),
    page.click('button[type="submit"]'),
  ]);
  const state = await context.storageState();
  await context.close();
  return state;
}
