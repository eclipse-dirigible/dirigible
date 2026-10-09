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
 * The Print action of a generated document or form page: the .print template is rendered
 * server-side to PDF from the payload the page's generated print feeder returns. Spread into the
 * page's Alpine data:
 *
 *   ...printActions({ entity: 'SalesInvoice', feeder: '/services/java/<project>/gen/events/<module>/SalesInvoicePrintFeeder/' }),
 *
 * The page provides `id` (the record's id). The page's view binds printLangOpen, printLanguages,
 * printLang, printLayouts, printTemplate, printLoading, printBusy and printError, and calls
 * openPrint(), selectPrintLanguage(code) and printDoc(lang, template).
 *
 * Print always renders LIVE. The immutable issued copy stays first-class on its own surface: the
 * read-only Snapshot panel serves every stored version (Open + Download), so a live re-render after
 * master-data changes is acceptable.
 *
 * When the dialog opens: with more than one template language, or more than one tenant template in
 * the chosen language - a real choice the tenant authored. The dialog then lists the tenant's own
 * templates, the active one and the shipped version that prints by default; the shipped default
 * alone never opens it, so a tenant with one custom layout still prints directly. The other shipped
 * versions accumulate with every release and are pinned in Settings > Print Templates, not chosen
 * per print - listing them would open the dialog on every print once a second release has shipped.
 *
 * A print names its template only when the user picked one: without a pick the server resolves the
 * tenant's active template itself, so a selection changed between the listing and the print never
 * turns into a 404.
 */
function printActions(options) {
  const base = '/services/print/' + encodeURIComponent(options.entity);
  return {
    printLangOpen: false,
    printLanguages: [],
    printLang: '',
    printLayouts: [],
    printTemplate: '',
    // The template the server prints when none is named - what the dialog preselects.
    printActiveTemplate: '',
    // While the layouts of a language are being read: Print waits, so it never sends one language
    // with another language's layout.
    printLoading: false,
    printBusy: false,
    printError: null,
    _printLayoutsRequest: 0,

    async openPrint() {
      this.printError = null;
      this.printBusy = true;
      let languages = [];
      try {
        languages = (await App.services.api.get(base + '/languages', { baseUrl: '' })) || [];
      } catch (e) {
        console.error('Print languages lookup failed', e);
      }
      // The Region & Language setting only pre-sorts its language first as the suggested default;
      // zero languages still attempts the default, which the server answers with a clear 404.
      const configured = (Alpine.store('locale') || {}).value;
      this.printLanguages = languages.slice().sort((a, b) =>
        (a.code === configured ? -1 : 0) - (b.code === configured ? -1 : 0));
      await this.selectPrintLanguage(languages.length ? this.printLanguages[0].code : 'en');
      // Busy until the layouts are known too: a second click meanwhile would start a second flow.
      this.printBusy = false;
      if (this.printLanguages.length > 1 || this.printLayouts.filter((t) => t.kind === 'tenant').length > 1) {
        // A real choice: ALWAYS ask (auto-printing the configured language here suppressed the
        // dialog entirely, since the locale store always resolves to a value - a shipped regression).
        // The layout is preselected to the one the tenant prints with.
        this.printLangOpen = true;
      } else {
        await this.printDoc(this.printLang, this.printTemplate);
      }
    },

    // The layouts of a print language, the active one preselected: the layout the tenant configured,
    // else the shipped default. Picking another prints this one document with it; the tenant default
    // stays as it is. A response for a language the user has since left is dropped.
    async selectPrintLanguage(lang) {
      const request = ++this._printLayoutsRequest;
      this.printLang = lang;
      this.printLoading = true;
      let templates = [];
      try {
        templates = (await App.services.api.get(base + '/templates?lang=' + encodeURIComponent(lang), { baseUrl: '' })) || [];
      } catch (e) {
        console.error('Print templates lookup failed', e);
      }
      if (request !== this._printLayoutsRequest) {
        return;
      }
      this.printLayouts = templates.filter((t) => t.kind === 'tenant' || t.active || t.defaultTemplate);
      const active = templates.find((t) => t.active);
      this.printActiveTemplate = active ? active.name : '';
      this.printTemplate = this.printActiveTemplate;
      this.printLoading = false;
    },

    async printDoc(lang, template) {
      if (this.printLoading) {
        return;
      }
      this.printError = null;
      this.printBusy = true;
      try {
        // The generated print feeder loads the document + its related graph through the repositories
        // and returns the { document, items } payload the template binds - so
        // {{document.<Relation>.<Field>}} resolves. It is the auditable source of what a print
        // receives; the browser calls it as the logged-in user (auth + tenant + i18n are the caller's).
        // Pin the feeder's language to the CHOSEN print language, not the UI locale: the repositories'
        // multilingual overlay reads Accept-Language, so without this the nomenclature values (Payment
        // Method, Status, ...) would render in the UI locale while the template is in the print language.
        const payload = await App.services.api.get(options.feeder + encodeURIComponent(this.id), { baseUrl: '', language: lang });
        const picked = template && template !== this.printActiveTemplate ? template : '';
        const response = await fetch(base + '?lang=' + encodeURIComponent(lang)
          + (picked ? '&template=' + encodeURIComponent(picked) : ''), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });
        if (!response.ok) {
          this.printError = response.status === 404
            ? 'No print template found. Publish the project or add one in Settings > Print Templates.'
            : 'Print failed (' + response.status + ')';
          return;
        }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        // The object URL must outlive the tab that renders it: a timed revoke kills the PDF
        // viewer's reload/print the moment it fires (observed live - the blob tab worked once,
        // then "the file was removed"). The URL is released when THIS page unloads, which is the
        // blob's natural owner lifetime; a print-sized PDF held until then costs nothing.
        const opened = window.open(url, '_blank');
        if (!opened) {
          // Popup blocked - the open runs after async work, outside the click's gesture stack.
          // Fall back to a download, which the browser always allows.
          const link = document.createElement('a');
          link.href = url;
          link.download = options.entity + '-' + this.id + '.pdf';
          document.body.appendChild(link);
          link.click();
          link.remove();
        }
      } catch (e) {
        console.error('Print failed', e);
        this.printError = 'Print failed';
      } finally {
        this.printBusy = false;
      }
    },
  };
}
