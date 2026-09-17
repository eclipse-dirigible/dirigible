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
 * format — the Harmonia stack's SINGLE source of value formatting (dates, datetimes, floating-point
 * numbers). Previously the same logic was hand-copied into ~12 page components; every one of them now
 * delegates here so the whole instance formats identically.
 *
 * The three display patterns are per-user preferences (Region & Language setting), persisted in
 * localStorage exactly like the language flag, with instance defaults that reproduce the historical
 * output (space-grouped 2-decimal numbers, ISO-ish dates). This file is deliberately self-contained
 * and dependency-free (no Alpine, no window.App): the standalone BPM task-form iframe loads it by
 * absolute URL and reads the same keys, so a form opened outside the shell formats the same way.
 *
 * DISPLAY formatting (HarmoniaFormat.number / .value) honours the patterns, and so does what a date
 * INPUT shows and accepts: HarmoniaFormat.pickerConfig() describes the Date pattern to the Harmonia
 * date pickers, which would otherwise display and parse in the document's (or the browser's) locale —
 * the one surface where guessing is not merely inconsistent but silently wrong, 06.09.2026 typed into
 * an m/d/yyyy field being a valid 9 June. The MODEL behind those inputs stays ISO regardless, which is
 * what HarmoniaFormat.toDateInput produces: an <input> model dictates its own value shape
 * (YYYY-MM-DD, ...THH:mm, HH:mm), so that helper is pattern-free; it lives here only to dedupe.
 *
 * Number rule (per the design decision): the DECIMAL COUNT and whether-to-group come from the field's
 * own DecimalFormat pattern (money scale 2 vs quantity scale 0 stay distinct); the GROUPING and DECIMAL
 * SEPARATORS come from the instance-wide Number pattern. So switching the Number setting to a European
 * "#.##0,00" renders "1.234,56" everywhere while each field keeps its authored precision.
 */
(() => {
  const KEYS = {
    date: 'codbex.harmonia.format.date',
    dateTime: 'codbex.harmonia.format.datetime',
    number: 'codbex.harmonia.format.number',
  };

  const SPACE = ' ';
  const NBSP = String.fromCharCode(0xA0);

  // Defaults reproduce the pre-unification output: space grouping, dot decimal, two fraction digits;
  // ISO-style dates. Date/datetime use DateTimeFormatter-style tokens; number uses DecimalFormat style.
  const DEFAULTS = {
    date: 'yyyy-MM-dd',
    dateTime: 'yyyy-MM-dd HH:mm:ss',
    number: '###' + SPACE + '###' + SPACE + '###' + SPACE + '##0.00',
  };

  const readKey = (k) => {
    try {
      return localStorage.getItem(KEYS[k]);
    } catch (e) {
      return null;
    }
  };

  // Current effective patterns (read live so a reload-free change or a parent-window edit is picked up).
  const patterns = () => ({
    date: readKey('date') || DEFAULTS.date,
    dateTime: readKey('dateTime') || DEFAULTS.dateTime,
    number: readKey('number') || DEFAULTS.number,
  });

  const pad = (n) => String(n).padStart(2, '0');

  // "2026-08-10 12:30" -> "2026-08-10T12:30": the single space separating date and time becomes the
  // ISO 'T', so both new Date() (WebKit rejects the space form) and 'T'-splitting consumers accept it.
  const isoT = (s) => (s.length > 10 && s[10] === ' ' ? s.slice(0, 10) + 'T' + s.slice(11) : s);

  const firstOf = (p, chars) => {
    for (const c of chars) {
      if (p.indexOf(c) >= 0) return c;
    }
    return '';
  };

  // Grouping + decimal separators from a DecimalFormat-style pattern. Two conventions are recognised:
  // a comma AFTER the last dot means the European "#.##0,00" (comma decimal, dot/space group); otherwise
  // the dot is the decimal and grouping is space > nbsp > comma if present.
  const separators = (pattern) => {
    const p = String(pattern);
    if (p.lastIndexOf(',') > p.lastIndexOf('.')) {
      return { groupSep: firstOf(p, ['.', SPACE, NBSP]), decimalSep: ',' };
    }
    return { groupSep: firstOf(p, [SPACE, NBSP, ',']), decimalSep: '.' };
  };

  // Fraction digit count declared by a field pattern (its decimal separator is always '.', as emitted
  // by the intent generator). No field pattern -> follow the instance Number pattern's own fraction.
  const fieldDecimals = (fieldPattern) => {
    if (fieldPattern) {
      const dot = String(fieldPattern).lastIndexOf('.');
      return dot >= 0 ? String(fieldPattern).length - dot - 1 : 0;
    }
    const gp = patterns().number;
    const ds = separators(gp).decimalSep;
    const i = gp.lastIndexOf(ds);
    const frac = i >= 0 ? gp.slice(i + 1) : '';
    return /^[0#]+$/.test(frac) ? frac.length : 0;
  };

  // Whether the field's integer part carries a grouping placeholder (so "0" / a plain integer pattern
  // is NOT grouped — keeps ids and years intact). No field pattern -> group (the shell default did).
  const groupingChars = [SPACE, NBSP, ','];
  const fieldGroups = (fieldPattern) => {
    if (!fieldPattern) return true;
    const intPart = String(fieldPattern).split('.')[0];
    return groupingChars.some((c) => intPart.indexOf(c) >= 0);
  };

  // Substitute DateTimeFormatter-style tokens (yyyy, yy, MM, M, dd, d, HH, H, mm, ss) with the value's
  // components. Longer tokens are replaced before their shorter prefixes; substituted values are digits,
  // so no token re-matches an already-substituted run.
  const applyDatePattern = (pattern, c) => String(pattern)
    .replace(/yyyy/g, c.y)
    .replace(/yy/g, pad(c.y % 100))
    .replace(/MM/g, pad(c.mo))
    .replace(/M/g, c.mo)
    .replace(/dd/g, pad(c.da))
    .replace(/d/g, c.da)
    .replace(/HH/g, pad(c.h))
    .replace(/H/g, c.h)
    .replace(/mm/g, pad(c.mi))
    .replace(/ss/g, pad(c.s));

  // An ISO-ish date / date-time string as a backend or a user may write it: "2026-09-06",
  // "2026-09-06T17:02:31", "2026-09-06 17:02". Its fields are read LITERALLY, as the wall clock they
  // spell; anything trailing (fractional seconds) is ignored - the components below are all the display
  // patterns can render. A string carrying a zone offset is NOT one of these: see ZONED.
  const ISO_LIKE = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/;

  // A zone offset closing the TIME component: "...T17:02:31Z", "...+00:00", "...-0500", "...+03".
  // Its presence means the string names an INSTANT rather than a wall clock, so its literal fields are
  // the offset's, not the viewer's: Jackson serializes a java.util.Date / Instant / Timestamp as
  // "2026-09-06T17:02:31.000+00:00", and reading that literally prints the UTC clock to a user three
  // zones away. Such a value is parsed and rendered from its LOCAL fields, like the epoch-number branch.
  const ZONED = /\d{2}:\d{2}(?::\d{2})?(?:[.,]\d+)?(?:Z|z|[+-]\d{2}(?::?\d{2})?)$/;

  // The local (viewer-zone) date-time components of a Date, in the shape applyDatePattern takes.
  const localComponents = (d) => ({
    y: d.getFullYear(), mo: d.getMonth() + 1, da: d.getDate(),
    h: d.getHours(), mi: d.getMinutes(), s: d.getSeconds(),
  });

  // Pattern letters that carry a date field, and the order codes the Harmonia picker's config takes.
  const DATE_FIELDS = { y: 'year', M: 'month', d: 'day' };
  const ORDER_CODES = { year: 'Y', month: 'M', day: 'D' };

  // Translate a DateTimeFormatter-style pattern into the { order, delimiter, options } config a
  // Harmonia date picker takes, so the picker DISPLAYS and PARSES exactly what the instance Date
  // setting says. Without it a picker follows the document language (or, absent one, the browser's),
  // which is how a form parsed 06.09.2026 as 9 June while the same instance printed dates elsewhere
  // day-first. A pattern that is not a plain three-field date with one repeated separator yields null,
  // and the picker keeps its own locale default rather than a half-applied shape.
  const pickerConfigFrom = (pattern) => {
    const p = String(pattern);
    const runs = [];
    let i = 0;
    while (i < p.length) {
      const field = DATE_FIELDS[p[i]];
      if (!field) { i++; continue; }
      let j = i;
      while (p[j] === p[i]) j++;
      runs.push({ field: field, len: j - i, start: i, end: j });
      i = j;
    }
    if (runs.length !== 3) return null;
    const fields = runs.map((r) => r.field);
    if (new Set(fields).size !== 3) return null;
    const delimiter = p.slice(runs[0].end, runs[1].start);
    if (!delimiter || delimiter !== p.slice(runs[1].end, runs[2].start)) return null;
    const options = {};
    runs.forEach((r) => {
      // A year is spelled out in full only as yyyy; a month/day is zero-padded only as MM/dd.
      options[r.field] = r.field === 'year'
        ? (r.len >= 4 ? 'numeric' : '2-digit')
        : (r.len >= 2 ? '2-digit' : 'numeric');
    });
    return { order: fields.map((f) => ORDER_CODES[f]).join(''), delimiter: delimiter, options: options };
  };

  const HarmoniaFormat = {
    KEYS,
    defaults() {
      return { ...DEFAULTS };
    },
    patterns,

    /**
     * Format a floating-point value for display. Decimals + whether-to-group come from the field
     * pattern; the separators come from the instance Number pattern. Empty -> emptyText (default '—');
     * a non-finite value passes through unchanged.
     */
    number(value, fieldPattern, emptyText) {
      const empty = emptyText === undefined ? '—' : emptyText;
      if (value === null || value === undefined || value === '') return empty;
      const n = Number(value);
      if (!isFinite(n)) return value;
      const decimals = fieldDecimals(fieldPattern);
      const { groupSep, decimalSep } = separators(patterns().number);
      const parts = n.toFixed(decimals).split('.');
      if (groupSep && fieldGroups(fieldPattern)) {
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, groupSep);
      }
      return parts.length > 1 ? parts[0] + decimalSep + parts[1] : parts[0];
    },

    /**
     * Format a date/datetime value for display using the instance Date / Timestamp patterns. Jackson
     * serializes java.time as arrays (LocalDate [y,m,d], LocalDateTime [y,m,d,h,mi,s,ns]), an
     * Instant/Timestamp as an epoch number, and a java.util.Date as an ISO string with a zone offset;
     * all three shapes are recognised. An offset-carrying string is converted to the viewer's zone (an
     * offset-less one is a wall clock and is read literally). Empty -> '—'; an unrecognised value
     * (e.g. an already-formatted string) passes through unchanged.
     */
    value(v, isDate) {
      if (v === null || v === undefined || v === '') return '—';
      const p = patterns();
      if (Array.isArray(v)) {
        if (v.length === 3) return applyDatePattern(p.date, { y: v[0], mo: v[1], da: v[2], h: 0, mi: 0, s: 0 });
        if (v.length >= 5) return applyDatePattern(p.dateTime, { y: v[0], mo: v[1], da: v[2], h: v[3], mi: v[4], s: v[5] || 0 });
        return v.join(', ');
      }
      if (isDate && (typeof v === 'number' || v instanceof Date)) {
        const d = v instanceof Date ? v : new Date(v < 1e11 ? v * 1000 : v); // epoch seconds or millis
        if (!isNaN(d.getTime())) return applyDatePattern(p.dateTime, localComponents(d));
      }
      if (isDate && typeof v === 'string') {
        // An offset-carrying string is an instant: convert to the viewer's zone before formatting.
        if (ZONED.test(v)) {
          const d = new Date(v);
          if (!isNaN(d.getTime())) return applyDatePattern(p.dateTime, localComponents(d));
        }
        const m = ISO_LIKE.exec(v);
        if (m) {
          const c = { y: +m[1], mo: +m[2], da: +m[3], h: +(m[4] || 0), mi: +(m[5] || 0), s: +(m[6] || 0) };
          return applyDatePattern(m[4] === undefined ? p.date : p.dateTime, c);
        }
      }
      return v;
    },

    /**
     * DISPLAY-format a date/date-time value against the active date pattern. Convenience alias for
     * value(v, true) - a backend LocalDate array, epoch number, or ISO string renders as a date.
     */
    date(v) {
      return this.value(v, true);
    },

    /**
     * A document number for display. A not-yet-issued document holds a create-time UUID placeholder in
     * its number field (the platform's numbering places it there until the real number is stamped at
     * issue); render that as empty so the title shows just the document label ("Sales Invoice", not the
     * raw UUID). Once the real number is stamped it passes through unchanged, as does any non-UUID value.
     */
    documentNumber(v) {
      if (v === null || v === undefined) return '';
      const uuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
      return uuid.test(String(v)) ? '' : v;
    },

    /**
     * The active Date pattern as an input hint ("dd.mm.yyyy" for dd.MM.yyyy): the same pattern, lower
     * cased, which is how a date placeholder is conventionally spelled. Shown in the date inputs so the
     * expected field order is visible before anything is typed.
     */
    dateHint() {
      return patterns().date.toLowerCase();
    },

    /**
     * The { order, delimiter, options } configuration a Harmonia date / date-time picker popup takes,
     * derived from the instance Date (or Timestamp) pattern. `kind` is 'date' (default) or 'dateTime';
     * only the date half of a Timestamp pattern is described, the time segments being the picker's own.
     * A pattern the picker cannot be configured from yields an empty config (its locale default).
     */
    pickerConfig(kind) {
      const p = patterns();
      return pickerConfigFrom(kind === 'dateTime' ? p.dateTime : p.date) || {};
    },

    /**
     * Convert a date/datetime value to the FIXED shape an HTML <input> requires — NOT pattern-driven.
     * `widget` is one of DATE, DATETIME-LOCAL, TIME, MONTH, WEEK (case-insensitive). Empty -> ''.
     * An input holds a LOCAL wall clock, so an offset-carrying string is converted to the viewer's zone.
     * MONTH (YYYY-MM) and WEEK (YYYY-Www) are stored as plain strings, so they slice through unchanged.
     */
    toDateInput(value, widget) {
      if (value === null || value === undefined || value === '') return '';
      const w = String(widget || 'DATE').toUpperCase();
      // WEEK (YYYY-Www) has no calendar-date form, so it is always a stored string.
      if (w === 'WEEK') return String(value).slice(0, 8);
      let y, mo = 1, da = 1, h = 0, mi = 0;
      if (Array.isArray(value)) {
        y = value[0]; mo = value[1] || 1; da = value[2] || 1; h = value[3] || 0; mi = value[4] || 0;
      } else if (typeof value === 'number') {
        const d = new Date(value < 1e11 ? value * 1000 : value);
        if (isNaN(d.getTime())) return '';
        y = d.getFullYear(); mo = d.getMonth() + 1; da = d.getDate(); h = d.getHours(); mi = d.getMinutes();
      } else if (ZONED.test(String(value))) {
        // An offset-carrying string is an instant, and an <input> holds a LOCAL wall clock: slicing the
        // raw string would seed the field with the offset's clock, which toPayload then reads back as a
        // different instant. Derive the local components instead, as the number branch above does.
        const d = new Date(String(value));
        if (isNaN(d.getTime())) return '';
        y = d.getFullYear(); mo = d.getMonth() + 1; da = d.getDate(); h = d.getHours(); mi = d.getMinutes();
      } else {
        // A backend/user string may separate date and time with a space ("2026-08-10 12:30:00");
        // normalize to the ISO 'T' so the slice below yields a value the datetime input (and the
        // picker's 'T'-splitting seed logic) accepts.
        const s = isoT(String(value));
        switch (w) {
          case 'DATETIME-LOCAL': return s.slice(0, 16);
          case 'TIME': return s.slice(0, 5);
          case 'MONTH': return s.slice(0, 7);
          default: return s.slice(0, 10);
        }
      }
      const date = y + '-' + pad(mo) + '-' + pad(da);
      switch (w) {
        case 'DATETIME-LOCAL': return date + 'T' + pad(h) + ':' + pad(mi);
        case 'TIME': return pad(h) + ':' + pad(mi);
        case 'MONTH': return y + '-' + pad(mo);
        default: return date;
      }
    },

    /**
     * Inverse of toDateInput: convert an HTML <input> value to the payload value the backend expects.
     * DATE / DATETIME-LOCAL -> a full ISO instant (…Z) so a Jackson java.time.* field binds;
     * TIME / MONTH / WEEK -> pass through unchanged (they are stored as plain strings, not instants);
     * empty -> null. An unparseable value is returned unchanged.
     * `widget` is one of DATE, DATETIME-LOCAL, TIME, MONTH, WEEK (case-insensitive).
     */
    toPayload(value, widget) {
      if (value === null || value === undefined || value === '') return null;
      const w = String(widget || 'DATE').toUpperCase();
      if (w === 'TIME' || w === 'MONTH' || w === 'WEEK') return value;
      // Normalize a space-separated date-time before parsing: WebKit's new Date() rejects
      // "2026-08-10 12:30" (a shape a user can type), which would pass the raw string through.
      const d = new Date(typeof value === 'string' ? isoT(value) : value);
      return isNaN(d.getTime()) ? value : d.toISOString();
    },
  };

  window.HarmoniaFormat = HarmoniaFormat;

  // The Region & Language editing surface. Values mirror localStorage; a change persists and reloads
  // (like the language flag) so every already-rendered cell re-formats. Absent Alpine (standalone
  // form iframe) the store is simply never registered — the pure helpers above still work.
  if (typeof document !== 'undefined' && document.addEventListener) {
    document.addEventListener('alpine:init', () => {
      Alpine.store('formats', {
        date: DEFAULTS.date,
        dateTime: DEFAULTS.dateTime,
        number: DEFAULTS.number,

        init() {
          const d = readKey('date'); if (d) this.date = d;
          const dt = readKey('dateTime'); if (dt) this.dateTime = dt;
          const n = readKey('number'); if (n) this.number = n;
        },

        defaults() {
          return { ...DEFAULTS };
        },

        // Persist the three edited patterns (blank -> the instance default) and reload once so every
        // already-rendered cell re-formats at once — the same reload-to-apply the language flag uses.
        apply() {
          Object.keys(KEYS).forEach((k) => {
            const value = (this[k] || '').trim() || DEFAULTS[k];
            this[k] = value;
            try { localStorage.setItem(KEYS[k], value); } catch (e) { /* storage unavailable */ }
          });
          window.location.reload();
        },

        // Restore all three to the instance defaults.
        reset() {
          Object.keys(KEYS).forEach((k) => {
            this[k] = DEFAULTS[k];
            try { localStorage.removeItem(KEYS[k]); } catch (e) { /* storage unavailable */ }
          });
          window.location.reload();
        },
      });
    }, { once: true });
  }
})();
