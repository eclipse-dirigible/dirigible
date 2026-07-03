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
/**
 * Adopted from codbex-athena-app (js/services/api.js).
 *
 * ApiError — rich error thrown by api.request() for any non-OK response or
 * transport failure. Extends Error so legacy `err.message` access keeps working,
 * but `err.message` is the DEVELOPER-facing text and MUST NOT be shown to the
 * end-user. UI code selects localized text via App.services.apiErrors.
 *
 * Error body contract:
 *   { errorType, errorMessage, errorCauses: [{ errorType, errorMessage, field }] }
 */
class ApiError extends Error {
  constructor({ httpStatus, errorType, errorMessage, errorCauses } = {}) {
    super(errorMessage || String(httpStatus || ''));
    this.name = 'ApiError';
    this.isApiError = true;
    this.httpStatus = httpStatus || 0;          // 0 == transport/network failure
    this.errorType = errorType || null;
    this.errorMessage = errorMessage || '';     // dev-facing — never shown to user
    this.errorCauses = Array.isArray(errorCauses) ? errorCauses : [];
  }
}

App.services.ApiError = ApiError;

App.services.api = {
  // Base URL for this application's generated REST controllers. Read from the
  // generated config (js/config.js) so this file stays model-independent.
  get baseUrl() {
    return (App.config && App.config.restBase) || '/services/ts/app/gen/api';
  },

  // Base URLs for OTHER backend modules, referenced by a logical name via
  // request(method, url, body, { base: 'name' }). Populate per app as needed.
  baseUrls: {},

  // Resolve the base URL for a request from its options:
  //   { baseUrl: '…' } explicit override  >  { base: 'name' } named module  >  default.
  resolveBaseUrl(opts = {}) {
    // Note: check for `undefined`, not truthiness — `{ baseUrl: '' }` is a valid
    // override meaning "the url is already absolute, prepend nothing".
    if (opts.baseUrl !== undefined) return opts.baseUrl;
    if (opts.base) {
      const named = this.baseUrls[opts.base];
      if (!named) throw new Error(`[api] unknown base '${opts.base}' — add it to App.services.api.baseUrls`);
      return named;
    }
    return this.baseUrl;
  },

  // Canonical HTTP status -> errorType, so errorType is always populated even
  // when the server omits a structured body.
  typeFromStatus(status) {
    switch (status) {
      case 400: return 'BadRequest';
      case 401: return 'Unauthorized';
      case 403: return 'InsufficientPermission';
      case 404: return 'NotFound';
      case 422: return 'ValidationError';
      case 502: return 'UpstreamError';
      default:  return 'InternalServerError';
    }
  },

  // The app's single language flag (the Region & Language setting, an Alpine store). Sent as
  // Accept-Language on every call so the SAME flag drives the backend: generated multilingual
  // repositories overlay <TABLE>_LANG values for it. Absent store (standalone pages) -> no header
  // override (the browser's own Accept-Language applies).
  language() {
    try {
      const locale = window.Alpine && Alpine.store('locale');
      return (locale && locale.value) || null;
    } catch (e) { return null; }
  },

  // request(method, url, body, opts?) — opts selects the base URL for this call:
  //   { baseUrl } explicit override, or { base } named entry in baseUrls.
  async request(method, url, body, opts = {}) {
    const baseUrl = this.resolveBaseUrl(opts);

    // X-Requested-With marks the call as programmatic for browsers without Sec-Fetch-Mode: the
    // server then answers an expired session with a PLAIN 401 (no Basic challenge), so the
    // browser's native login dialog never pops over a background poll.
    const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
    const language = this.language();
    if (language) headers['Accept-Language'] = language;

    let r;
    try {
      r = await fetch(`${baseUrl}${url}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
        credentials: 'same-origin'
      });
    } catch (e) {
      const err = new ApiError({ httpStatus: 0, errorType: 'NetworkError', errorMessage: e.message });
      console.error(`[api] ${method} ${baseUrl}${url} failed:`, err.errorMessage);
      throw err;
    }

    if (r.ok) {
      // A success can carry an empty body (204, or a 200 from an action endpoint such as task
      // claim/complete). Reading r.json() on an empty body throws "Unexpected end of JSON input",
      // so read text first and only parse when there's something to parse.
      if (r.status === 204) return null;
      const text = await r.text();
      if (!text) return null;
      try { return JSON.parse(text); } catch (_) { return text; }
    }

    // Non-OK: read the body ONCE as text, then attempt to parse the structured error.
    const raw = await r.text().catch(() => '');
    let parsed = null;
    if (raw) {
      try { parsed = JSON.parse(raw); } catch (_) { /* non-JSON body */ }
    }

    const err = parsed && typeof parsed === 'object'
      ? new ApiError({
          httpStatus: r.status,
          errorType: parsed.errorType || parsed.code || this.typeFromStatus(r.status),
          errorMessage: parsed.errorMessage || parsed.message || r.statusText,
          errorCauses: parsed.errorCauses,
        })
      : new ApiError({
          httpStatus: r.status,
          errorType: this.typeFromStatus(r.status),
          errorMessage: raw || r.statusText,
        });

    console.error(`[api] ${method} ${baseUrl}${url} -> ${err.httpStatus} ${err.errorType}:`, err.errorMessage, err.errorCauses);
    throw err;
  },

  // Thin verb helpers over request(). `opts` selects the base URL (see request).
  get(url, opts)        { return this.request('GET', url, undefined, opts); },
  post(url, body, opts) { return this.request('POST', url, body, opts); },
  put(url, body, opts)  { return this.request('PUT', url, body, opts); },
  delete(url, opts)     { return this.request('DELETE', url, undefined, opts); },

  // getAll(url, opts?) — fetch an ENTIRE collection, transparently paging past the
  // generated controllers' default page size. A bare GET on a list controller returns
  // only its first page ($limit defaults to 20), which silently truncated client-side-
  // paginated lists and relationship dropdowns to 20 rows. This walks $limit/$offset
  // pages until a short (or empty) page, concatenating the results. Use it wherever the
  // UI needs the whole set (entity lists with client-side pagination, dropdown options);
  // keep get() for single records and /count.
  async getAll(url, opts = {}) {
    const pageSize = 1000;
    const sep = url.includes('?') ? '&' : '?';
    const all = [];
    for (let offset = 0;; offset += pageSize) {
      const page = await this.get(`${url}${sep}$limit=${pageSize}&$offset=${offset}`, opts);
      if (!Array.isArray(page) || page.length === 0) break;
      all.push(...page);
      if (page.length < pageSize) break;
    }
    return all;
  },
};
