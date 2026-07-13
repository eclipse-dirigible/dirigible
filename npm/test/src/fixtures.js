import { expect, test as base } from '@playwright/test';
import { BASE_URL } from './env.js';
import { login } from './session.js';

// Pre-authenticated Playwright test:
// - authState: worker-scoped form login, one session per worker
// - storageState: every page starts logged in
// - api: an APIRequestContext carrying the same session, for REST-level checks
export const test = base.extend({
  authState: [
    async ({ browser }, use) => {
      await use(await login(browser));
    },
    { scope: 'worker' },
  ],
  storageState: async ({ authState }, use) => {
    await use(authState);
  },
  api: async ({ playwright, authState }, use) => {
    const request = await playwright.request.newContext({
      baseURL: BASE_URL,
      storageState: authState,
    });
    await use(request);
    await request.dispose();
  },
});

export { expect };
