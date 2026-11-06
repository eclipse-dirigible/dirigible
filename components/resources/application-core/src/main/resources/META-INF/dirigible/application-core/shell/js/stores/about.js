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
 * about store - the About dialog of the user menu: which application, and which platform, the user is
 * running (#8502).
 *
 * The answer comes from /services/core/version: the application a deployment declares
 * (applicationName / applicationVersion, from DIRIGIBLE_APPLICATION_NAME / DIRIGIBLE_APPLICATION_VERSION,
 * set by the image that packages the application) and the platform under it (productName,
 * productVersion, productCommitId). A deployment that declares no application version shows the
 * platform alone. The tenant comes from the tenant chip's store, when the shell carries one and the
 * instance is multitenant. "Copy" puts the same lines on the clipboard, for a tester's report.
 *
 * The dialog is ONE fragment (shell/views/_about.html) fetched into `markup` and mounted with x-html by
 * every shell, like "Report an issue". Loaded by shells that may carry no i18n service, so translation
 * goes through `t()` below rather than a bare `T`.
 */
document.addEventListener('alpine:init', () => {
  const VERSION_URL = '/services/core/version';
  const MARKUP_URL = '/services/web/application-core/shell/views/_about.html';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
  const SHORT_COMMIT = 10;

  // A value the build left unfiltered (`${project.version}`) or blank is no value.
  function declared(value) {
    if (value === undefined || value === null) return '';
    const text = String(value).trim();
    return text.indexOf('${') >= 0 ? '' : text;
  }

  function brandingName() {
    try {
      return (window.HarmoniaBranding && window.HarmoniaBranding.name) || '';
    } catch (e) {
      return '';
    }
  }

  Alpine.store('about', {
    /** The dialog's markup. */
    markup: '',
    /** Whether the dialog is open. */
    dialogOpen: false,
    /** The version endpoint's answer; null until read. */
    version: null,
    loaded: false,
    /** Whether the version could not be read. */
    failed: false,
    /** Whether the lines were just copied. */
    copied: false,

    init() {
      this.loadMarkup();
    },

    async loadMarkup() {
      if (this.markup) return;
      try {
        const response = await fetch(MARKUP_URL, { credentials: 'same-origin' });
        if (response.ok) this.markup = await response.text();
      } catch (e) {
        console.error('about: could not load the About dialog', e);
      }
    },

    async open() {
      this.copied = false;
      this.dialogOpen = true;
      await this.load();
    },

    close() {
      this.dialogOpen = false;
    },

    async load() {
      if (this.version) return;
      this.failed = false;
      try {
        const response = await fetch(VERSION_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        this.version = await response.json();
      } catch (e) {
        console.error('about: could not read the version', e);
        this.failed = true;
      } finally {
        this.loaded = true;
      }
    },

    /** Whether the deployment declares the application it runs. */
    get hasApplication() {
      return !!this.applicationVersion;
    },

    /** The application's name: the declared one, else the instance branding. */
    get applicationName() {
      return declared(this.version && this.version.applicationName) || brandingName();
    },

    get applicationVersion() {
      return declared(this.version && this.version.applicationVersion);
    },

    get platformName() {
      return declared(this.version && this.version.productName) || 'Eclipse Dirigible';
    },

    get platformVersion() {
      return declared(this.version && this.version.productVersion);
    },

    get commit() {
      return declared(this.version && this.version.productCommitId);
    },

    get shortCommit() {
      return this.commit.slice(0, SHORT_COMMIT);
    },

    /** The tenant the user works in, when the instance is multitenant and the shell knows it. */
    get tenant() {
      try {
        const tenant = Alpine.store('tenant');
        return tenant && tenant.visible ? tenant.label : '';
      } catch (e) {
        return '';
      }
    },

    /** The dialog's lines as plain text, for the clipboard. */
    get text() {
      const lines = [];
      if (this.hasApplication) {
        lines.push(this.t('shell.about.application', 'Application') + ': ' + [this.applicationName, this.applicationVersion].filter(Boolean).join(' '));
      }
      lines.push(this.t('shell.about.platform', 'Platform') + ': ' + [this.platformName, this.platformVersion].filter(Boolean).join(' '));
      if (this.commit) lines.push(this.t('shell.about.commit', 'Commit') + ': ' + this.commit);
      if (this.tenant) lines.push(this.t('shell.about.tenant', 'Tenant') + ': ' + this.tenant);
      return lines.join('\n');
    },

    async copy() {
      const text = this.text;
      this.copied = false;
      try {
        await navigator.clipboard.writeText(text);
        this.copied = true;
      } catch (e) {
        // No clipboard API (an insecure origin): the old way, through a selected textarea.
        const area = document.createElement('textarea');
        area.value = text;
        area.setAttribute('readonly', '');
        area.style.cssText = 'position: fixed; opacity: 0;';
        document.body.appendChild(area);
        area.select();
        try {
          this.copied = document.execCommand('copy');
        } catch (ignored) {
          console.error('about: could not copy to the clipboard', ignored);
        }
        area.remove();
      }
    },

    t(key, fallback, options) {
      if (typeof window.T === 'function') return T('application-core:' + key, fallback, options);
      return String(fallback).replace(/\{\{(\w+)\}\}/g, (match, name) => (options && name in options ? options[name] : match));
    },
  });
});
