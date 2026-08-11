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
 * calendarEvents — rows to x-h-calendar events, and the date shapes around them.
 *
 * Every calendar surface in the Harmonia stack maps a controller's rows onto the SAME event shape by
 * the SAME conventions, and the interesting part of that mapping is the value normalization: Jackson
 * serializes java.time as arrays (LocalDate [y,m,d]; LocalDateTime [y,m,d,h,mi,s,ns]) and
 * Instant/Timestamp as a numeric epoch in SECONDS, while x-h-calendar wants ISO strings. This file is
 * the single implementation the document line-item calendars (power / personal / partner) share.
 *
 * Deliberately dependency-free (no Alpine, no window.App), like services/format.js, so any page or
 * standalone iframe can load it by absolute URL.
 *
 * `cal` is the calendar definition a composition child contributes through its detail registration
 * (detail-register.js, from the intent's `calendar:` block): { start, end?, title?, color?, view,
 * range }.
 */
(() => {

  const PALETTE = ['blue', 'green', 'purple', 'orange', 'teal', 'pink', 'indigo', 'yellow', 'red', 'gray'];

  const pad = (n) => String(n).padStart(2, '0');

  const HarmoniaCalendar = {

    /** Any Jackson java.time shape (array / epoch seconds / ISO string) as the ISO string; '' when unset. */
    toISO(v) {
      if (v === undefined || v === null || v === '') return '';
      if (Array.isArray(v)) {
        const date = v[0] + '-' + pad(v[1]) + '-' + pad(v[2]);
        if (v.length <= 3) return date;
        return date + 'T' + pad(v[3] || 0) + ':' + pad(v[4] || 0) + ':' + pad(v[5] || 0);
      }
      if (typeof v === 'number') {
        // Jackson serializes Instant/Timestamp as epoch SECONDS (with nanos); JS Date wants millis.
        const ms = v < 1e12 ? v * 1000 : v;
        try { return new Date(ms).toISOString(); } catch (e) { return ''; }
      }
      return String(v);
    },

    /** Whether the value carries a date only (no time), so its event is all-day. */
    isDateOnly(v) {
      return Array.isArray(v) ? v.length <= 3 : (typeof v === 'string' && v.length <= 10);
    },

    /** Deterministic categorical colour from the Harmonia calendar palette. */
    colorFor(v) {
      const key = (v === undefined || v === null) ? '' : String(v);
      let h = 0;
      for (let i = 0; i < key.length; i++) h = (h * 31 + key.charCodeAt(i)) >>> 0;
      return PALETTE[h % PALETTE.length];
    },

    /**
     * The rows as calendar events. `opts`: { primaryKey, label?, title? } - `title(row)` resolves the
     * event's own title (so a caller can run a FK through its own lookup map) and falls back to
     * "<label> #<id>". A row with no start value carries no position on a timeline and is skipped.
     */
    events(rows, cal, opts) {
      if (!cal || !cal.start) return [];
      const o = opts || {};
      return (rows || []).map((row) => {
        const start = this.toISO(row[cal.start]);
        if (!start) return null;
        const id = row[o.primaryKey];
        const title = o.title ? o.title(row) : null;
        const event = {
          id: String(id),
          title: (title === undefined || title === null || String(title) === '')
            ? ((o.label || 'Record') + ' #' + id) : String(title),
          start: start,
          allDay: cal.range ? true : this.isDateOnly(row[cal.start]),
        };
        if (cal.end) {
          const end = this.toISO(row[cal.end]);
          if (end) event.end = end;
        }
        if (cal.color) event.color = this.colorFor(row[cal.color]);
        return event;
      }).filter(Boolean);
    },

    /**
     * The clicked day (plus the slot time in week/day views) as the value a date / datetime-local input
     * expects, so a create draft can be prefilled from it; '' when the event carries no usable date.
     */
    clickedDateValue(event) {
      const d = event && event.detail ? event.detail.date : null;
      if (!(d instanceof Date) || isNaN(d.getTime())) return '';
      const value = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
      return event.detail.time ? value + 'T' + event.detail.time : value;
    },

    /** The clicked event's record id, or null when the click carried none. */
    clickedEventId(event) {
      const clicked = event && event.detail ? event.detail.event : null;
      return clicked && clicked.id !== undefined && clicked.id !== null ? clicked.id : null;
    },
  };

  window.HarmoniaCalendar = HarmoniaCalendar;
})();
