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
angular.module('ideWorkspace', [])
    .provider('workspaceApi', function WorkspaceApiProvider() {
        this.workspacesServiceUrl = '/services/ide/workspaces';
        this.workspaceManagerServiceUrl = '/services/ide/workspace';
        this.workspaceSearchServiceUrl = '/services/ide/workspace-search';
        this.$get = ['$http', function workspaceApiFactory($http) {
            const setWorkspace = function (workspaceName) {
                if (workspaceName !== undefined && !(typeof workspaceName === 'string'))
                    throw Error("setWorkspace: workspaceName must be an string");
                const workspace = { name: workspaceName };
                localStorage.setItem('DIRIGIBLE.workspace', JSON.stringify(workspace));
                return workspace;
            };

            const getCurrentWorkspace = function () {
                let storedWorkspace = JSON.parse(localStorage.getItem('DIRIGIBLE.workspace') || '{}');
                if (!('name' in storedWorkspace)) storedWorkspace = setWorkspace('workspace');
                return storedWorkspace;
            };

            const listWorkspaceNames = function () {
                return $http.get(this.workspacesServiceUrl)
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const load = function (resourcePath) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
                return $http.get(url, { headers: { 'describe': 'application/json' } })
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const resourceExists = function (resourcePath) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(resourcePath.split('/')).build();
                return $http.head(url)
                    .then(function successCallback(response) {
                        return { status: response.status };
                    }, function errorCallback(response) {
                        if (response.status !== 404)
                            console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const loadContent = async function (workspace, filePath) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(filePath.split('/')).build();
                return $http.get(url)
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const saveContent = async function (workspace, filePath, content) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(filePath.split('/')).build();
                return $http.put(url, content, { headers: { 'Content-Type': 'text/plain;charset=UTF-8' } })
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const getMetadata = function (resourceUrl) {
                return $http.get(resourceUrl, { headers: { 'describe': 'application/json' } })
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }

            const getMetadataByPath = function (workspace, path) {
                const resourceUrl = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(path.split('/')).build();
                return getMetadata(resourceUrl);
            }.bind(this);

            const rename = function (oldName, newName, path, workspaceName) {
                let pathSegments = path.split('/');
                pathSegments = pathSegments.slice(1);
                if (pathSegments.length >= 1) {
                    const url = new UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(workspaceName).path('rename').build();
                    return $http.post(url, {
                        sources: [new UriBuilder().path(pathSegments).path(oldName).build()],
                        target: new UriBuilder().path(pathSegments).path(newName).build(),
                        sourceWorkspace: workspaceName,
                        targetWorkspace: workspaceName
                    }).then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
                }
            }.bind(this);

            const remove = function (filepath) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(filepath.split('/')).build();
                return $http.delete(url, { headers: { 'Dirigible-Editor': 'Workspace' } })
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const copy = function (sourcePath, targetPath, sourceWorkspace, targetWorkspace) {
                const url = new UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(targetWorkspace).path('copy').build();
                return $http.post(url, {
                    sourceWorkspace: sourceWorkspace,
                    sources: Array.isArray(sourcePath) ? sourcePath : [sourcePath],
                    targetWorkspace: targetWorkspace,
                    target: (targetPath.endsWith('/') ? targetPath : targetPath + '/'),
                }).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Workspace service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            const move = function (sourcePath, targetPath, sourceWorkspace, targetWorkspace) {
                // TODO: Move to another workspace
                const url = new UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(sourceWorkspace).path('move').build();
                return $http.post(url, {
                    sources: Array.isArray(sourcePath) ? sourcePath : [sourcePath],
                    target: targetPath,
                    sourceWorkspace: sourceWorkspace,
                    targetWorkspace: targetWorkspace
                }).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Workspace service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            const createNode = function (name, targetPath, isDirectory, content = '') {
                let url = new UriBuilder().path((this.workspacesServiceUrl + targetPath).split('/')).path(name).build();
                if (isDirectory)
                    url += "/";
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

            const createWorkspace = function (workspace) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).build();
                return $http.post(url, {})
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const deleteWorkspace = function (workspace) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).build();
                return $http.delete(url, { headers: { 'Dirigible-Editor': 'Workspace' } })
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const createProject = function (workspace, projectName) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(projectName).build();
                return $http.post(url, {})
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const linkProject = function (workspace, projectName, path) {
                const url = new UriBuilder().path(this.workspaceManagerServiceUrl.split('/')).path(workspace).path('linkProject').build();
                return $http.post(url, {
                    sources: Array.isArray(sourcePath) ? projectName : [projectName],
                    target: path
                }).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Workspace service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            const deleteProject = function (workspace, projectName) {
                const url = new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(projectName).build();
                return $http.delete(url, { headers: { 'Dirigible-Editor': 'Workspace' } })
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const search = function (workspace, resourcePath, searchTerm) {
                const url = new UriBuilder().path(this.workspaceSearchServiceUrl.split('/')).path(workspace).path(resourcePath.split('/')).build();
                return $http.post(url, searchTerm)
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Workspace service:', response);
                        return { status: response.status };
                    });
            }.bind(this);

            const getFullURL = function (workspace, resourcePath) {
                return new UriBuilder().path(this.workspacesServiceUrl.split('/')).path(workspace).path(resourcePath ? resourcePath.split('/') : '').build();
            }.bind(this);

            return {
                setWorkspace: setWorkspace,
                getCurrentWorkspace: getCurrentWorkspace,
                listWorkspaceNames: listWorkspaceNames,
                load: load,
                resourceExists: resourceExists,
                loadContent: loadContent,
                saveContent: saveContent,
                getMetadata: getMetadata,
                getMetadataByPath: getMetadataByPath,
                rename: rename,
                remove: remove,
                copy: copy,
                move: move,
                createNode: createNode,
                createWorkspace: createWorkspace,
                deleteWorkspace: deleteWorkspace,
                createProject: createProject,
                linkProject: linkProject,
                deleteProject: deleteProject,
                search: search,
                getFullURL: getFullURL,
            };
        }];
    });
