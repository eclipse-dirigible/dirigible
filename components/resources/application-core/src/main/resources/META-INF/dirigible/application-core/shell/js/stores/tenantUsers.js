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
 * A tenant owner sees who belongs to the tenant and where each person stands, invites a person with
 * one or more roles, changes a member's roles in one save, and removes a member. Every change is ONE
 * request to /services/security/tenant-users, answered 202 at once; an external provisioning system
 * carries it out and writes back what it did, and the list shows that - this page never decides
 * membership. A change in flight shows on the roles it touches (ADDING / REMOVING); a change that
 * was not applied shows as the user's lastError. The endpoint family exists only when the platform
 * enables the feature - so a 404 on the context means "not offered here" and the section stays hidden.
 *
 * The markup is ONE fragment (shell/views/_tenant-users.html), mounted by the application shell's
 * Settings and by every generated application shell through x-html, like the tenant chip. The mounts
 * use only `visible`, `markup` and `load()`. Shells generated before this store existed never load
 * it; shared code that reaches for it must check Alpine.store('tenantUsers') first.
 */
document.addEventListener('alpine:init', () => {
  const CONTEXT_URL = '/services/security/tenant-users/context';
  const USERS_URL = '/services/security/tenant-users';
  const MARKUP_URL = '/services/web/application-core/shell/views/_tenant-users.html';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
  /** How often a change in flight is re-read, and - once it has taken a while - how much less often. */
  const POLL_FAST_MS = 5 * 1000;
  const POLL_SLOW_MS = 30 * 1000;
  const SLOW_AFTER_MS = 2 * 60 * 1000;

  Alpine.store('tenantUsers', {
    /** What the platform says: { enabled, tenantId, canManage, canRead, ownerRole, roles, caller }; null until loaded. */
    context: null,
    /** The live users of the tenant, as the provisioning system last reported them. */
    items: [],
    loaded: false,
    busy: false,
    /** The last refusal, in the user's words; empty when there is none. */
    error: '',
    /** The invitation being typed. */
    invite: { email: '', roles: [] },
    /** The user the edit dialog changes; null when closed. */
    edit: null,
    /** The roles ticked in the edit dialog - always an array, so the checkboxes can bind it while the dialog is closed. */
    editRoles: [],
    /** The user the remove dialog asks about; null when closed. */
    removeTarget: null,
    /**
     * This page's own changes not yet seen in the list, by email: { revision, at, roles }. The revision
     * is the row's at submit, or null for a person not in the list yet. An entry goes once the row's
     * revision passes it - or the row appears, for a new person, or disappears, for a removal.
     */
    sent: {},
    /** The fragment the section renders from. */
    markup: '',
    /** A load asked for before the context arrived - e.g. the page opened on the section's route. */
    loadRequested: false,
    pollTimer: null,
    pollSince: 0,

    /** The section is offered to the owners of the current tenant only. */
    get visible() {
      return !!(this.context && this.context.enabled && this.context.canManage);
    },

    /** The roles an owner may grant. */
    get roles() {
      return (this.context && this.context.roles) || [];
    },

    init() {
      this.loadContext();
      document.addEventListener('visibilitychange', () => {
        if (!document.hidden && this.needsPolling()) this.load();
      });
    },

    async loadContext() {
      try {
        const response = await fetch(CONTEXT_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.context = { enabled: false };
          return;
        }
        this.context = await response.json();
        if (this.invite.roles.length === 0) this.invite.roles = this.defaultRoles();
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

    /** Reads the users of the tenant - called when the section is opened, after every change, and while one is in flight. */
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
        this.settleSent();
      } catch (e) {
        console.error('tenantUsers: could not read the users', e);
        this.error = this.messageFor(0, null);
      } finally {
        this.loaded = true;
        this.schedulePoll();
      }
    },

    // ---- the three commands - each one request, answered 202 ----

    /** Invites the person in the form with the roles chosen. */
    async inviteUser() {
      const email = (this.invite.email || '').trim().toLowerCase();
      const roles = this.invite.roles.slice();
      if (!email || roles.length === 0) return;
      const existing = this.userByEmail(email);
      await this.send('POST', USERS_URL, { email: email, roles: roles }, () => {
        this.track(email, existing ? existing.revision : null, roles);
        this.announce(this.t('shell.tenantUsers.invited', 'Invitation sent to {{email}}', { email: email }));
        this.invite.email = '';
        this.invite.roles = this.defaultRoles();
      });
    },

    /** Sends the roles ticked in the edit dialog - one request, adding and removing together. */
    async saveRoles() {
      if (!this.canSave) return;
      const user = this.edit;
      const roles = this.editRoles.slice().sort();
      const url = USERS_URL + '/' + encodeURIComponent(user.id) + '/roles';
      await this.send('PUT', url, { roles: roles, expectedRevision: user.revision }, () => {
        this.track(user.email, user.revision, roles);
        this.announce(this.t('shell.tenantUsers.saved', 'Role change sent for {{email}}', { email: user.email }));
        this.edit = null;
      });
    },

    /** Removes the user the remove dialog asks about - or, called with a failed row, dismisses it. */
    async removeUser(target) {
      const user = target || this.removeTarget;
      if (!user) return;
      const url = USERS_URL + '/' + encodeURIComponent(user.id);
      await this.send('DELETE', url, { expectedRevision: user.revision }, () => {
        this.track(user.email, user.revision, []);
        if (!this.isFailed(user)) {
          this.announce(this.t('shell.tenantUsers.removed', 'Removal sent for {{email}}', { email: user.email }));
        }
        this.removeTarget = null;
      });
    },

    async send(method, url, body, onSuccess) {
      if (this.busy) return;
      this.busy = true;
      this.error = '';
      try {
        const response = await fetch(url, {
          method: method,
          headers: Object.assign({ 'Content-Type': 'application/json' }, HEADERS),
          credentials: 'same-origin',
          body: JSON.stringify(body),
        });
        if (!response.ok) {
          this.error = this.messageFor(response.status, await this.refusalOf(response));
          return;
        }
        const accepted = await this.refusalOf(response);
        // The id of the published request - for support, never shown.
        if (accepted && accepted.requestId) console.info('tenantUsers: request', accepted.requestId, 'accepted');
        onSuccess();
        this.pollSince = Date.now();
        await this.load();
      } catch (e) {
        console.error('tenantUsers: the request failed', e);
        this.error = this.messageFor(0, null);
      } finally {
        this.busy = false;
      }
    },

    // ---- the dialogs ----

    openEdit(user) {
      this.error = '';
      this.edit = user;
      this.editRoles = this.heldRoles(user);
    },

    closeEdit() {
      this.edit = null;
    },

    openRemove(user) {
      this.error = '';
      this.removeTarget = user;
    },

    closeRemove() {
      this.removeTarget = null;
    },

    /** Fills the invite form from a failed row, so the owner can send the invitation again. */
    inviteAgain(user) {
      this.invite.email = user.email;
      const roles = this.heldRoles(user);
      this.invite.roles = roles.length ? roles : this.defaultRoles();
      const input = document.getElementById('tenant-users-email');
      if (input) input.focus();
    },

    /** The roles ticked in the edit dialog that the user does not hold. */
    get rolesAdded() {
      if (!this.edit) return [];
      const held = this.heldRoles(this.edit);
      return this.editRoles.filter((role) => !held.includes(role)).sort();
    },

    /** The roles the user holds that are unticked in the edit dialog. */
    get rolesRemoved() {
      if (!this.edit) return [];
      return this.heldRoles(this.edit).filter((role) => !this.editRoles.includes(role)).sort();
    },

    get canSave() {
      return !!this.edit && !this.busy && this.editRoles.length > 0 && (this.rolesAdded.length > 0 || this.rolesRemoved.length > 0);
    },

    /** Whether the edit would leave the tenant without an owner - by this list's count; the provisioning system decides. */
    get editLeavesNoOwner() {
      return !!this.edit && this.rolesRemoved.includes(this.ownerRole) && this.ownersLeft(this.edit) === 0;
    },

    get removeLeavesNoOwner() {
      return !!this.removeTarget && this.heldRoles(this.removeTarget).includes(this.ownerRole) && this.ownersLeft(this.removeTarget) === 0;
    },

    get ownerRole() {
      return (this.context && this.context.ownerRole) || 'Owner';
    },

    // ---- what a row shows ----

    isSelf(user) {
      return !!(this.context && this.context.caller && user && user.email === this.context.caller);
    },

    /** A change is in flight while the user is pending, or any role is being added or removed. */
    inProgress(user) {
      return user.status === 'PENDING' || (user.roles || []).some((role) => role.state === 'ADDING' || role.state === 'REMOVING');
    },

    /** This page sent a change for the user that the list does not show yet. */
    sending(user) {
      return !!this.sent[user.email];
    },

    isPending(user) {
      return this.inProgress(user) || this.sending(user);
    },

    isActive(user) {
      return !!user.lastSignInAt;
    },

    isFailed(user) {
      return user.status === 'FAILED';
    },

    /** A member, whose roles can be changed - not a failed invitation. */
    isMember(user) {
      return user.status === 'INVITED' || user.status === 'ASSIGNED';
    },

    /** The roles the user holds now: granted, or being removed but not yet. */
    heldRoles(user) {
      return (user.roles || []).filter((role) => role.state === 'GRANTED' || role.state === 'REMOVING').map((role) => role.role);
    },

    /** How many other live users hold the owner role. */
    ownersLeft(except) {
      return this.items.filter((user) => user.email !== (except && except.email) && this.heldRoles(user).includes(this.ownerRole)).length;
    },

    /** People this page invited that the list does not show yet - each shown as a row of its own, "Sending...". */
    get sendingInvites() {
      return Object.keys(this.sent)
        .filter((email) => this.sent[email].revision === null && !this.userByEmail(email))
        .sort()
        .map((email) => ({ email: email, roles: this.sent[email].roles }));
    },

    /** Why the latest change was not applied - shown while nothing is in flight; null otherwise. */
    outcome(user) {
      if (!user.lastError || this.isPending(user)) return null;
      const code = user.lastError.code;
      if (code === 'FAILED') {
        return {
          failed: true,
          text: this.t('shell.tenantUsers.outcome.failed', 'Failed:'),
          detail: user.lastError.message || this.t('shell.tenantUsers.reason.FAILED', 'The change could not be carried out.'),
        };
      }
      return {
        failed: false,
        text: this.t('shell.tenantUsers.outcome.refused', 'Not applied:'),
        detail: this.reasonText(code, user.lastError.message),
      };
    },

    /** The words for a reason the provisioning system refused a change with; its own message when the code is unknown. */
    reasonText(code, message) {
      const known = {
        REVISION_CONFLICT: 'The user changed in the meantime. Reload and try again.',
        ALREADY_MEMBER: 'The person is already a member of this tenant.',
        NOT_A_MEMBER: 'The person is not a member of this tenant.',
        LAST_OWNER: 'The tenant would be left without an owner.',
        INVALID_ROLE: 'One of the roles is not a role of this application.',
        NO_ROLES: 'No role was chosen.',
        TENANT_NOT_ACTIVE: 'The tenant is not active.',
        WORKSPACE_NOT_PROVISIONED: 'The application is not provisioned for this tenant yet.',
        ACCOUNT_MISSING: 'The person has no account.',
      };
      if (known[code]) return this.t('shell.tenantUsers.reason.' + code, known[code]);
      return message || code || '';
    },

    statusVariant(user) {
      if (this.isActive(user) && this.isMember(user)) return 'positive';
      switch (user.status) {
        case 'INVITED':
        case 'ASSIGNED': return 'information';
        case 'FAILED': return 'negative';
        default: return 'outline';
      }
    },

    statusLabel(user) {
      if (this.isActive(user) && this.isMember(user)) return this.t('shell.tenantUsers.status.ACTIVE', 'Active');
      const fallback = { PENDING: 'Pending', INVITED: 'Invited', ASSIGNED: 'Assigned', FAILED: 'Failed' }[user.status] || user.status;
      return this.t('shell.tenantUsers.status.' + user.status, fallback);
    },

    /** A held role is a plain badge, one being added a positive "+", one being removed a negative "-". */
    roleVariant(role) {
      switch (role.state) {
        case 'ADDING': return 'positive';
        case 'REMOVING': return 'negative';
        default: return 'outline';
      }
    },

    roleLabel(role) {
      switch (role.state) {
        case 'ADDING': return '+ ' + role.role;
        case 'REMOVING': return '− ' + role.role;
        default: return role.role;
      }
    },

    roleTitle(role) {
      if (!role.grantedAt) return '';
      return this.t('shell.tenantUsers.grantedBy', 'Granted by {{by}} on {{at}}', { by: role.grantedBy || '-', at: this.time(role.grantedAt) });
    },

    /** A timestamp in the user's locale; empty when there is none. */
    time(iso) {
      if (!iso) return '';
      const date = new Date(iso);
      return isNaN(date.getTime()) ? iso : date.toLocaleString();
    },

    // ---- internals ----

    defaultRoles() {
      const roles = this.roles;
      if (roles.includes('User')) return ['User'];
      return roles.length ? [roles[roles.length - 1]] : [];
    },

    userByEmail(email) {
      return this.items.find((user) => user.email === email) || null;
    },

    track(email, revision, roles) {
      this.sent = Object.assign({}, this.sent, { [email]: { revision: revision, at: Date.now(), roles: roles } });
    },

    /** Drops the page's own changes the list now shows. */
    settleSent() {
      const sent = {};
      for (const email of Object.keys(this.sent)) {
        const entry = this.sent[email];
        const row = this.userByEmail(email);
        const seen = entry.revision === null ? !!row : (!row || row.revision > entry.revision);
        if (!seen) sent[email] = entry;
      }
      this.sent = sent;
    },

    needsPolling() {
      return Object.keys(this.sent).length > 0 || this.items.some((user) => this.inProgress(user));
    },

    /** The section is on screen: mounted, shown and in a visible tab. */
    onScreen() {
      const page = document.getElementById('tenant-users-page');
      return !document.hidden && !!page && page.offsetParent !== null;
    },

    /** Re-reads the list while a change is in flight and the section is on screen; stops when nothing is. */
    schedulePoll() {
      if (this.pollTimer) {
        clearTimeout(this.pollTimer);
        this.pollTimer = null;
      }
      if (!this.needsPolling()) {
        this.pollSince = 0;
        return;
      }
      if (!this.pollSince) this.pollSince = Date.now();
      if (!this.onScreen()) return;
      const interval = Date.now() - this.pollSince > SLOW_AFTER_MS ? POLL_SLOW_MS : POLL_FAST_MS;
      this.pollTimer = setTimeout(() => this.load(), interval);
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
      const known = {
        INVALID_EMAIL: 'Enter a valid email address.',
        INVALID_ROLE: 'Choose one of the offered roles.',
        NO_ROLES: 'Choose at least one role.',
        DEFAULT_TENANT: 'Select a tenant to manage its users.',
        NOT_A_TENANT_OWNER: 'Only an owner of this tenant can manage its users.',
        PUBLISH_FAILED: 'The change could not be sent. Try again in a moment.',
        ALREADY_MEMBER: 'This person is already a member - change their roles instead.',
        REQUEST_PENDING: 'A change for this person is still in progress. Wait for it to finish.',
        NOT_A_MEMBER: 'This person is not a member whose roles can be changed.',
        STALE_REVISION: 'This person changed since the list was loaded. Reload and try again.',
        NO_CHANGE: 'This person already has exactly these roles.',
        LAST_OWNER: 'This person is the only owner of the tenant. Make someone else an owner first.',
        USER_NOT_FOUND: 'This person is no longer in the list. Reload it.',
      };
      if (reason && known[reason]) return this.t('shell.tenantUsers.error.' + reason, known[reason]);
      if (status === 401) return this.t('shell.tenantUsers.error.SESSION', 'Your session has expired. Reload the page to sign in again.');
      return this.t('shell.tenantUsers.error.FAILED', 'Something went wrong. Please try again.');
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
