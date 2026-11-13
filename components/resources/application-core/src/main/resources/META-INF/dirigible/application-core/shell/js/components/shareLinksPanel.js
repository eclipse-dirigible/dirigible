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
/**
 * shareLinksPanel — the public links to a record's issued copy (intent `share:`, dirigible #8613).
 *
 * A record page of an entity declaring `share:` renders one of these in its rail, passing the
 * entity's controller path and the open record's id. It lists the record's links (newest first, as
 * the controller answers them), creates one and revokes one:
 *
 *   GET    {apiPath}/{id}/share-links           the links - 403 for a caller holding no share role
 *   POST   {apiPath}/{id}/share-links           a new link, the only answer that carries its `url`
 *   DELETE {apiPath}/{id}/share-links/{linkId}  revokes one
 *
 * A caller the list is refused to (403) never sees the section: `allowed` stays false and the page
 * keeps the section - and, when nothing else is in it, the rail - hidden, without an error.
 *
 * The URL of a link holds its secret token and the server never answers it again, so `created` keeps
 * the one just made for as long as this page stays open, to be copied from a read-only field.
 * Whether a link MAY be created is the server's decision (409 with its reason); the page only hides
 * the button while the record does not satisfy the entity's `share.when` terms.
 */
function shareLinksPanel(apiPath, recordId) {
  return {
    ...basePage(),
    apiPath,
    recordId,
    links: [],
    allowed: false, // true once the list was answered: the caller holds a share role
    state: 'loading', // loading | error | default
    error: null,
    creating: false,
    created: null, // { id, url } of the link made on this page - the only time its URL is known
    copied: false,
    revokeOpen: false,
    revokeTarget: null,
    revokeBusy: false,
    revokeError: null,

    async init() {
      await this.load();
      // An action can move the record out of its shareable state, and the server
      // may retire its links with it: re-read, as the page's other per-record sections do.
      this.onActionDone(() => this.load());
    },

    get basePath() {
      return this.apiPath + '/' + encodeURIComponent(this.recordId) + '/share-links';
    },

    t(key, fallback, options) {
      return window.T ? T('application-core:shell.shareLinks.' + key, fallback, options) : fallback;
    },

    async load() {
      if (this.recordId === null || this.recordId === undefined || this.recordId === '') return;
      try {
        this.links = await App.services.api.get(this.basePath) || [];
        this.allowed = true;
        this.error = null;
        this.state = 'default';
      } catch (e) {
        this.links = [];
        if (e && e.httpStatus === 403) {
          // Not a failure: sharing is not this caller's to see.
          this.allowed = false;
          return;
        }
        this.allowed = true;
        this.error = App.services.apiErrors.messageFor(e, this.t('loadFailed', 'Could not load the share links.'));
        this.state = 'error';
        console.error('[shareLinksPanel] load failed', e);
      }
    },

    async create() {
      if (this.creating) return;
      this.creating = true;
      this.error = null;
      this.copied = false;
      try {
        const link = await App.services.api.post(this.basePath);
        this.created = link && link.url ? { id: link.id, url: this.absoluteUrl(link.url) } : null;
        await this.load();
      } catch (e) {
        this.error = App.services.apiErrors.refusalMessageFor(e, this.t('createFailed', 'Could not create the link.'));
      } finally {
        this.creating = false;
      }
    },

    // The server answers a path when the application's base URL is not configured.
    absoluteUrl(url) {
      return String(url).charAt(0) === '/' ? window.location.origin + url : url;
    },

    async copy() {
      if (!this.created) return;
      const text = this.created.url;
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
          console.error('shareLinksPanel: could not copy to the clipboard', ignored);
        }
        area.remove();
      }
    },

    askRevoke(link) {
      this.revokeTarget = link;
      this.revokeError = null;
      this.revokeOpen = true;
    },

    async confirmRevoke() {
      if (!this.revokeTarget) return;
      this.revokeBusy = true;
      this.revokeError = null;
      try {
        await App.services.api.delete(this.basePath + '/' + encodeURIComponent(this.revokeTarget.id));
        // A revoked link's URL is of no use to anyone: stop offering it for copying.
        if (this.created && this.created.id === this.revokeTarget.id) this.created = null;
        this.revokeOpen = false;
        this.revokeTarget = null;
        await this.load();
      } catch (e) {
        this.revokeError = App.services.apiErrors.refusalMessageFor(e, this.t('revokeFailed', 'Could not revoke the link.'));
      } finally {
        this.revokeBusy = false;
      }
    },

    linkStatus(link) {
      if (link.status === 'REVOKED') return this.t('revoked', 'Revoked');
      if (link.status === 'EXPIRED') return this.t('expired', 'Expired');
      return this.t('active', 'Active');
    },

    linkVariant(link) {
      if (link.status === 'REVOKED') return 'negative';
      if (link.status === 'EXPIRED') return 'outline';
      return 'positive';
    },

    // "jane · 2026-11-13 10:42:07": who made the link and when, as the History trail words it.
    createdText(link) {
      const when = window.HarmoniaFormat.date(link.createdAt);
      return link.createdBy ? link.createdBy + ' · ' + when : when;
    },

    expiryText(link) {
      if (!link.expiresAt) return this.t('neverExpires', 'Never expires');
      return this.t('expires', 'Expires {{date}}', { date: window.HarmoniaFormat.date(link.expiresAt) });
    },

    openedText(link) {
      const count = Number(link.openCount) || 0;
      const opened = this.t('opened', 'Opened: {{count}}', { count });
      if (!count || !link.lastOpenedAt) return opened;
      return opened + ' · ' + this.t('lastOpened', 'last {{date}}', { date: window.HarmoniaFormat.date(link.lastOpenedAt) });
    },

    revokedText(link) {
      if (link.status !== 'REVOKED' || !link.revokedAt) return '';
      const when = window.HarmoniaFormat.date(link.revokedAt);
      return this.t('revokedBy', 'Revoked: {{who}}', { who: link.revokedBy ? link.revokedBy + ' · ' + when : when });
    },
  };
}
