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
 * Adopted from codbex-athena-app (js/services/formValidation.js).
 *
 * FormValidation — simple schema-based form validator.
 *
 * Schema shape:
 *   {
 *     fieldName: [
 *       { rule: 'required',  message: 'Field is required.' },
 *       { rule: 'minLength', value: 2,       message: 'At least 2 chars.' },
 *       { rule: 'pattern',   value: '^\\d+$', message: 'Digits only.' },
 *     ]
 *   }
 *
 * Built-in rules: required, minLength, maxLength, min, max, email, pattern.
 * Returns: { errors: { field: 'msg' }, valid: boolean }. First failing rule wins.
 */
(function (root) {
  // The String coercion is what makes `required` correct for ARRAY-modelled controls too (a
  // multiselect's model): String([]) === '' fails the rule, a non-empty array stringifies to its
  // joined values and passes. Keep the coercion - an emptiness check written any other way must
  // still hold for arrays.
  const trim = v => (v == null ? '' : String(v).trim());

  function applyRule(rule, value, cfg) {
    switch (rule) {
      case 'required':  return trim(value) !== '';
      case 'minLength': return trim(value).length >= cfg.value;
      case 'maxLength': return trim(value).length <= cfg.value;
      case 'min':       return !isNaN(value) && Number(value) >= cfg.value;
      case 'max':       return !isNaN(value) && Number(value) <= cfg.value;
      case 'email':     return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trim(value));
      case 'pattern':   return new RegExp(cfg.value, cfg.flags || '').test(trim(value));
      default:          return true; // unknown rules pass silently
    }
  }

  function validate(data, schema) {
    const errors = {};
    for (const [field, fieldRules] of Object.entries(schema)) {
      const value = data[field];
      for (const cfg of fieldRules) {
        // Presence is `required`'s decision alone: a format/range rule (email, pattern, minLength,
        // min/max) describes what a value must look like WHEN THERE IS ONE. Applying it to an empty
        // value turns every optional field carrying a format constraint into a required one by the
        // back door — a customer with no e-mail could not be saved at all ("Enter a valid email
        // address." on a blank, non-required Email), and the only way through was to invent one.
        if (cfg.rule !== 'required' && trim(value) === '') continue;
        if (!applyRule(cfg.rule, value, cfg)) {
          errors[field] = cfg.message;
          break; // first failing rule wins
        }
      }
    }
    return { errors, valid: Object.keys(errors).length === 0 };
  }

  root.FormValidation = { validate };
})(window);
