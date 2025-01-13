/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
let app = angular.module("welcome", ['ideUI', 'ideView']);

app.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'welcome-view';
}]);

app.controller('welcomeCtrl', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

    $scope.templates = [];
    $scope.filteredTemplates = [];
    $scope.search = {
        text: '',
        applyFilter: () => {
            const searchText = $scope.search.text.toLowerCase();
            $scope.filteredTemplates = searchText ? $scope.templates.filter(t => t.name.toLowerCase().includes(searchText)) : $scope.templates;
            $scope.currentPage = 1;
        }
    };
    $scope.currentPage = 1;
    $scope.pageSize = 6;

    $scope.getTemplateFormItems = (template) => [
        {
            id: 'workspace',
            type: 'dropdown',
            label: 'Workspace',
            required: true,
            value: $scope.workspaces[0],
            items: $scope.workspaces.map(x => ({ label: x, value: x }))
        },
        {
            id: 'projectName',
            type: 'input',
            label: 'Project',
            placeholder: 'project name',
            required: true
        },
        {
            id: 'fileName',
            type: 'input',
            label: 'File',
            placeholder: 'file name',
            required: true
        },
        ...template.parameters.map(p => ({
            id: `param_${p.name}`,
            type: 'input',
            label: p.label,
            required: true
        }))
    ];

    $http.get('/services/js/resources-core/services/templates.js').then(function (response) {
        $scope.templates = response.data.filter(value => !value.extension);
        $scope.search.applyFilter();
    });
    $http.get('/services/ide/workspaces').then(function (response) {
        $scope.workspaces = response.data;
    });

    $scope.openCreateProjectDialog = function (template) {
        $scope.selectedTemplate = template;

        messageHub.showFormDialog(
            'createProjectForm',
            'Create from template',
            $scope.getTemplateFormItems(template),
            [{
                id: "ok",
                type: "emphasized",
                label: "Ok",
                whenValid: true
            },
            {
                id: "cancel",
                type: "transparent",
                label: "Cancel",
            }],
            'welcome-view.project.create',
            'Please wait...',
            '',
            template.name
        );
    };

    messageHub.onDidReceiveMessage(
        'welcome-view.project.create',
        async function (msg) {
            if (msg.data.buttonId === "ok") {
                try {
                    await $scope.createFromTemplate(msg.data.formData);
                } catch (ex) {
                    messageHub.setStatusError(`Failed to create project. ${ex.message}`);
                } finally {
                    messageHub.hideFormDialog("createProjectForm");
                }
            } else {
                messageHub.hideFormDialog("createProjectForm");
            }
        },
        true
    );

    $scope.createFromTemplate = function (formData) {

        if (!$scope.selectedTemplate)
            return Promise.reject(new Error('No template selected'));

        const getFormValue = (id) => {
            const item = formData.find(x => x.id === id);
            return item && item.value;
        }

        const projectName = getFormValue('projectName');
        const workspace = getFormValue('workspace');
        const fileName = getFormValue('fileName');
        const parameters = formData.reduce((ret, p) => {
            if (p.id.startsWith('param_')) {
                ret[p.id.substring(6)] = p.value;
            }
            return ret;
        }, {});

        let url = `/services/ide/generate/file/${workspace}/${projectName}/${fileName}`;

        return new Promise((resolve, reject) => {
            $http.post(url, { template: $scope.selectedTemplate.id, parameters })
                .then(response => {
                    messageHub.announceWorkspaceChanged({ name: workspace, publish: { path: `/${projectName}` } });
                    resolve(response.data);
                }).catch(ex => {
                    reject(ex);
                });
        });
    };
}]);

app.filter("startFrom", function () {
    return function (input, start) {
        start = +start;
        if (input) return input.slice(start);
        return 0;
    };
});
