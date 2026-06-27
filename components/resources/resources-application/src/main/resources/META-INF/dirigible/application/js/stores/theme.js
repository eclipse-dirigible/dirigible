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
 * theme store - the shell's light/dark switch. A global store (not a component property) so the
 * toolbar toggle resolves from any scope. Backed by Harmonia's colour-scheme API, which persists the
 * choice to the shared localStorage key and applies the `.dark` class; the hosted perspective iframes
 * read the same key so everything themes consistently.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('theme', {
    value: 'light',

    init() {
      try {
        this.value = (window.Harmonia && Harmonia.getColorScheme() === 'dark') ? 'dark' : 'light';
      } catch (e) { this.value = 'light'; }
    },

    toggle() {
      this.value = this.value === 'dark' ? 'light' : 'dark';
      if (window.Harmonia) Harmonia.setColorScheme(this.value);
    },
  });
}, { once: true });
