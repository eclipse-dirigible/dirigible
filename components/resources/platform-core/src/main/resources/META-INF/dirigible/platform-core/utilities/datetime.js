/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
class DateTimeUtil {
    /**
     * DateTime utility for formatting and getting relative time.
     * Uses native Intl APIs for localization.
     * @param {string} [locale] - Optional locale (e.g. "en", "bg", "de"). Defaults to runtime locale.
     */
    constructor(locale = undefined) {
        this.locale = locale;

        // Cache Intl formatters for performance
        this._dtf = {
            default: new Intl.DateTimeFormat(this.locale),
            monthLong: new Intl.DateTimeFormat(this.locale, { month: "long" }),
            monthShort: new Intl.DateTimeFormat(this.locale, { month: "short" }),
            weekdayLong: new Intl.DateTimeFormat(this.locale, { weekday: "long" }),
            weekdayShort: new Intl.DateTimeFormat(this.locale, { weekday: "short" }),
            dayPeriod: new Intl.DateTimeFormat(this.locale, { hour: "numeric", hour12: true })
        };

        this._rtf = new Intl.RelativeTimeFormat(this.locale, { numeric: "auto" });
    }

    /**
     * Convert input into a valid Date object.
     * @private
     * @param {string|Date|number} input - ISO string, Date object, or timestamp.
     * @returns {Date}
     * @throws {Error} If the date is invalid.
     */
    toDate(input) {
        const date = input instanceof Date ? input : new Date(input);
        if (isNaN(date)) throw new Error("Invalid date");
        return date;
    }

    /**
     * Pad a number with leading zeros.
     * @private
     * @param {number} num
     * @returns {string}
     */
    pad(num) {
        return String(num).padStart(2, "0");
    }

    /**
     * Format a date using either:
     * 1) Custom tokens
     * 2) Intl.DateTimeFormat options
     * 3) Locale default formatting (if no format is provided)
     *
     * ---
     * Supported tokens:
     * YYYY, YY - Year (2026, 26)
     * MMMM, MMM, MM, M - Month (January, Jan, 01, 1), localized
     * DD, D, dddd, ddd - Day (01, 1, Thursday, Thu), localized
     * HH, H, hh, h - Hour (09, 9, 09, 9), lowercase is for the 12 hour format
     * mm, m - Minute (07, 7)
     * ss, s - Second (01, 1)
     * A (AM/PM, localized)
     *
     * ---
     * Behavior:
     * - If `formatStr` is omitted → uses locale default format
     * - If `formatStr` is an object → uses Intl.DateTimeFormat options
     * - If `formatStr` is a string → uses token-based formatting
     *
     * @param {string|Date|number} input - Date input (ISO string, Date object, or timestamp)
     * @param {string|Object} [formatStr] - Format string OR Intl options
     * @returns {string}
     *
     * @example
     * du.format("2026-03-26", "YYYY-MM-DD") // "2026-03-26"
     *
     * @example
     * du.format("2026-03-26", "D MMMM YYYY") // "26 March 2026"
     *
     * @example
     * du.format("2026-03-26") // locale default (e.g. "3/26/2026")
     *
     * @example
     * du.format("2026-03-26", { dateStyle: "long" }) // "March 26, 2026"
     */
    format(input, formatStr) {
        const date = this.toDate(input);

        // 👉 Default locale format
        if (!formatStr) {
            return this._dtf.default.format(date);
        }

        // 👉 Intl options
        if (typeof formatStr === "object") {
            return new Intl.DateTimeFormat(this.locale, formatStr).format(date);
        }

        const hours24 = date.getHours();
        const hours12 = hours24 % 12 || 12;

        const map = {
            YYYY: date.getFullYear(),
            YY: String(date.getFullYear()).slice(-2),

            MMMM: this._dtf.monthLong.format(date),
            MMM: this._dtf.monthShort.format(date),
            MM: this.pad(date.getMonth() + 1),
            M: date.getMonth() + 1,

            DD: this.pad(date.getDate()),
            D: date.getDate(),

            dddd: this._dtf.weekdayLong.format(date),
            ddd: this._dtf.weekdayShort.format(date),

            HH: this.pad(hours24),
            H: hours24,

            hh: this.pad(hours12),
            h: hours12,

            mm: this.pad(date.getMinutes()),
            m: date.getMinutes(),

            ss: this.pad(date.getSeconds()),
            s: date.getSeconds(),

            A: this._dtf.dayPeriod
                .formatToParts(date)
                .find(p => p.type === "dayPeriod")?.value || ""
        };

        return formatStr.replace(
            /YYYY|YY|MMMM|MMM|MM|M|DD|D|dddd|ddd|HH|H|hh|h|mm|m|ss|s|A/g,
            token => map[token]
        );
    }

    /**
     * Format a date relative to another date (e.g. "2 hours ago", "in 3 days").
     *
     * @param {string|Date|number} input - Target date
     * @param {string|Date|number} [base=new Date()] - Base date to compare against
     * @returns {string}
     *
     * @example
     * du.relative(Date.now() - 60000) // "1 minute ago"
     * du.relative(Date.now() + 86400000) // "in 1 day"
     */
    relative(input, base = new Date()) {
        const date = this.toDate(input);
        const now = this.toDate(base);

        const diffSec = Math.round((date - now) / 1000);

        const units = [
            { limit: 60, unit: "second", value: diffSec },
            { limit: 60, unit: "minute", value: diffSec / 60 },
            { limit: 24, unit: "hour", value: diffSec / 3600 },
            { limit: 7, unit: "day", value: diffSec / 86400 },
            { limit: 4.345, unit: "week", value: diffSec / 604800 },
            { limit: 12, unit: "month", value: diffSec / 2629800 },
            { limit: Infinity, unit: "year", value: diffSec / 31557600 }
        ];

        for (const u of units) {
            if (Math.abs(u.value) < u.limit) {
                return this._rtf.format(Math.round(u.value), u.unit);
            }
        }
    }
}