/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
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
 * The Home landing page component. Standalone on purpose: it loads no shell runtime - only the
 * platform branding (window.PlatformBranding), the logged-in user's name, and the shells
 * aggregated from the `platform-shells` extension point, rendered as the destination entries.
 */
document.addEventListener('alpine:init', () => {
    // Fallbacks for shells that do not declare their own icon/description (older registrations).
    const ICONS = { applicationShell: 'layout-grid', myShell: 'inbox', partnerShell: 'handshake', shellIde: 'code' };
    const DESCRIPTIONS = {
        applicationShell: 'All business applications in one workspace.',
        myShell: 'Your tasks and your records - the personal workspace.',
        partnerShell: 'The portal for external partners.',
        shellIde: 'The development workbench for building on the platform.',
    };

    Alpine.data('home', () => ({
        branding: { name: '', subtitle: '', logo: '' },
        userName: '',
        shells: [],
        loaded: false,
        dark: false,

        async init() {
            const b = window.PlatformBranding || (window.top && window.top.PlatformBranding) || {};
            this.branding = { name: b.name || '', subtitle: b.subtitle || '', logo: b.logo || '' };
            if (this.branding.name) document.title = this.branding.name;
            const favicon = b.icons && b.icons.favicon;
            if (favicon) {
                const link = document.querySelector('link[rel="icon"]');
                if (link) link.href = favicon;
            }
            try { this.dark = window.Harmonia && Harmonia.getColorScheme() === 'dark'; } catch (e) { this.dark = false; }
            await Promise.all([this.loadUser(), this.loadShells()]);
            this.loaded = true;
        },

        async loadUser() {
            try {
                const r = await fetch('/services/js/platform-core/services/user-name.js', {
                    headers: { 'Accept': 'text/plain' }, credentials: 'same-origin',
                });
                if (r.ok) this.userName = (await r.text()).trim();
            } catch (e) {
                console.error('home: could not load the user name', e);
            }
        },

        async loadShells() {
            try {
                const r = await fetch('/services/js/platform-core/extension-services/shells.js', { credentials: 'same-origin' });
                if (!r.ok) return;
                const list = await r.json();
                this.shells = (Array.isArray(list) ? list : [])
                    .filter((s) => s && s.path && s.label)
                    .sort((a, b) => (a.order ?? 100) - (b.order ?? 100));
            } catch (e) {
                console.error('home: could not load the shells', e);
            }
        },

        greeting() {
            const h = new Date().getHours();
            if (h < 5) return 'Welcome back';
            if (h < 12) return 'Good morning';
            if (h < 18) return 'Good afternoon';
            return 'Good evening';
        },

        iconFor(id) { return ICONS[id] || 'box'; },
        descriptionFor(id) { return DESCRIPTIONS[id] || ''; },

        toggleTheme() {
            this.dark = !this.dark;
            if (window.Harmonia) Harmonia.setColorScheme(this.dark ? 'dark' : 'light');
        },

        logout() { window.location.replace('/logout'); },
    }));
});
