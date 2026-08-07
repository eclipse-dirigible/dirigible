/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * State behind the Logs page: the live log stream from the platform's console socket - the only
 * live surface in the product - and the log files on disk. Changing log LEVELS is deliberately not
 * here; that is management with real blast radius and stays in the Workbench.
 */
document.addEventListener('alpine:init', () => {
  /** The socket the platform's logging appender broadcasts to (the IDE Console's own source). */
  const CONSOLE_SOCKET = '/websockets/ide/console';

  /** How many records the live view keeps. A log stream is unbounded; the browser is not. */
  const MAX_RECORDS = 2000;

  /** How much of a log file is shown initially, and how much each "load more" adds. */
  const TAIL_CHARS = 64 * 1024;

  Alpine.store('logs', {
    // Live stream
    records: [],
    connected: false,
    paused: false,
    levels: { ERROR: true, WARN: true, INFO: true, DEBUG: false, TRACE: false },
    search: '',

    // Files
    files: [],
    filesLoading: false,
    selectedFile: '',
    fileContent: '',
    fileLoading: false,
    fileError: null,
    /** How much of the loaded file's tail is currently shown. */
    shownChars: TAIL_CHARS,

    _socket: null,

    /** Open the console socket. Idempotent - the page may be entered more than once. */
    connect() {
      if (this._socket) return;
      const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
      try {
        this._socket = new WebSocket(protocol + window.location.host + CONSOLE_SOCKET);
      } catch (e) {
        console.error('monitoring: could not open the console socket', e);
        return;
      }
      this._socket.onopen = () => { this.connected = true; };
      this._socket.onclose = () => { this.connected = false; this._socket = null; };
      this._socket.onerror = (e) => {
        console.error('monitoring: the console socket failed', e);
        this.connected = false;
      };
      this._socket.onmessage = (message) => {
        if (this.paused || typeof message.data !== 'string') return;
        try {
          const record = JSON.parse(message.data);
          this.records.push(record);
          if (this.records.length > MAX_RECORDS) this.records.splice(0, this.records.length - MAX_RECORDS);
        } catch (e) {
          console.error('monitoring: unreadable log record', e);
        }
      };
    },

    disconnect() {
      if (!this._socket) return;
      const socket = this._socket;
      this._socket = null;
      this.connected = false;
      socket.close();
    },

    clear() {
      this.records = [];
    },

    togglePause() {
      this.paused = !this.paused;
    },

    toggleLevel(level) {
      this.levels[level] = !this.levels[level];
    },

    /** The records the filters let through, newest last (the order they arrived in). */
    get visibleRecords() {
      const needle = this.search.trim()
                         .toLowerCase();
      return this.records.filter((record) => this.levels[record.level]
        && (!needle || (record.message || '').toLowerCase()
                                             .includes(needle)));
    },

    async loadFiles() {
      this.filesLoading = true;
      try {
        this.files = await window.MonitoringOps.logFiles();
      } catch (e) {
        this.files = [];
        console.error('monitoring: could not list the log files', e);
      } finally {
        this.filesLoading = false;
      }
    },

    /** Load a whole log file once; the view shows its tail. The endpoint has no range support, so
     *  it must not be polled - a refresh is an explicit re-read. */
    async selectFile(file) {
      this.selectedFile = file;
      this.fileLoading = true;
      this.fileError = null;
      this.shownChars = TAIL_CHARS;
      try {
        const content = await window.MonitoringOps.logFile(file);
        this.fileContent = typeof content === 'string' ? content : JSON.stringify(content);
      } catch (e) {
        this.fileContent = '';
        this.fileError = e;
        console.error('monitoring: could not read the log file ' + file, e);
      } finally {
        this.fileLoading = false;
      }
    },

    /** The tail currently shown, and whether there is more above it. */
    get fileTail() {
      return this.fileContent.length > this.shownChars
        ? this.fileContent.slice(this.fileContent.length - this.shownChars)
        : this.fileContent;
    },

    get hasMoreAbove() {
      return this.fileContent.length > this.shownChars;
    },

    showMore() {
      this.shownChars += TAIL_CHARS;
    },
  });
});
