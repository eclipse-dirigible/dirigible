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
 * shells store — the other shells this instance ships, offered in every shell's user menu so
 * switching is one click instead of a detour through Home.
 *
 * The list is the SAME `platform-shells` aggregation Home renders, so a stack that contributes a
 * shell gets it in the switcher for free - there is no second registration. Reachability is the
 * platform's business: a shell the user's roles do not allow answers 403 when opened, exactly as it
 * does from Home; the listing has never been role-filtered and this store does not invent that.
 */
document.addEventListener('alpine:init', () => {
  const SHELLS_URL = '/services/js/platform-core/extension-services/shells.js';

  /** Fallback icons for shells registered before they declared their own. */
  const ICONS = {
    applicationShell: 'layout-grid',
    personalShell: 'inbox',
    partnerShell: 'handshake',
    adminShell: 'shield',
    monitoringShell: 'activity',
    builderShell: 'sparkles',
    shellIde: 'code',
  };

  Alpine.store('shells', {
    /** Every listed shell except the one being viewed, in registration order. */
    items: [],
    loaded: false,

    init() {
      this.load();
    },

    async load() {
      try {
        const response = await fetch(SHELLS_URL, { credentials: 'same-origin' });
        if (!response.ok) return;
        const listed = await response.json();
        this.items = (Array.isArray(listed) ? listed : []).filter((shell) =>
          shell && shell.path && shell.label && !this.isCurrent(shell))
          .sort((left, right) => (left.order ?? 100) - (right.order ?? 100));
      } catch (e) {
        // The switcher is a convenience; a shell whose list cannot be read simply does not offer it.
        console.error('shells: could not load the registered shells', e);
      } finally {
        this.loaded = true;
      }
    },

    /**
     * Whether a listed shell is the one currently open. The shell declares its own root in
     * App.config.basePath ('/services/web/monitoring'), which prefixes its registered path
     * ('/services/web/monitoring/index.html'); the URL is the fallback for a page without a config.
     *
     * @param {object} shell the listed shell
     * @return {boolean} true when it is the shell being viewed
     */
    isCurrent(shell) {
      const base = (window.App && App.config && App.config.basePath) || '';
      const root = base || window.location.pathname.replace(/\/[^/]*$/, '');
      return !!root && (shell.path === root || shell.path.indexOf(root + '/') === 0);
    },

    iconFor(shell) {
      return shell.icon || ICONS[shell.id] || 'box';
    },

    /** Leave for another shell. A full navigation, not a route change - these are separate pages. */
    open(shell) {
      window.location.assign(shell.path);
    },

    /** The launchpad, which also carries the shells' descriptions. */
    openHome() {
      window.location.assign('/home');
    },
  });
}, { once: true });
