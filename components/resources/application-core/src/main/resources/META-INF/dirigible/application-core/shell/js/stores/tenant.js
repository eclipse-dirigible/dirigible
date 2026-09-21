/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/*
 * tenant store — the tenant this page runs in, and the tenants the user could switch to, for the
 * chip every shell header mounts next to the user menu.
 *
 * Everything on a page silently depends on the tenant - the data, the menu, the roles - and until
 * this store nothing told the user which one they were in. The state comes from one endpoint that
 * answers on every resolution strategy; the store only decides what the chip shows:
 *  - nothing on a single-tenant instance (`visible` is false),
 *  - a read-only chip where the tenant is the host's (SUBDOMAIN) or the user has no other tenant,
 *  - a menu of the offered tenants where there is another one to show, entered with the same POST
 *    the picker uses. A tenant that cannot be entered is listed and says why, as on the picker.
 *
 * The markup is ONE fragment (`shell/views/_tenant-menu.html`), fetched and rendered through x-html
 * so nine pages do not carry nine copies of it. Loaded by shells that carry no i18n service too
 * (Home), so translation goes through `t()` below rather than a bare `T`.
 */
document.addEventListener('alpine:init', () => {
  const CURRENT_URL = '/services/security/tenant-selection/current';
  const SELECTION_URL = '/services/security/tenant-selection';
  const PICKER_URL = '/tenant-selection.html?switch=true';
  const MENU_URL = '/services/web/application-core/shell/views/_tenant-menu.html';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };

  Alpine.store('tenant', {
    /** The tenant of this request: { id, name, defaultTenant }; null until loaded. */
    current: null,
    /** The tenants the user's groups grant: { id, name, provisionedHere, state }. */
    items: [],
    /** Whether this instance serves more than the default tenant at all. */
    multitenant: false,
    /** How this instance resolves tenants: 'SUBDOMAIN' or 'TOKEN_GROUPS'. */
    strategy: '',
    /** Whether at least one offered tenant other than the current one can be entered now. */
    switchable: false,
    loaded: false,
    busy: false,
    /** The refusal of the last switch, in the user's words; empty when there was none. */
    error: '',
    /** The fragment the chip renders from. */
    markup: '',

    /** The chip is for multitenant instances; a single-tenant one has nothing to say. */
    get visible() {
      return this.loaded && this.multitenant && !!this.current;
    },

    /** Whether the chip opens a menu: only when there is a tenant other than the current one to show. */
    get hasMenu() {
      return this.items.some((tenant) => !this.isCurrent(tenant));
    },

    get label() {
      return this.current ? (this.current.name || this.current.id) : '';
    },

    /** Why the chip is read-only, for its tooltip. */
    get hint() {
      if (this.strategy === 'SUBDOMAIN') return this.t('shell.tenant.hostBound', 'The tenant is determined by the host');
      if (this.current && this.current.defaultTenant) return this.t('shell.tenant.default', 'The default tenant of this instance');
      return this.t('shell.tenant.current', 'The tenant you are working in');
    },

    init() {
      this.load();
      this.loadMarkup();
    },

    async load() {
      try {
        const response = await fetch(CURRENT_URL, { headers: HEADERS, credentials: 'same-origin' });
        // Not answered (an instance without the endpoint, a page that lost its session): no chip,
        // rather than a chip that guesses.
        if (!response.ok) return;
        const state = await response.json();
        this.current = state.tenant || null;
        this.items = Array.isArray(state.tenants) ? state.tenants : [];
        this.multitenant = !!state.multitenant;
        this.strategy = state.resolutionStrategy || '';
        this.switchable = !!state.switchable;
      } catch (e) {
        console.error('tenant: could not load the current tenant', e);
      } finally {
        this.loaded = true;
      }
    },

    async loadMarkup() {
      try {
        const response = await fetch(MENU_URL, { credentials: 'same-origin' });
        if (response.ok) this.markup = await response.text();
      } catch (e) {
        console.error('tenant: could not load the tenant menu', e);
      }
    },

    isCurrent(tenant) {
      return !!this.current && tenant.id === this.current.id;
    },

    /** A tenant the user can move to: entered here, and not the one they are in. */
    canEnter(tenant) {
      return tenant.state === 'READY' && !this.isCurrent(tenant);
    },

    /**
     * Why a listed tenant cannot be entered, in the picker's words - two reasons, two sentences:
     * waiting ends the first and never ends the second. Empty for a tenant that can be entered.
     *
     * @param {object} tenant the offered tenant
     * @return {string} the explanation, or ''
     */
    stateText(tenant) {
      if (tenant.state === 'READY') return '';
      if (tenant.state === 'PREPARING') return this.t('shell.tenant.preparing', 'Being prepared. Try again in a few minutes.');
      return this.t('shell.tenant.unavailable', 'Not available. Contact your administrator.');
    },

    /**
     * Enter another tenant. The selection and the roles are replaced together, no re-login; it
     * applies from the next request, so the shell is reloaded at its root - the route the user is on
     * may not exist in the other tenant. A refusal leaves the current tenant as it is and is shown.
     *
     * @param {object} tenant the offered tenant to enter
     */
    async switchTo(tenant) {
      if (this.busy || !this.canEnter(tenant)) return;
      this.busy = true;
      this.error = '';
      try {
        const response = await fetch(SELECTION_URL, {
          method: 'POST',
          headers: { ...HEADERS, 'Content-Type': 'application/json' },
          credentials: 'same-origin',
          body: JSON.stringify({ tenantId: tenant.id }),
        });
        if (response.ok) {
          this.reloadShell();
          return;
        }
        this.error = this.messageFor(response.status, await this.refusalOf(response));
        await this.load();
      } catch (e) {
        console.error('tenant: could not switch the tenant', e);
        this.error = this.t('shell.tenant.switchFailed', 'The tenant could not be switched.');
      } finally {
        this.busy = false;
      }
    },

    /** The refusal body of a failed selection, or null when there is none to read. */
    async refusalOf(response) {
      try {
        return await response.json();
      } catch (e) {
        return null;
      }
    },

    /**
     * The picker's own wording for a refused selection, keyed by the status the endpoint answers
     * with; a 409 carries the reason that tells a tenant being prepared from one never registered.
     */
    messageFor(status, refusal) {
      switch (status) {
        case 401: return this.t('shell.tenant.sessionExpired', 'Your session has expired. Reload the page to sign in again.');
        case 403: return this.t('shell.tenant.notAMember', 'You are no longer assigned to this tenant.');
        case 404: return this.t('shell.tenant.notSelectable', 'This application does not let users select a tenant.');
        case 409: return refusal && refusal.reason === 'UNKNOWN_HERE'
          ? this.t('shell.tenant.notAvailable', 'This tenant is not available. Contact your administrator.')
          : this.t('shell.tenant.stillPreparing', 'This tenant is still being prepared. Try again in a few minutes.');
        default: return this.t('shell.tenant.switchFailed', 'The tenant could not be switched.');
      }
    },

    /** The full picker page, opened as the switcher. */
    openPicker() {
      window.location.assign(PICKER_URL);
    },

    /**
     * Land on the shell's root after a switch - of the hosting window when this page is embedded in
     * another shell of the same origin, since the host shows the tenant too.
     */
    reloadShell() {
      let target = window;
      try {
        if (window.top && window.top !== window && window.top.location.host === window.location.host) target = window.top;
      } catch (e) {
        // A foreign host: reloading this frame is all that is possible.
      }
      target.location.replace(target.location.pathname);
    },

    /** Translate a shell key; the fallback where the page carries no i18n service (Home). */
    t(key, fallback) {
      return typeof window.T === 'function' ? T('application-core:' + key, fallback) : fallback;
    },
  });
}, { once: true });
