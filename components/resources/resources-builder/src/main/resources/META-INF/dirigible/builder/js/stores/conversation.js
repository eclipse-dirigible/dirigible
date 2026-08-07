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
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('conversation', {
    /** Display messages: { role: 'user' | 'assistant' | 'note' | 'error', text }. */
    messages: [],
    /** The clean transcript sent upstream: { role: 'user' | 'assistant', content }. */
    turns: [],
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

    /** The localStorage key holding this app's conversation (per project; a fresh app has its own). */
    storageKey() {
      return 'dirigible.builder.chat.' + (Alpine.store('intent').project || 'new');
    },

    /** Restore the conversation of the currently open app. */
    restore() {
      this.messages = [];
      this.turns = [];
      try {
        const raw = localStorage.getItem(this.storageKey());
        if (!raw) return;
        const saved = JSON.parse(raw);
        this.messages = Array.isArray(saved.messages) ? saved.messages : [];
        this.turns = Array.isArray(saved.turns) ? saved.turns : [];
      } catch (e) {
        console.error('builder: could not restore the conversation', e);
      }
    },

    /** Persist the conversation. The agent endpoint is stateless, so the browser is the only keeper. */
    persist() {
      try {
        localStorage.setItem(this.storageKey(), JSON.stringify({ messages: this.messages, turns: this.turns }));
      } catch (e) { /* storage full or unavailable - the conversation simply is not restored */ }
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
        this.persist();
      }

      if (reply.reply) {
        this.say('assistant', reply.reply);
        this.turns.push({ role: 'assistant', content: reply.reply });
      }

      if (reply.proposedYaml) await this.adopt(reply.proposedYaml);
      this.persist();
    },

    /**
     * Validate a proposal and, when it is sound, make it the app. An invalid proposal is NOT applied -
     * the server already spent its repair rounds on it, so the user is shown what is wrong and can
     * rephrase instead of ending up with a broken buffer.
     */
    async adopt(proposedYaml) {
      const intent = Alpine.store('intent');
      let model;
      try {
        model = await App.services.intentApi.parse(proposedYaml);
      } catch (e) {
        const issues = e.issues.length ? e.issues : ['The proposal could not be parsed.'];
        this.say('error', 'That change does not validate yet, so it was not applied:\n' + issues.map(i => '• ' + i).join('\n'));
        return;
      }
      const saved = await intent.apply(proposedYaml, model);
      if (!saved) this.say('error', intent.saveError);
      document.dispatchEvent(new CustomEvent('builder:model-changed'));
    },

    /** Drop the conversation of the current app (used when switching apps or starting a new one). */
    clear() {
      this.messages = [];
      this.turns = [];
      this.input = '';
    },
  });
}, { once: true });
