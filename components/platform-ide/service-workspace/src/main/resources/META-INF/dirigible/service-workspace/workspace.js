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
angular.module('WorkspaceService', []).constant('workspaceStorageKey', `${brandingInfo.keyPrefix ?? 'DIRIGIBLE'}.workspace.selected`).provider('WorkspaceService', function WorkspaceServiceProvider(workspaceStorageKey) {
    this.workspacesServiceUrl = '/services/ide/workspaces';
    this.workspaceManagerServiceUrl = '/services/ide/workspace';
    this.workspaceSearchServiceUrl = '/services/ide/workspace-search';
    this.$get = ['$http', function workspaceApiFactory($http) {
        const setWorkspace = (workspace) => {
            if (workspace === undefined || workspace === null || typeof workspace !== 'string')
                throw Error("setWorkspace: workspace parameter must be an string");
            localStorage.setItem(workspaceStorageKey, workspace);
            return workspace;
        };

        /**
         * Returns the currently selected workspace.
         */
        const getCurrentWorkspace = () => {
            let storedWorkspace = localStorage.getItem(workspaceStorageKey);
            if (!storedWorkspace) storedWorkspace = setWorkspace('workspace');
            return storedWorkspace;
        };

        /**
         * Lists all available workspaces.
         */
        const listWorkspaceNames = function () {
            return $http.get(this.workspacesServiceUrl);
        }.bind(this);

        /**
         * List the contents of a path.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const list = function (resourcePath) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        /**
         * Checkes if a resource exists.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const resourceExists = function (resourcePath) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.head(url);
        }.bind(this);

        /**
         * Loads file content.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const loadContent = async function (resourcePath) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(url);
        }.bind(this);

        /**
         * Saves content to a file
         * @param {string} resourcePath - Full resource path, including workspace name.
         * @param {string} content - File content.
         */
        const saveContent = async function (resourcePath, content) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.put(url, content, { headers: { 'Content-Type': 'text/plain;charset=UTF-8' } });
        }.bind(this);

        /**
         * Get metadata, from a full URL path.
         * @param {string} resourceUrl - URL of the resource.
         */
        const getMetadataByUrl = function (resourceUrl) {
            return $http.get(UriBuilder().path(resourceUrl.split('/')).build(), { headers: { 'describe': 'application/json' } });
        }

        /**
         * Gets metadata, from full resource path.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const getMetadata = function (resourcePath) {
            const resourceUrl = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(resourceUrl, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        /**
         * Renames a file/folder.
         * @param {string} oldName - Old name of the resource.
         * @param {string} newName - New name of the resource.
         * @param {string} resourcePath - Resource path, including workspace name.
         */
        const rename = function (oldName, newName, resourcePath) { // TODO: back-end should return full path.
            const pathSegments = resourcePath.split('/').slice(1);
            const workspace = pathSegments.shift();
            if (pathSegments.length >= 1) {
                const url = UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(workspace).path('rename').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        sources: [UriBuilder().path(pathSegments).path(oldName).build()],
                        target: UriBuilder().path(pathSegments).path(newName).build(),
                        sourceWorkspace: workspace,
                        targetWorkspace: workspace
                    }).then((response) => {
                        for (let i = 0; i < response.data.length; i++) {
                            response.data[i].from = `/${workspace}/${response.data[i].from}`;
                            response.data[i].to = `/${workspace}/${response.data[i].to}`;
                        }
                        resolve(response);
                    }, (response) => {
                        reject(response);
                    });
                });
            }
        }.bind(this);

        /**
         * Deletes a file/folder.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const remove = function (resourcePath) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.delete(url, { headers: { 'Dirigible-Editor': 'Workspace' } });
        }.bind(this);

        /**
         * Copies a file/folder.
         * @param {string|Array.<string>} sourcePath - Folder name.
         * @param {string} targetPath - Full target path, including workspace name.
         */
        const copy = function (sourcePath, targetPath) {
            const sources = [];
            let sourceWorkspace;
            if (Array.isArray(sourcePath)) {
                for (let i = 0; i < sourcePath.length; i++) {
                    const source = sourcePath[i].split('/');
                    sourceWorkspace = source.splice(1, 1)[0];
                    sources.push(source.join('/'));
                }
            } else {
                const source = sourcePath.split('/');
                sourceWorkspace = source.splice(1, 1)[0];
                sources.push(source.join('/'));
            }
            const targetSegments = targetPath.split('/');
            const targetWorkspace = targetSegments.splice(1, 1)[0];
            const url = UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(targetWorkspace).path('copy').build();
            return $http.post(url, {
                sourceWorkspace: sourceWorkspace,
                sources: sources,
                targetWorkspace: targetWorkspace, // TODO: Remove the target workspace from the body as we already have it it the URL
                target: targetSegments.join('/'),
            });
        }.bind(this);

        /**
         * Moves a file/folder.
         * @param {string|Array.<string>} sourcePath - Folder name.
         * @param {string} targetPath - Full target path, including workspace name.
         */
        const move = function (sourcePath, targetPath) { // TODO: back-end should return full path.
            // TODO: Move to another workspace
            const sources = [];
            let sourceWorkspace;
            if (Array.isArray(sourcePath)) {
                for (let i = 0; i < sourcePath.length; i++) {
                    const source = sourcePath[i].split('/');
                    sourceWorkspace = source.splice(1, 1)[0];
                    sources.push(source.join('/'));
                }
            } else {
                const source = sourcePath.split('/');
                sourceWorkspace = source.splice(1, 1)[0];
                sources.push(source.join('/'));
            }
            const targetSegments = targetPath.split('/');
            const targetWorkspace = targetSegments.splice(1, 1)[0];
            const url = UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(sourceWorkspace).path('move').build();
            return new Promise((resolve, reject) => {
                $http.post(url, {
                    sources: sources,
                    target: targetSegments.join('/'),
                    sourceWorkspace: sourceWorkspace, // TODO: Remove the source workspace from the body as we already have it it the URL
                    targetWorkspace: targetWorkspace
                }).then((response) => {
                    for (let i = 0; i < response.data.length; i++) {
                        response.data[i].from = `/${sourceWorkspace}/${response.data[i].from}`;
                        response.data[i].to = `/${targetWorkspace}/${response.data[i].to}`;
                    }
                    resolve(response);
                }, (response) => {
                    reject(response);
                });
            });
        }.bind(this);

        /**
         * Creates a new file.
         * @param {string} name - File name.
         * @param {string} targetPath - Full target path, including workspace name.
         * @param {string} [contnet] - File contents.
         */
        const createFile = function (name, targetPath, content = '') {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(targetPath.split('/')).path(name).build();
            return $http.post(url, content, { headers: { 'Dirigible-Editor': 'Workspace', 'Content-Type': 'text/plain' } })
                .then(function successCallback(response) {
                    return {
                        status: response.status,
                        data: response.config.url,
                    };
                }, function errorCallback(response) {
                    console.error('Workspace service:', response);
                    return { status: response.status };
                });
        }.bind(this);

        /**
         * Creates a new folder.
         * @param {string} name - Folder name.
         * @param {string} targetPath - Full target path, including workspace name.
         */
        const createFolder = function (name, targetPath) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(targetPath.split('/')).path(name).path('/').build();
            return $http.post(url, content = '', { headers: { 'Dirigible-Editor': 'Workspace', 'Content-Type': 'text/plain' } })
                .then(function successCallback(response) {
                    return {
                        status: response.status,
                        data: response.config.url,
                    };
                }, function errorCallback(response) {
                    console.error('Workspace service:', response);
                    return { status: response.status };
                });
        }.bind(this);

        /**
         * Creates a workspace.
         * @param {string} workspace - Workspace name.
         */
        const createWorkspace = function (workspace) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).build();
            return $http.post(url, {});
        }.bind(this);

        /**
         * Deletes a workspace.
         * @param {string} workspace - Workspace name.
         */
        const deleteWorkspace = function (workspace) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).build();
            return $http.delete(url, { headers: { 'Dirigible-Editor': 'Workspace' } });
        }.bind(this);

        /**
         * Creates a project inside a workspace.
         * @param {string} workspace - Workspace name.
         * @param {string} projectName - Project name.
         */
        const createProject = function (workspace, projectName) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(projectName).build();
            return $http.post(url, {});
        }.bind(this);

        // TODO: Check if this API works
        // const linkProject = function (workspace, projectName, path) {
        //     const url = UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(workspace).path('linkProject').build();
        //     return $http.post(url, {
        //         sources: Array.isArray(projectName) ? projectName : [projectName],
        //         target: path
        //     });
        // }.bind(this);

        /**
         * Deletes a project inside a workspace.
         * @param {string} workspace - Workspace name.
         * @param {string} projectName - Project name.
         */
        const deleteProject = function (workspace, projectName) {
            const url = UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(projectName).build();
            return $http.delete(url, { headers: { 'Dirigible-Editor': 'Workspace' } });
        }.bind(this);

        /**
         * Performs a search.
         * @param {string} searchPath - Full path, including the workspace, where the search should be performed.
         * @param {string} searchTerm - File content to search for.
         * @returns - List of matching results.
         */
        const search = function (searchPath, searchTerm) {
            const url = UriBuilder().path(this.workspaceSearchServiceUrl.split('/')).path(searchPath.split('/')).build();
            return $http.post(url, searchTerm);
        }.bind(this);

        /**
         * Creates a direct web path to a resource.
         * @param {string} resourcePath - Full resource path, including the workspace.
         * @returns - URL encoded web path without the domain.
         */
        const getFullURL = function (resourcePath) {
            return UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath ? resourcePath.split('/') : '').build();
        }.bind(this);

        return {
            setWorkspace: setWorkspace,
            getCurrentWorkspace: getCurrentWorkspace,
            listWorkspaceNames: listWorkspaceNames,
            list: list,
            resourceExists: resourceExists,
            loadContent: loadContent,
            saveContent: saveContent,
            getMetadata: getMetadata,
            getMetadataByUrl: getMetadataByUrl,
            rename: rename,
            remove: remove,
            copy: copy,
            move: move,
            createFile: createFile,
            createFolder: createFolder,
            createWorkspace: createWorkspace,
            deleteWorkspace: deleteWorkspace,
            createProject: createProject,
            // linkProject: linkProject,
            deleteProject: deleteProject,
            search: search,
            getFullURL: getFullURL,
        };
    }];
});