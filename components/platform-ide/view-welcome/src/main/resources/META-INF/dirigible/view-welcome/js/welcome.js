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
const welcome = angular.module('welcome', ['blimpKit', 'platformView', 'WorkspaceService', 'TemplatesService']);
welcome.filter('startFrom', () => {
    return (input, start) => {
        start = +start;
        if (input) return input.slice(start);
        return 0;
    };
});
welcome.controller('WelcomeController', ($scope, $http, WorkspaceService, TemplatesService) => {
    const dialogHub = new DialogHub();
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    $scope.templates = [];
    const workspaces = [];
    const selectedWorkspace = WorkspaceService.getCurrentWorkspace();
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
    $scope.pageSize = 12;

    const getTemplateFormItems = (template) => {
        let form = {
            'workspace': {
                controlType: 'dropdown',
                label: 'Workspace',
                required: true,
                value: selectedWorkspace,
                options: workspaces,
            },
            'projectName': {
                controlType: 'input',
                type: 'text',
                label: 'Project',
                placeholder: 'Project name',
                required: true,
            },
            'fileName': {
                controlType: 'input',
                type: 'text',
                label: 'File name',
                placeholder: 'fileName',
                required: true,
            },
        };
        for (let i = 0; i < template.parameters.length; i++) {
            form[`param_${template.parameters[i].name}`] = {
                controlType: 'input',
                type: 'text',
                label: template.parameters[i].label,
                require: true,
            };
        }
        return form;
    };

    TemplatesService.listTemplates().then((response) => {
        $scope.$evalAsync(() => {
            $scope.templates.push(...response.data.filter(value => !value.extension));
            $scope.search.applyFilter();
        });
    }, (response) => {
        console.error(response);
        dialogHub.showAlert({
            title: 'Template API error',
            message: 'Unable to load template list',
            type: AlertTypes.Error,
        });
    });

    WorkspaceService.listWorkspaceNames().then((response) => {
        $scope.$evalAsync(() => {
            workspaces.push(...response.data.map(x => ({ label: x, value: x })));
        });
    }, (response) => {
        console.error(response);
        dialogHub.showAlert({
            title: 'Workspace API error',
            message: 'Unable to load workspace list',
            type: AlertTypes.Error,
        });
    });

    $scope.openCreateProjectDialog = (template) => {
        $scope.selectedTemplate = template;
        dialogHub.showFormDialog({
            title: 'Create from template',
            subheader: template.name,
            form: getTemplateFormItems(template),
            submitLabel: 'Create',
            cancelLabel: 'Cancel'
        }).then(async (form) => {
            if (form) {
                dialogHub.showBusyDialog('Creating...');
                try {
                    await createFromTemplate(form);
                } catch (ex) {
                    console.error(ex);
                    dialogHub.showAlert({
                        title: 'Failed to create project',
                        message: ex.message || 'There was an error while trying to create a new project from template.',
                        type: AlertTypes.Error,
                    });
                    statusBarHub.showError('Failed to create project');
                } finally {
                    statusBarHub.showMessage(`Created new project in '${form.workspace}'`);
                    dialogHub.closeBusyDialog();
                }
            }
        }, (error) => {
            console.error(error);
        });
    };

    const createFromTemplate = (formData) => {
        if (!$scope.selectedTemplate)
            return Promise.reject(new Error('No template selected'));

        const projectName = formData.projectName;
        const workspace = formData.workspace;
        const fileName = formData.fileName;
        const parameters = {};
        for (const [key, value] of Object.entries(formData)) {
            if (key.startsWith('param_')) {
                parameters[key.substring(6)] = value;
            }
        }

        const url = `/services/ide/generate/file/${workspace}/${projectName}/${fileName}`;

        return new Promise((resolve, reject) => {
            $http.post(url, { template: $scope.selectedTemplate.id, parameters })
                .then(response => {
                    workspaceHub.announceWorkspaceChanged({ workspace: workspace, params: { publish: { path: `/${workspace}/${projectName}` } } });
                    resolve(response.data);
                }).catch(ex => {
                    reject(ex);
                });
        });
    };
});