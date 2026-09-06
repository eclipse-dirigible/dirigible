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
 * Adopted from codbex-athena-app (js/services/apiError.js).
 *
 * apiErrors — localized, user-safe message catalog for API errors.
 *
 * The server's `errorMessage` is developer-facing and MUST NOT be shown to the
 * end-user (it is logged to the console by api.js). The UI selects what to
 * display based on `errorType` via the resolvers below.
 *
 * Designed for later i18n: swap the flat maps for a locale lookup in one place.
 */
(function (root) {
  const App = root.App = root.App || {};
  App.services = App.services || {};

  App.services.apiErrors = {
    // Top-level errorType -> user-facing message.
    messages: {
      BadRequest:             'The request could not be completed. Please review your input.',
      ValidationError:        'Please correct the highlighted fields.',
      Unauthorized:           'Your session has ended. Please sign in again.',
      InsufficientPermission: 'You do not have permission to perform this action.',
      TokenExpired:           'Your session has expired. Please sign in again.',
      NotFound:               'The requested item could not be found.',
      Conflict:               'This is not allowed in the record\u2019s current state.',
      UpstreamError:          'A dependent service is unavailable. Please try again shortly.',
      InternalServerError:    'Something went wrong on our end. Please try again.',
      NetworkError:           'Unable to reach the server. Check your connection and try again.',
    },

    // 422 cause errorType -> per-field message. Extend as backend cause types appear.
    fieldMessages: {
      NotNull:   'This field is required.',
      Required:  'This field is required.',
      NotEmpty:  'This field is required.',
      Future:    'Date must be in the future.',
      Past:      'Date must be in the past.',
      Pattern:   'This value has an invalid format.',
      Email:     'Enter a valid email address.',
      Size:      'This value has an invalid length.',
      Min:       'This value is too small.',
      Max:       'This value is too large.',
      Duplicate: 'This value already exists.',
    },

    default: 'Something went wrong. Please try again.',

    /** Top-level, user-safe message for an error. `fallback` overrides the catalog default. */
    messageFor(err, fallback) {
      const type = err && err.errorType;
      return (type && this.messages[type]) || fallback || this.default;
    },

    /**
     * The message for a REFUSAL - a business rule the server applied to something the user asked for
     * (a transition outside its `from:` statuses, a `checks:` gate) rather than a fault.
     *
     * Those messages are authored, in the intent, to be read by the person who pressed the button
     * ("VoidSalesInvoice is allowed only from status [3, 4] - current status is [6]"), so here the
     * server's text IS the user-facing text and the generic catalog line would destroy the only
     * information the response carried (dirigible #7073). Restricted to the two statuses that mean
     * "your request was refused" - 400 and 409; every other failure keeps going through messageFor,
     * where a developer-facing errorMessage never reaches the screen.
     */
    refusalMessageFor(err, fallback) {
      const status = err && err.httpStatus;
      const message = typeof (err && err.errorMessage) === 'string' ? err.errorMessage.trim() : '';
      // A refusal is one authored sentence. A markup blob or an essay is a proxy/container error page
      // that happens to carry the same status - show the catalog line for those rather than pasting
      // an HTML document into a toast.
      const readable = message && message.length <= 300 && message.indexOf('<') === -1;
      if ((status === 400 || status === 409) && readable) {
        return message;
      }
      return this.messageFor(err, fallback);
    },

    /** Per-field message for a single 422 errorCauses[] entry. */
    fieldMessageFor(cause) {
      const type = cause && cause.errorType;
      return (type && this.fieldMessages[type]) || this.default;
    },

    /**
     * The property a server rejection NAMES, or null.
     *
     * The generated controllers reject a bad write with a business message that carries the
     * property in single quotes ("The 'TaxRate' property is required"), as a plain 400 rather
     * than a structured 422 with errorCauses. Read that way it is a per-field rejection, and
     * answering it with the generic banner tells the user nothing about which field to fix.
     *
     * `knownNames` is the gate that keeps the developer-facing rule intact: only a quoted token
     * that IS a field of the form in hand is recognised, so an arbitrary 400 (a stack-trace
     * reason, an internal identifier) still falls back to the catalog message.
     */
    namedProperty(err, knownNames) {
      const text = (err && err.errorMessage) || '';
      const known = new Set(knownNames || []);
      const pattern = /'([A-Za-z_][A-Za-z0-9_]*)'/g;
      let match;
      while ((match = pattern.exec(text)) !== null) {
        if (known.has(match[1])) return match[1];
      }
      return null;
    },

    /**
     * The server's message rewritten for a person: every quoted property name it carries is
     * replaced by that property's display label ("The 'Tax Rate' property is required").
     * `labels` maps a property name to its label; an unmapped name is left as it stands.
     */
    messageWithLabels(err, labels) {
      const text = (err && err.errorMessage) || '';
      const map = labels || {};
      return text.replace(/'([A-Za-z_][A-Za-z0-9_]*)'/g,
        (whole, name) => (Object.prototype.hasOwnProperty.call(map, name) ? `'${map[name]}'` : whole));
    },
  };
})(window);
