/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The conversation with the AI assistant, and the auto-apply loop that turns it into an app.
 *
 * Two lists, the same discipline as the Intent Editor's chat pane: `messages` is what the user sees
 * (it may hold notes and errors), while `turns` is the clean alternating user/assistant transcript
 * the model API requires - a failed call pops its dangling user turn so the next request stays valid.
 *
 * The agent endpoint answers 200 even when its proposal STILL fails validation (the server's bounded
 * self-correction gave up), signalling it only as trailing prose in `reply`. So every proposal is
 * re-validated here through /parse before it is allowed anywhere near the buffer.
 *
 * The conversation is persisted SERVER-side, in a tenant-aware table, keyed by app + surface: it is the
 * record of why an application looks the way it does, so it has to survive a different browser, a
 * cleared profile and a teammate opening the same app - and it is the only audit trail linking an
 * assistant proposal to the request that produced it. Persistence is append-only and never blocking: a
 * failed save leaves the turn pending so the next one carries it, and the chat itself keeps working.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('conversation', {
    /** Display messages: { role: 'user' | 'assistant' | 'note' | 'error', text }. */
    messages: [],
    /** The clean transcript sent upstream: { role: 'user' | 'assistant', content }. */
    turns: [],
    /** How many of `messages` the server has accepted. Everything past it is still unsaved. */
    persistedCount: 0,
    input: '',
    busy: false,
    /** Set when the assistant is not configured (412) - a persistent banner, not a transient bubble. */
    notConfigured: false,
    /** Rotating wording under the typing indicator, so a multi-minute turn does not look hung. */
    phase: '',

    _phaseTimer: null,

    PHASES: [
      'Reading your description...',
      'Shaping the entities...',
      'Working out the relations...',
      'Designing the processes...',
      'Adding the forms and reports...',
      'Checking it all fits together...',
      'Almost there...',
    ],

    /**
     * Ask - once, on load - whether the assistant is usable, so an unconfigured instance says so
     * before the user types a message that can only fail. The check is configuration-only
     * server-side; it never costs an upstream model call.
     */
    async probeConfiguration() {
      this.notConfigured = !(await App.services.intentApi.agentConfigured());
    },

    /** Restore the conversation of the currently open app from the server. */
    async restore() {
      this.messages = [];
      this.turns = [];
      this.persistedCount = 0;
      const project = Alpine.store('intent').project;
      // A brand-new app has no project to key a conversation on yet, so there is nothing to restore.
      if (!project) return;
      let stored;
      try {
        stored = await App.services.intentApi.conversation(project);
      } catch (e) {
        // The history is unavailable; the chat still works, it just starts from a blank pane.
        console.error('builder: could not restore the conversation', e);
        return;
      }
      this.messages = stored.messages.map(m => ({ role: m.role, text: m.content }));
      // The transcript is DERIVED from the stored roles rather than stored a second time, so the two
      // lists cannot drift - and it is derived SERVER-side, because "which messages may be replayed"
      // is a property of the roles: a failed turn keeps the message that was sent (support needs it)
      // but replaying that unanswered turn would break the model API's alternation.
      this.turns = stored.turns.map(t => ({ role: t.role, content: t.content }));
      this.persistedCount = this.messages.length;
    },

    /**
     * Append everything this app's conversation has said but not yet saved. Called once per turn, after
     * the turn is fully resolved - including a failed one, whose error bubble is part of the record.
     *
     * A failure is deliberately silent apart from the console: the count is left where it is, so the
     * next turn re-sends this tail and a transient outage costs nothing.
     */
    async flush() {
      const project = Alpine.store('intent').project;
      // The very first turn happens before the project exists (it is created by the proposal that
      // follows). Its messages stay pending and are carried by the next flush.
      if (!project) return;
      const pending = this.messages.slice(this.persistedCount);
      if (!pending.length) return;
      try {
        await App.services.intentApi.appendConversation(project, pending.map(m => ({ role: m.role, content: m.text })));
        // Advance by what was actually sent, not to the current length - a message the user typed while
        // this call was in flight has not been saved yet.
        this.persistedCount += pending.length;
      } catch (e) {
        console.error('builder: could not save the conversation', e);
      }
    },

    startPhases() {
      let index = 0;
      this.phase = this.PHASES[0];
      this._phaseTimer = setInterval(() => {
        index = Math.min(index + 1, this.PHASES.length - 1);
        this.phase = this.PHASES[index];
      }, 6000);
    },

    stopPhases() {
      if (this._phaseTimer) clearInterval(this._phaseTimer);
      this._phaseTimer = null;
      this.phase = '';
    },

    say(role, text) {
      this.messages.push({ role, text });
      // Let the DOM grow before scrolling, so the newest bubble is the one in view.
      setTimeout(() => {
        const list = document.getElementById('builder-messages');
        if (list) list.scrollTop = list.scrollHeight;
      }, 0);
    },

    /** Send one turn: ask the assistant, validate whatever it proposes, and apply it when it is sound. */
    async send(message) {
      const text = (message || '').trim();
      if (!text || this.busy) return;
      const intent = Alpine.store('intent');
      const history = this.turns.slice();

      this.say('user', text);
      this.turns.push({ role: 'user', content: text });
      this.input = '';
      this.busy = true;
      this.startPhases();

      // The whole turn is bracketed so the conversation is saved exactly once, at the end - by which
      // point an applied proposal has created the project the conversation is keyed on.
      try {
        let reply;
        try {
          reply = await App.services.intentApi.agent(intent.yaml, text, history);
          this.notConfigured = false;
        } catch (e) {
          // The turn never completed - drop its unanswered user turn so the transcript stays alternating.
          this.turns.pop();
          if (e.status === 412) {
            this.notConfigured = true;
            this.say('error', 'The AI assistant is not configured on this instance. Set DIRIGIBLE_INTENT_AI_API_KEY and reload.');
          } else if (e.status === 0) {
            this.say('error', 'The assistant did not answer in time. Please try again.');
          } else {
            this.say('error', 'The assistant could not be reached. Please try again.');
          }
          return;
        } finally {
          this.busy = false;
          this.stopPhases();
        }

        if (reply.reply) {
          this.say('assistant', reply.reply);
          this.turns.push({ role: 'assistant', content: reply.reply });
        }

        // What the model could NOT express, said separately from what it did. As part of the reply
        // prose this reads like the rest of the answer and is skimmed past - which is how a "the
        // system identifies the driver" requirement became "an officer identifies the driver" with
        // nobody noticing. It is stored as its own message, so it is in the record too.
        this.reportBoundaries(reply.boundaries);

        if (reply.proposedYaml) await this.adopt(reply.proposedYaml);
      } finally {
        await this.flush();
      }
    },

    /**
     * Render each reported boundary as its own message: the requirement, why the model of the
     * application does not carry it, and what has to be hand-written instead. The text is deliberately
     * self-contained so it can be forwarded verbatim - a boundary somebody actually hit is the only
     * reliable signal of which missing capability is worth building.
     */
    reportBoundaries(boundaries) {
      for (const boundary of boundaries || []) {
        if (!boundary || !boundary.requirement) continue;
        const lines = ['Not expressible in the model: ' + boundary.requirement];
        if (boundary.explanation) lines.push(boundary.explanation);
        if (boundary.extensionKind && boundary.extensionKind !== 'none') {
          lines.push('Carried by: ' + boundary.extensionKind
            + (boundary.suggestedClass ? ' (' + boundary.suggestedClass + ', to be written by hand)' : ''));
        }
        this.say('boundary', lines.join('\n'));
      }
    },

    /**
     * Validate a proposal and, when it is sound, make it the app. An invalid proposal is NOT applied -
     * the server already spent its repair rounds on it, so the user is shown what is wrong and can
     * rephrase instead of ending up with a broken buffer.
     *
     * The check is the full pre-flight, not just a parse (dirigible #6956): a proposal that parses
     * but whose generation would drop a piece of the model, or be refused by a generation-time check,
     * is a model that LOOKS finished and then generates wrong - the worst thing this gate could
     * accept. The current project (when it already exists) is passed so the server judges the
     * proposal against the real `.settings` and cross-model dependencies.
     */
    async adopt(proposedYaml) {
      const intent = Alpine.store('intent');
      let model;
      try {
        const verdict = await App.services.intentApi.validate(proposedYaml, intent.project || undefined);
        const issues = (verdict && Array.isArray(verdict.issues)) ? verdict.issues : [];
        if (issues.length) {
          this.say('error', 'That change parses, but generating it would not produce what it says, so it was not applied:\n'
            + issues.map(i => '• ' + i).join('\n'));
          return;
        }
        model = verdict.model;
      } catch (e) {
        const issues = e.issues && e.issues.length ? e.issues : ['The proposal could not be parsed.'];
        this.say('error', 'That change does not validate yet, so it was not applied:\n' + issues.map(i => '• ' + i).join('\n'));
        return;
      }
      const saved = await intent.apply(proposedYaml, model);
      if (!saved) this.say('error', intent.saveError);
      document.dispatchEvent(new CustomEvent('builder:model-changed'));
    },

    /**
     * Forget the conversation currently on screen (used when switching apps or starting a new one).
     * Nothing stored is touched - the history of an app is append-only and outlives every session.
     */
    clear() {
      this.messages = [];
      this.turns = [];
      this.persistedCount = 0;
      this.input = '';
    },
  });
}, { once: true });
