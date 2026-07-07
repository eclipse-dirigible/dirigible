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
 * branding — makes the Harmonia shells honour the SAME instance branding as the AngularJS shell,
 * driven by the same DIRIGIBLE_BRANDING_* env vars. The platform service
 * /services/js/platform-branding/branding.js (loaded just before this file) sets the global
 * top.PlatformBranding = { name, subtitle, brand, brandUrl, icons.favicon, logo, theme, prefix }
 * from Configurations.get(...). This adapter reads that global (the AngularJS shell reads the same
 * one via getBrandingInfo()), applies the favicon + document title to the plain HTML head, and
 * exposes the values as an Alpine `branding` store so the shell markup can bind logo/name/subtitle.
 *
 * Opt-in title handling via a <html> attribute (each shell decides, since only the platform launchpad
 * is "the brand" while a generated app's tab keeps its own name):
 *   data-brand-title         -> document.title = brand name          (the Applications launchpad)
 *   data-brand-title-suffix  -> document.title = "<existing> | brand" (a generated app shell)
 *
 * The theme value is a BlimpKit theme id (not applicable to Harmonia's own light/dark), and the
 * localStorage prefix stays the fixed codbex.harmonia.* the Harmonia library itself uses — neither is
 * consumed here.
 */
(() => {
  const DEFAULTS = {
    name: 'Dirigible',
    subtitle: '',
    brand: 'Eclipse',
    brandUrl: 'https://www.dirigible.io/',
    favicon: '/services/web/platform-branding/images/favicon.ico',
    logo: '/services/web/platform-branding/images/dirigible.svg',
    prefix: 'dirigible',
  };

  // The platform branding global is set on `top` (shared across nested same-origin iframes). Fall back
  // to defaults if it is absent (service not loaded) or unreachable (a cross-origin embedding).
  const read = () => {
    let src = null;
    try {
      src = window.PlatformBranding || (window.top && window.top.PlatformBranding) || null;
    } catch (e) {
      src = null; // cross-origin top — keep defaults
    }
    if (!src) return { ...DEFAULTS };
    return {
      name: src.name || DEFAULTS.name,
      subtitle: src.subtitle || '',
      brand: src.brand || DEFAULTS.brand,
      brandUrl: src.brandUrl || DEFAULTS.brandUrl,
      favicon: (src.icons && src.icons.favicon) || DEFAULTS.favicon,
      logo: src.logo || DEFAULTS.logo,
      prefix: src.prefix || DEFAULTS.prefix,
    };
  };

  const BRANDING = read();
  window.HarmoniaBranding = BRANDING;

  // Replace the shell's blank placeholder favicon with the instance one.
  try {
    if (BRANDING.favicon && typeof document !== 'undefined' && document.head) {
      let link = document.querySelector('link[rel~="icon"]');
      if (!link) {
        link = document.createElement('link');
        link.setAttribute('rel', 'icon');
        document.head.appendChild(link);
      }
      link.setAttribute('href', BRANDING.favicon);
    }
  } catch (e) { /* head not ready — non-fatal, favicon just stays the placeholder */ }

  // Per-shell tab title (opt-in via a <html> attribute; see file header).
  try {
    const root = document.documentElement;
    if (root && root.hasAttribute('data-brand-title')) {
      document.title = BRANDING.name;
    } else if (root && root.hasAttribute('data-brand-title-suffix') && BRANDING.name) {
      const current = (document.title || '').trim();
      if (current && current.indexOf(BRANDING.name) < 0) {
        document.title = current + ' | ' + BRANDING.name;
      } else if (!current) {
        document.title = BRANDING.name;
      }
    }
  } catch (e) { /* no DOM — non-fatal */ }

  if (typeof document !== 'undefined' && document.addEventListener) {
    document.addEventListener('alpine:init', () => {
      Alpine.store('branding', {
        name: BRANDING.name,
        subtitle: BRANDING.subtitle,
        brand: BRANDING.brand,
        brandUrl: BRANDING.brandUrl,
        logo: BRANDING.logo,
        favicon: BRANDING.favicon,
        prefix: BRANDING.prefix,
      });
    }, { once: true });
  }
})();
