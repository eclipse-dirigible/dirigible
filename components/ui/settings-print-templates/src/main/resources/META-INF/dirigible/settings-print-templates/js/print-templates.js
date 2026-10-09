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
angular.module('printTemplates', ['blimpKit', 'platformView']).controller('PrintTemplatesController', ($scope, $http, ButtonStates) => {
    const PRINT_API = '/services/print';
    const CONFIGURATION_API = '/services/core/configurations/tenant';
    const dialogHub = new DialogHub();

    $scope.loaded = false;
    $scope.documentTypes = [];
    $scope.languages = [];
    $scope.templates = [];
    // Whether the tenant stored a selection for the chosen language - without one the newest shipped
    // version prints, and every release switches to its own; "Use default" clears it.
    $scope.selectionStored = false;
    $scope.selected = { entity: '', language: '' };
    $scope.editor = null;
    $scope.diff = null;

    function templatesUrl(name) {
        const base = `${PRINT_API}/${encodeURIComponent($scope.selected.entity)}/templates`;
        return name ? `${base}/${encodeURIComponent(name)}` : base;
    }

    function lang() {
        return `lang=${encodeURIComponent($scope.selected.language)}`;
    }

    function showError(title, response) {
        console.error(response);
        dialogHub.showAlert({
            title: title,
            message: (response && response.data && response.data.message) || 'Please look at the console for more information.',
            type: AlertTypes.Error,
            preformatted: false,
        });
    }

    // A response read as a blob carries its error body as a blob too: read it back as JSON first.
    function showBlobError(title, response) {
        const body = response && response.data;
        if (!(body instanceof Blob)) {
            showError(title, response);
            return;
        }
        body.text().then((text) => {
            let data = null;
            try {
                data = JSON.parse(text);
            } catch (e) {
                console.error(e);
            }
            $scope.$evalAsync(() => showError(title, { ...response, data: data }));
        });
    }

    // The tenant configuration key that selects the active template - the server names it, so the
    // page never repeats the print engine's key rule.
    $scope.selectionKey = () => {
        const language = $scope.languages.find((each) => each.code === $scope.selected.language);
        return language ? language.selectionKey : '';
    };

    $scope.loadDocumentTypes = () => {
        $http.get(`${PRINT_API}/document-types`).then((response) => {
            $scope.documentTypes = response.data;
            $scope.loaded = true;
            const current = $scope.documentTypes.find((type) => type.entity === $scope.selected.entity);
            if (!current && $scope.documentTypes.length) {
                $scope.selected.entity = $scope.documentTypes[0].entity;
            }
            $scope.entitySelected();
        }, (response) => showError('Unable to load the document types', response));
    };

    // The languages of the chosen document type only - one CMS listing per choice, not per type.
    $scope.entitySelected = () => {
        $scope.languages = [];
        $scope.templates = [];
        if (!$scope.selected.entity) return;
        const entity = $scope.selected.entity;
        $http.get(`${PRINT_API}/${encodeURIComponent(entity)}/languages`).then((response) => {
            if (entity !== $scope.selected.entity) return;
            $scope.languages = response.data;
            if (!$scope.languages.some((each) => each.code === $scope.selected.language)) {
                $scope.selected.language = $scope.languages.length ? $scope.languages[0].code : '';
            }
            $scope.load();
        }, (response) => showError('Unable to load the print template languages', response));
    };

    $scope.load = () => {
        $scope.editor = null;
        $scope.diff = null;
        if (!$scope.selected.entity || !$scope.selected.language) {
            $scope.templates = [];
            return;
        }
        $http.get(`${templatesUrl()}?${lang()}&details=true`).then((response) => {
            $scope.templates = response.data.map((template) => ({ ...template, compare: false }));
        }, (response) => showError('Unable to load the print templates', response));
        $http.get(CONFIGURATION_API).then((response) => {
            const key = $scope.selectionKey();
            $scope.selectionStored = (response.data || []).some((each) => each.key === key && each.value);
        }, (response) => {
            console.error(response);
            $scope.selectionStored = false;
        });
    };

    $scope.useDefault = () => {
        $http.delete(`${CONFIGURATION_API}?key=${encodeURIComponent($scope.selectionKey())}`).then(() => {
            $scope.load();
        }, (response) => showError('Unable to clear the active print template', response));
    };

    $scope.comparing = () => $scope.templates.filter((template) => template.compare);

    $scope.preview = (template) => {
        // The tab is opened now, while the click is still the user's gesture: opened after the
        // asynchronous render, the browser's popup blocker would swallow it.
        const tab = window.open('', '_blank');
        // Renders the template with an empty record, so the layout shows with its labels and no data.
        const url = `${PRINT_API}/${encodeURIComponent($scope.selected.entity)}?${lang()}&template=${encodeURIComponent(template.name)}`;
        $http.post(url, JSON.stringify({ document: {}, items: [] }), {
            headers: { 'Content-Type': 'application/json' },
            responseType: 'blob',
        }).then((response) => {
            const pdf = URL.createObjectURL(response.data);
            if (tab) {
                tab.location.href = pdf;
                return;
            }
            // Popups are blocked outright: download the preview instead, which is always allowed.
            const link = document.createElement('a');
            link.href = pdf;
            link.download = `${template.name}.pdf`;
            document.body.appendChild(link);
            link.click();
            link.remove();
        }, (response) => {
            if (tab) tab.close();
            showBlobError('Unable to preview the print template', response);
        });
    };

    $scope.setActive = (template) => {
        $http.put(CONFIGURATION_API, JSON.stringify({ key: $scope.selectionKey(), value: template.name })).then(() => {
            $scope.load();
        }, (response) => showError('Unable to set the active print template', response));
    };

    $scope.duplicate = (template) => {
        dialogHub.showFormDialog({
            title: `Duplicate "${template.name}"`,
            form: {
                'name': {
                    label: 'New template name',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'e.g. acme-blue',
                    inputRules: { patterns: ['^[A-Za-z0-9][A-Za-z0-9._-]*$'] },
                    errorMsg: "Letters, digits, '.', '-' and '_', starting with a letter or digit",
                    minlength: 1,
                    maxlength: 100,
                    focus: true,
                    required: true,
                },
            },
            submitLabel: 'Duplicate',
            cancelLabel: 'Cancel',
        }).then((form) => {
            if (!form) return;
            const url = `${templatesUrl(template.name)}/duplicate?${lang()}&as=${encodeURIComponent(form['name'].trim())}`;
            $http.post(url).then(() => {
                $scope.load();
            }, (response) => showError('Unable to duplicate the print template', response));
        });
    };

    $scope.edit = (template) => {
        $scope.diff = null;
        $http.get(`${templatesUrl(template.name)}?${lang()}`, { transformResponse: (data) => data }).then((response) => {
            $scope.editor = { name: template.name, source: response.data };
        }, (response) => showError('Unable to read the print template', response));
    };

    $scope.save = () => {
        $http.put(`${templatesUrl($scope.editor.name)}?${lang()}`, $scope.editor.source, {
            headers: { 'Content-Type': 'text/plain' },
        }).then(() => {
            $scope.load();
        }, (response) => showError('Unable to save the print template', response));
    };

    $scope.closeEditor = () => {
        $scope.editor = null;
    };

    $scope.remove = (template) => {
        dialogHub.showDialog({
            title: 'Delete print template',
            message: `Are you sure you want to delete "${template.name}"?`,
            buttons: [
                { id: 'delete', label: 'Delete', state: ButtonStates.Negative },
                { id: 'cancel', label: 'Cancel', state: ButtonStates.Transparent },
            ],
        }).then((buttonId) => {
            if (buttonId !== 'delete') return;
            $http.delete(`${templatesUrl(template.name)}?${lang()}`).then(() => {
                $scope.load();
            }, (response) => showError('Unable to delete the print template', response));
        });
    };

    $scope.compare = () => {
        const [left, right] = $scope.comparing();
        const read = (template) => $http.get(`${templatesUrl(template.name)}?${lang()}`, { transformResponse: (data) => data });
        Promise.all([read(left), read(right)]).then(([leftResponse, rightResponse]) => {
            $scope.$evalAsync(() => {
                $scope.editor = null;
                $scope.diff = { left: left.name, right: right.name, lines: diffLines(leftResponse.data, rightResponse.data) };
            });
        }, (response) => showError('Unable to compare the print templates', response));
    };

    $scope.closeDiff = () => {
        $scope.diff = null;
    };

    /** A line diff (jsdiff): ' ' kept, '-' only on the left, '+' only on the right. */
    function diffLines(leftText, rightText) {
        const lines = [];
        for (const part of Diff.diffLines(leftText, rightText)) {
            const kind = part.added ? '+' : part.removed ? '-' : ' ';
            const text = part.value.replace(/\r?\n$/, '');
            for (const line of text.split(/\r?\n/)) {
                lines.push({ kind: kind, text: line });
            }
        }
        return lines;
    }

    $scope.loadDocumentTypes();
});
