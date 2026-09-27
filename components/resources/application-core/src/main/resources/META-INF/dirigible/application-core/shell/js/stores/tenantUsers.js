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
 * tenantUsers store - the users of the current tenant, for the "Users" section of Settings.
 *
 * A tenant owner sees who belongs to the tenant, where each person stands, and invites another
 * person; the invitation is a request an external provisioning system answers, and its outcome is
 * recorded back into the same rows. Everything comes from one endpoint family,
 * /services/security/tenant-users, which exists only when the platform enables the feature - so a
 * 404 on the context means "not offered here" and the section stays hidden.
 *
 * The markup is ONE fragment (shell/views/_tenant-users.html), mounted by the application shell's
 * Settings and by every generated application shell through x-html, like the tenant chip. Shells
 * generated before this store existed never load it; shared code that reaches for it must check
 * Alpine.store('tenantUsers') first.
 */
document.addEventListener('alpine:init', () => {
  const CONTEXT_URL = '/services/security/tenant-users/context';
  const USERS_URL = '/services/security/tenant-users';
  const MARKUP_URL = '/services/web/application-core/shell/views/_tenant-users.html';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
  /** A request still unanswered after this long is shown as such, with a way to send it again. */
  const STALE_AFTER_MS = 15 * 60 * 1000;

  Alpine.store('tenantUsers', {
    /** What the platform says: { enabled, tenantId, canManage, canRead, ownerRole, roles }; null until loaded. */
    context: null,
    /** The users of the tenant, as the endpoint answers them. */
    items: [],
    loaded: false,
    busy: false,
    /** The last refusal, in the user's words; empty when there is none. */
    error: '',
    /** The invitation being typed. */
    form: { email: '', role: '' },
    /** The fragment the section renders from. */
    markup: '',
    /** A load asked for before the context arrived - e.g. the page opened on the section's route. */
    loadRequested: false,

    /** The section is offered to the owners of the current tenant only. */
    get visible() {
      return !!(this.context && this.context.enabled && this.context.canManage);
    },

    /** The roles an owner may grant, as configured. */
    get roles() {
      return (this.context && this.context.roles) || [];
    },

    init() {
      this.loadContext();
    },

    async loadContext() {
      try {
        const response = await fetch(CONTEXT_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.context = { enabled: false };
          return;
        }
        this.context = await response.json();
        if (!this.form.role) {
          const roles = this.roles;
          this.form.role = roles.includes('User') ? 'User' : (roles[roles.length - 1] || '');
        }
        if (this.visible) this.loadMarkup();
        if (this.visible && this.loadRequested) this.load();
      } catch (e) {
        console.error('tenantUsers: could not read the context', e);
        this.context = { enabled: false };
      }
    },

    async loadMarkup() {
      if (this.markup) return;
      try {
        const response = await fetch(MARKUP_URL, { credentials: 'same-origin' });
        if (response.ok) this.markup = await response.text();
      } catch (e) {
        console.error('tenantUsers: could not load the section', e);
      }
    },

    /** Reads the users of the tenant - called when the section is opened, and after every change. */
    async load() {
      if (this.context === null) {
        this.loadRequested = true;
        return;
      }
      this.loadRequested = false;
      if (!this.visible) return;
      this.loadMarkup();
      try {
        const response = await fetch(USERS_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return;
        }
        this.items = await response.json();
        this.error = '';
      } catch (e) {
        console.error('tenantUsers: could not read the users', e);
        this.error = this.messageFor(0, null);
      } finally {
        this.loaded = true;
      }
    },

    /** Invites the person in the form. */
    async invite() {
      const email = (this.form.email || '').trim();
      if (!email || !this.form.role) return;
      await this.send(USERS_URL, { email: email, role: this.form.role }, () => {
        this.announce(this.t('shell.tenantUsers.invited', 'Invitation sent to {{email}}', { email: email }));
        this.form.email = '';
      });
    },

    /** Sends a role's unanswered request again, with the same id. */
    async resend(user, role) {
      const url = USERS_URL + '/' + encodeURIComponent(user.id) + '/roles/' + encodeURIComponent(role.role) + '/resend';
      await this.send(url, {}, () => {
        this.announce(this.t('shell.tenantUsers.resent', 'Invitation sent again to {{email}}', { email: user.email }));
      });
    },

    async send(url, body, onSuccess) {
      if (this.busy) return;
      this.busy = true;
      this.error = '';
      try {
        const response = await fetch(url, {
          method: 'POST',
          headers: Object.assign({ 'Content-Type': 'application/json' }, HEADERS),
          credentials: 'same-origin',
          body: JSON.stringify(body),
        });
        if (!response.ok) {
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return;
        }
        onSuccess();
        await this.load();
      } catch (e) {
        console.error('tenantUsers: the request failed', e);
        this.error = this.messageFor(0, null);
      } finally {
        this.busy = false;
      }
    },

    /** A role requested and not answered for a while - the provisioning system may never have received the request. */
    isStale(role) {
      return role.state === 'REQUESTED' && !!role.requestedAt && Date.now() - Date.parse(role.requestedAt) > STALE_AFTER_MS;
    },

    statusVariant(status) {
      switch (status) {
        case 'ACTIVE': return 'positive';
        case 'INVITED':
        case 'ASSIGNED': return 'information';
        case 'FAILED': return 'negative';
        default: return 'outline';
      }
    },

    statusLabel(status) {
      const fallback = { PENDING: 'Pending', INVITED: 'Invited', ASSIGNED: 'Assigned', ACTIVE: 'Active', FAILED: 'Failed' }[status] || status;
      return this.t('shell.tenantUsers.status.' + status, fallback);
    },

    /** A granted role is a plain badge; a requested or failed one says so. */
    roleVariant(role) {
      switch (role.state) {
        case 'REQUESTED': return 'warning';
        case 'FAILED': return 'negative';
        default: return 'outline';
      }
    },

    roleLabel(role) {
      if (role.state === 'GRANTED') return role.role;
      const fallback = { REQUESTED: 'requested', FAILED: 'failed' }[role.state] || role.state;
      return role.role + ' - ' + this.t('shell.tenantUsers.roleState.' + role.state, fallback);
    },

    /** Who asked for the role and who granted it, with when. */
    roleTitle(role) {
      const lines = [];
      if (role.requestedAt) {
        lines.push(this.t('shell.tenantUsers.requested', 'Requested by {{by}} on {{at}}', { by: role.requestedBy || '-', at: this.time(role.requestedAt) }));
      }
      if (role.grantedAt) {
        lines.push(this.t('shell.tenantUsers.grantedBy', 'Granted by {{by}} on {{at}}', { by: role.grantedBy || '-', at: this.time(role.grantedAt) }));
      }
      if (role.errorMessage) lines.push(role.errorMessage);
      return lines.join('\n');
    },

    /** A timestamp in the user's locale; empty when there is none. */
    time(iso) {
      if (!iso) return '';
      const date = new Date(iso);
      return isNaN(date.getTime()) ? iso : date.toLocaleString();
    },

    async refusalOf(response) {
      try {
        return await response.json();
      } catch (e) {
        return null;
      }
    },

    /** The words for a refusal - from the endpoint's reason, never its raw text. */
    messageFor(status, refusal) {
      const reason = refusal && refusal.reason;
      switch (reason) {
        case 'REQUEST_PENDING': return this.t('shell.tenantUsers.error.pending', 'A request for this role is still waiting for an answer. Send it again from the list instead.');
        case 'ROLE_ALREADY_GRANTED': return this.t('shell.tenantUsers.error.granted', 'This person already has that role.');
        case 'INVALID_EMAIL': return this.t('shell.tenantUsers.error.email', 'Enter a valid email address.');
        case 'INVALID_ROLE': return this.t('shell.tenantUsers.error.role', 'Choose one of the offered roles.');
        case 'PUBLISH_FAILED': return this.t('shell.tenantUsers.error.publish', 'The invitation could not be sent. Try again in a moment.');
        case 'NOT_A_TENANT_OWNER': return this.t('shell.tenantUsers.error.owner', 'Only an owner of this tenant can manage its users.');
        case 'DEFAULT_TENANT': return this.t('shell.tenantUsers.error.tenant', 'Select a tenant to manage its users.');
        case 'NOT_PENDING': return this.t('shell.tenantUsers.error.notPending', 'This request has already been answered. Invite the person again instead.');
        default: break;
      }
      if (status === 401) return this.t('shell.tenantUsers.error.session', 'Your session has expired. Reload the page to sign in again.');
      return this.t('shell.tenantUsers.error.failed', 'Something went wrong. Please try again.');
    },

    announce(message) {
      const notifications = Alpine.store('notifications');
      if (notifications && typeof notifications.announce === 'function') {
        notifications.announce({ title: message, variant: 'positive' });
      }
    },

    /** Translate a shell key, with {{name}} placeholders; the fallback where the page carries no i18n service. */
    t(key, fallback, options) {
      if (typeof window.T === 'function') return T('application-core:' + key, fallback, options);
      return String(fallback).replace(/\{\{(\w+)\}\}/g, (match, name) => (options && name in options ? options[name] : match));
    },
  });
}, { once: true });
