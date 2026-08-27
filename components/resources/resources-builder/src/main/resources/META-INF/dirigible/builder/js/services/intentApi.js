/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Builder's whole server surface, in one place. Everything here already ships - the Builder adds
 * no backend of its own; it orchestrates the intent engine, the workspace, the publisher and the
 * platform's health/problems feeds from the browser.
 *
 * It deliberately does NOT go through the shared `App.services.api` fetch client: `/parse` takes
 * text/plain, `/agent` needs a multi-minute timeout, and 422 carries a body that must be read rather
 * than thrown away - all of which would mean bending that client out of shape.
 */
(() => {

  const INTENT_BASE = '/services/ide/intent';
  const WORKSPACES = '/services/ide/workspaces';
  const PUBLISHER = '/services/ide/publisher';
  const PROBLEMS = '/services/ide/problems';
  const HEALTHCHECK = '/services/core/healthcheck';
  const SHELLS = '/services/js/platform-core/extension-services/shells.js?extensionPoints=platform-shells';
  const PERSPECTIVES = '/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-perspectives';

  // The agent endpoint makes up to three upstream model calls (the first draft plus two server-side
  // repair rounds), each of them streamed and each reasoning over the COMPLETE app.intent it has to
  // re-emit - so there is no longer a fixed per-call ceiling to multiply, and the server's own outer
  // bound is 10 minutes per call. This is deliberately generous: aborting here reports a turn that is
  // still being generated as a failure, and the user has no way to tell the two apart.
  const AGENT_TIMEOUT_MS = 20 * 60 * 1000;
  const DEFAULT_TIMEOUT_MS = 60 * 1000;

  /** A failed call, carrying the status and the parsed body so callers can act on 412 / 422 / 502. */
  class BuilderHttpError extends Error {
    constructor(status, body, url) {
      super(`${status} ${url}`);
      this.name = 'BuilderHttpError';
      this.status = status;      // 0 == transport failure or timeout
      this.body = body;          // parsed JSON when the response carried one, else the raw text
    }

    /** The validation issues of a 422, or an empty list. */
    get issues() {
      return (this.body && Array.isArray(this.body.issues)) ? this.body.issues : [];
    }
  }

  const ws = () => App.config.workspace;
  const seg = encodeURIComponent;

  // A conversation is identified by the app and the surface it was held in - never by the workspace or
  // the user, so a teammate opening the same app on another machine restores the same dialogue.
  const CONVERSATION_SURFACE = 'builder';
  const conversationQuery = (project) =>
    `project=${seg(project)}&surface=${CONVERSATION_SURFACE}&path=${seg(App.config.intentFile)}`;

  /** Encode a file path segment by segment - the slashes are structural and must survive. */
  const encodePath = (path) => String(path).split('/').map(seg).join('/');

  async function call(method, url, options = {}) {
    // `alsoOk` lists non-2xx statuses that are still a success for this call - the idempotent
    // creates answer 304 NOT MODIFIED when the thing is already there, which `response.ok` (2xx
    // only) would otherwise report as a failure.
    const { body, contentType, timeoutMs = DEFAULT_TIMEOUT_MS, alsoOk = [] } = options;
    const headers = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
    if (contentType) headers['Content-Type'] = contentType;

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    let response;
    try {
      response = await fetch(url, { method, headers, body, credentials: 'same-origin', signal: controller.signal });
    } catch (e) {
      // An abort is our own timeout firing; anything else is a transport failure.
      const message = e.name === 'AbortError' ? 'The request timed out' : e.message;
      throw new BuilderHttpError(0, { message }, url);
    } finally {
      clearTimeout(timer);
    }

    const text = await response.text().catch(() => '');
    let parsed = text;
    if (text) {
      try { parsed = JSON.parse(text); } catch (e) { /* a non-JSON body stays raw text */ }
    }
    if (!response.ok && !alsoOk.includes(response.status)) throw new BuilderHttpError(response.status, parsed, url);
    return { status: response.status, data: text ? parsed : null };
  }

  App.services.BuilderHttpError = BuilderHttpError;

  App.services.intentApi = {

    // ----- Intent engine ------------------------------------------------------

    /**
     * Parse and validate intent YAML. Returns the structured model; throws a BuilderHttpError whose
     * `issues` holds the validation problems on 422.
     */
    async parse(yaml) {
      const result = await call('POST', `${INTENT_BASE}/parse`, { body: yaml || '', contentType: 'text/plain' });
      return result.data;
    },

    /**
     * Whether the AI assistant is usable on this instance. Configuration-only server-side, so the
     * shell can say up front that it is unavailable without spending an upstream model call to find
     * out. An unreachable status endpoint is treated as configured - the first real turn then
     * reports the truth, which is better than a false "not configured" banner over a working shell.
     */
    async agentConfigured() {
      try {
        const result = await call('GET', `${INTENT_BASE}/agent/status`);
        return !result.data || result.data.configured !== false;
      } catch (e) {
        console.error('builder: could not read the assistant status', e);
        return true;
      }
    },

    /**
     * One conversation turn. `history` must be the clean alternating user/assistant transcript.
     * Returns `{reply, proposedYaml, boundaries}` - note the endpoint answers 200 even when the
     * proposal is still invalid, so callers MUST re-validate the proposal through parse() before
     * applying it. `boundaries` lists the requirements the proposal could NOT express; they are part
     * of the answer, not a footnote, and must be shown as such.
     */
    async agent(yaml, message, history) {
      const result = await call('POST', `${INTENT_BASE}/agent`, {
        body: JSON.stringify({ yaml: yaml || '', message, history: history || [] }),
        contentType: 'application/json',
        timeoutMs: AGENT_TIMEOUT_MS,
      });
      return result.data || {};
    },

    /**
     * Generate the derived model files AND the code from them - one call, since the intent engine runs
     * the model-to-code recipes itself. Reads the intent FROM DISK, so the buffer must be saved first.
     * Returns `{written, scrubbed, codeGenerations, warnings}`, where each `codeGenerations` entry
     * carries its own outcome (`generated`, plus `error` when it failed); throws with `issues` on 422.
     */
    async generate(project, path) {
      const url = `${INTENT_BASE}/generate?workspace=${seg(ws())}&project=${seg(project)}&path=${seg(path)}`;
      // One call now covers the models AND every model-to-code recipe, so it is allowed the time the
      // whole chain used to have across its several calls.
      const result = await call('POST', url, { timeoutMs: 10 * 60 * 1000 });
      return result.data || {};
    },

    // ----- Conversation history -----------------------------------------------

    /**
     * The stored conversation of one app: `{messages, turns}` - everything that was said
     * (`[{sequence, role, content, createdBy, createdAt}]`) plus the alternating user/assistant
     * transcript to send upstream on the next turn, which the SERVER derives (a failed turn's
     * unanswered message is kept in the record but must never be replayed). Both are empty when
     * nothing has been said about the app yet - the endpoint answers 200 with empty lists rather than
     * 404, so "no history" is never an error path.
     */
    async conversation(project) {
      const result = await call('GET', `${INTENT_BASE}/conversations?${conversationQuery(project)}`);
      const data = result.data || {};
      return { messages: data.messages || [], turns: data.turns || [] };
    },

    /**
     * Append the messages of one completed turn, as `[{role, content}]`. Starts the conversation when
     * this is its first message; the server stamps the author and the time.
     */
    async appendConversation(project, messages) {
      await call('POST', `${INTENT_BASE}/conversations/messages?${conversationQuery(project)}`, {
        body: JSON.stringify({ messages }),
        contentType: 'application/json',
      });
    },

    // ----- Workspace ----------------------------------------------------------

    /**
     * Create the project unless it already exists (201 created / 304 not modified - both fine).
     * The WORKSPACE is ensured first: on an instance where the user has never opened the IDE it does
     * not exist yet, and creating a project inside a missing workspace answers 404 - which would make
     * the very first app the Builder ever saves fail.
     */
    async ensureProject(project) {
      await call('POST', `${WORKSPACES}/${seg(ws())}`, { alsoOk: [304] });
      await call('POST', `${WORKSPACES}/${seg(ws())}/${seg(project)}`, { alsoOk: [304] });
    },

    /** The file's content, or null when it does not exist. */
    async readFile(project, path) {
      try {
        const result = await call('GET', `${WORKSPACES}/${seg(ws())}/${seg(project)}/${encodePath(path)}`);
        // A YAML body is not JSON, so `data` is the raw text; a file that happens to parse as JSON
        // would come back parsed, hence the defensive stringify.
        return typeof result.data === 'string' ? result.data : JSON.stringify(result.data);
      } catch (e) {
        if (e.status === 404) return null;
        throw e;
      }
    },

    /**
     * Write the file, creating it when absent. POST rejects an existing file with 400 and PUT expects
     * one, so the existence probe decides which verb to use.
     */
    async writeFile(project, path, content) {
      const url = `${WORKSPACES}/${seg(ws())}/${seg(project)}/${encodePath(path)}`;
      const exists = (await this.readFile(project, path)) !== null;
      await call(exists ? 'PUT' : 'POST', url, { body: content, contentType: 'text/plain' });
    },

    /** The names of the workspace projects that carry a root intent file - the Builder's own apps. */
    async listIntentProjects() {
      const intentFile = App.config.intentFile;
      try {
        const result = await call('GET', `${WORKSPACES}/${seg(ws())}`);
        const projects = (result.data && result.data.projects) || [];
        return projects
          .filter(p => (p.files || []).some(f => f && f.name === intentFile))
          .map(p => p.name)
          .sort((a, b) => a.localeCompare(b));
      } catch (e) {
        // A workspace that does not exist yet simply has no apps.
        if (e.status === 404) return [];
        throw e;
      }
    },

    // ----- Publish + verification ---------------------------------------------

    /** Publish the whole project into the registry. Synchronous: the 200 means the publish finished. */
    async publish(project) {
      await call('POST', `${PUBLISHER}/${seg(ws())}/${seg(project)}/`, { timeoutMs: 5 * 60 * 1000 });
    },

    /** Every recorded problem. Synchronizer failures (compile errors, failed seeds) land here. */
    async problems() {
      const result = await call('GET', PROBLEMS);
      return Array.isArray(result.data) ? result.data : [];
    },

    /** The platform health status: `Ready`, `Running` or `NotReady`. */
    async health() {
      const result = await call('GET', HEALTHCHECK);
      return (result.data && result.data.status) || '';
    },

    /** The registered shells, so the success panel links to real destinations instead of hardcoded ones. */
    async shells() {
      const result = await call('GET', SHELLS);
      const data = result.data;
      const shells = Array.isArray(data) ? data : (data && data.shells) || [];
      return shells.filter(s => s && s.path);
    },

    /**
     * The published app's own entry URL, DISCOVERED from the perspectives the generated app
     * contributes - never assembled from a guessed gen-folder convention, which would 404 the moment
     * a template changed its layout. Returns null while nothing is published for the project yet.
     */
    async appEntryUrl(project) {
      const prefix = `/services/web/${project}/`;
      try {
        const result = await call('GET', PERSPECTIVES);
        const groups = (result.data && result.data.perspectives) || [];
        const items = groups.flatMap(g => (Array.isArray(g.items) ? g.items : [g]));
        const match = items.find(i => i && typeof i.path === 'string' && i.path.startsWith(prefix));
        return match ? match.path : null;
      } catch (e) {
        console.error('builder: could not resolve the published app URL', e);
        return null;
      }
    },
  };
})();
