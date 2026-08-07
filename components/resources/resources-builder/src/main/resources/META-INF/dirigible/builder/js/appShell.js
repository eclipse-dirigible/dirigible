/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Builder shell controller - a conversational AI intent builder.
 *
 * You describe the application you want; an app.intent is created and evolved underneath; the canvas
 * shows the entities, processes and glue that result; one button turns the conversation into a
 * running application. The intent YAML is never the authoring surface here (the Intent Editor in the
 * IDE remains that) - it is available read-only in a drawer, because seeing what was built is
 * reassuring, but editing it is not what this shell is for.
 *
 * State that several scopes read - the intent, the conversation, the publish run, the theme - lives
 * in Alpine STORES, never on this component: an eagerly evaluated binding against a component
 * property that Alpine has not resolved yet throws, and stores resolve from any scope at any time.
 */
const LAST_PROJECT_KEY = 'dirigible.builder.project';

document.addEventListener('alpine:init', () => {

  Alpine.data('app', () => ({
    /** The read-only YAML drawer ("view source"), deliberately low-key. */
    sourceOpen: false,
    copied: false,
    /** Openers offered on the empty canvas, so a first-time user is never facing a blank prompt. */
    examples: [
      'Create an order management app with customers, orders and an approval process for orders above 10000.',
      'Build a library: books, members, loans, and a process that reminds a member when a loan is overdue.',
      'I need an expense tracker where employees submit expenses and a manager approves them.',
      'Model a small CRM with companies, contacts and activities, plus a weekly report of activities per company.',
    ],

    async init() {
      const intent = Alpine.store('intent');
      // Fire-and-forget: the "not configured" banner appears as soon as the answer arrives, without
      // holding up the rest of the shell.
      Alpine.store('conversation').probeConfiguration();
      await intent.loadApps();

      // Reopen whatever was last worked on, so a browser refresh is not a lost app.
      let last = '';
      try { last = localStorage.getItem(LAST_PROJECT_KEY) || ''; } catch (e) { /* no storage */ }
      if (last && intent.apps.includes(last)) {
        await intent.open(last);
      }
      Alpine.store('conversation').restore();

      // The canvas re-renders whenever the model changes - the conversation raises this after every
      // applied proposal, and the publish panel after it is dismissed.
      document.addEventListener('builder:model-changed', () => this.renderDiagrams());
      this.renderDiagrams();
    },

    // ----- Canvas -------------------------------------------------------------

    /**
     * Draw the current model into the canvas. Deferred to the next tick because the host element is
     * inside an x-show branch that Alpine may only just have revealed - mxGraph measures its
     * container, so rendering into a hidden one lays out to nothing.
     */
    renderDiagrams() {
      this.$nextTick(() => {
        const host = document.getElementById('builder-diagrams');
        if (!host) return;
        const model = Alpine.store('intent').model;
        if (model) IntentDiagrams.render(model, host);
        else IntentDiagrams.dispose(host);
      });
    },

    // ----- Conversation -------------------------------------------------------

    async send() {
      const conversation = Alpine.store('conversation');
      const text = conversation.input;
      if (!text.trim() || conversation.busy) return;
      // A new turn supersedes the previous outcome panel, so the canvas shows the app again.
      if (Alpine.store('publish').state !== 'running') Alpine.store('publish').state = 'idle';
      await conversation.send(text);
      this.rememberProject();
    },

    /** Enter sends, Shift+Enter inserts a newline - the convention every chat UI shares. */
    onKey(event) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        this.send();
      }
    },

    useExample(text) {
      Alpine.store('conversation').input = text;
      const box = document.getElementById('builder-input');
      if (box) box.focus();
    },

    // ----- Apps ---------------------------------------------------------------

    rememberProject() {
      try {
        const project = Alpine.store('intent').project;
        if (project) localStorage.setItem(LAST_PROJECT_KEY, project);
      } catch (e) { /* no storage */ }
    },

    async openApp(project) {
      const intent = Alpine.store('intent');
      if (intent.project === project) return;
      Alpine.store('publish').state = 'idle';
      if (await intent.open(project)) {
        this.rememberProject();
        Alpine.store('conversation').restore();
        this.renderDiagrams();
      }
    },

    newApp() {
      Alpine.store('intent').reset();
      Alpine.store('conversation').clear();
      Alpine.store('publish').state = 'idle';
      try { localStorage.removeItem(LAST_PROJECT_KEY); } catch (e) { /* no storage */ }
      this.renderDiagrams();
    },

    // ----- Source drawer ------------------------------------------------------

    toggleSource() {
      this.sourceOpen = !this.sourceOpen;
      this.copied = false;
    },

    async copySource() {
      try {
        await navigator.clipboard.writeText(Alpine.store('intent').yaml || '');
        this.copied = true;
        setTimeout(() => { this.copied = false; }, 2000);
      } catch (e) {
        console.error('builder: could not copy the source', e);
      }
    },

    // ----- Publish ------------------------------------------------------------

    get canPublish() {
      const intent = Alpine.store('intent');
      return intent.hasIntent && !!intent.project && !Alpine.store('publish').running;
    },

    async publish() {
      if (!this.canPublish) return;
      await Alpine.store('publish').run();
    },

    /** The icon for a pipeline step's current state. */
    stepIcon(status) {
      return { done: 'circle-check', running: 'loader-circle', failed: 'circle-x' }[status] || 'circle';
    },

    logout() { window.location.replace('/logout'); },
  }));
}, { once: true });
