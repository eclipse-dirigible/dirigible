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

    // The same key the print engine reads (PrintTemplateSelection.key): the entity and the language,
    // upper-cased, anything but a letter or digit turned into an underscore.
    $scope.selectionKey = () => {
        const segment = (value) => (value || '').toUpperCase().replace(/[^A-Z0-9]/g, '_');
        return `DIRIGIBLE_PRINT_TEMPLATE_${segment($scope.selected.entity)}_${segment($scope.selected.language)}`;
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

    $scope.entitySelected = () => {
        const type = $scope.documentTypes.find((each) => each.entity === $scope.selected.entity);
        $scope.languages = type ? type.languages : [];
        if (!$scope.languages.includes($scope.selected.language)) {
            $scope.selected.language = $scope.languages.length ? $scope.languages[0] : '';
        }
        $scope.load();
    };

    $scope.load = () => {
        $scope.editor = null;
        $scope.diff = null;
        if (!$scope.selected.entity || !$scope.selected.language) {
            $scope.templates = [];
            return;
        }
        $http.get(`${templatesUrl()}?${lang()}`).then((response) => {
            $scope.templates = response.data.map((template) => ({ ...template, compare: false }));
        }, (response) => showError('Unable to load the print templates', response));
    };

    $scope.comparing = () => $scope.templates.filter((template) => template.compare);

    $scope.preview = (template) => {
        // Renders the template with an empty record, so the layout shows with its labels and no data.
        const url = `${PRINT_API}/${encodeURIComponent($scope.selected.entity)}?${lang()}&template=${encodeURIComponent(template.name)}`;
        $http.post(url, JSON.stringify({ document: {}, items: [] }), {
            headers: { 'Content-Type': 'application/json' },
            responseType: 'blob',
        }).then((response) => {
            window.open(URL.createObjectURL(response.data), '_blank');
        }, (response) => showError('Unable to preview the print template', response));
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

    /** A line diff over the longest common subsequence: ' ' kept, '-' only on the left, '+' only on the right. */
    function diffLines(leftText, rightText) {
        const left = leftText.split(/\r?\n/);
        const right = rightText.split(/\r?\n/);
        const common = Array.from({ length: left.length + 1 }, () => new Array(right.length + 1).fill(0));
        for (let i = left.length - 1; i >= 0; i--) {
            for (let j = right.length - 1; j >= 0; j--) {
                common[i][j] = left[i] === right[j] ? common[i + 1][j + 1] + 1 : Math.max(common[i + 1][j], common[i][j + 1]);
            }
        }
        const lines = [];
        let i = 0;
        let j = 0;
        while (i < left.length && j < right.length) {
            if (left[i] === right[j]) {
                lines.push({ kind: ' ', text: left[i++] });
                j++;
            } else if (common[i + 1][j] >= common[i][j + 1]) {
                lines.push({ kind: '-', text: left[i++] });
            } else {
                lines.push({ kind: '+', text: right[j++] });
            }
        }
        while (i < left.length) lines.push({ kind: '-', text: left[i++] });
        while (j < right.length) lines.push({ kind: '+', text: right[j++] });
        return lines;
    }

    $scope.loadDocumentTypes();
});
