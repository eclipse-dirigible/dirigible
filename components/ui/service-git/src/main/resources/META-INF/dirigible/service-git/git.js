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
angular.module('GitService', [])
    .provider('GitService', function GitApiProvider() {
        this.gitServiceUrl = '/services/ide/git';
        this.$get = ['$http', function gitApiFactory($http) {
            function getErrorMessage(response) {
                if (response && response.data) {
                    if (typeof response.data === "string") {
                        const data = JSON.parse(response.data);
                        if (data.error && data.message) return `${data.error}: ${data.message}`;
                    }
                    else return `${response.data.error}: ${response.data.message}`;
                } else return 'Check console for more information.';
            }

            const listProjects = function (resourcePath) {
                if (resourcePath !== undefined && !(typeof resourcePath === 'string'))
                    throw Error("listProjects: resourcePath must be a path");
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(resourcePath.split('/')).build();
                return new Promise((resolve, reject) => {
                    $http.get(url + '/', { headers: { describe: 'application/json' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const cloneRepository = function (workspace, repository, branch = '', username, password) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path('clone').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        repository: repository,
                        branch: branch,
                        publish: true,
                        username: username,
                        password: btoa(password),
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const pullRepository = function (workspace, project, branch = '', username, password) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('pull').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        publish: true,
                        username: username,
                        password: btoa(password),
                        branch: branch,
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            // This has to be implemented in the back-end. See issue #1984
            const pullRepositories = async function (workspace, projects, username, password, callback) {
                let response = { status: 200, message: '' };
                for (let i = 0; i < projects.length; i++) {
                    await pullRepository(workspace, projects[i], '', username, password).then((resp) => {
                        response.status = resp.status;
                    }, () => {
                        response.message = resp.message;
                    });
                }
                callback(response);
            }.bind(this);

            const pushRepository = function (workspace, project, branch = '', username, email, password) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('push').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        username: username,
                        password: btoa(password),
                        email: email,
                        branch: branch,
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const pushAllRepositories = function (workspace, username, email, password, autoAdd = false, autoCommit = false) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path('push').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        username: username,
                        password: btoa(password),
                        email: email,
                        autoAdd: autoAdd,
                        autoCommit: autoCommit,
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const resetRepository = function (workspace, project) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('reset').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {}).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const importProjects = function (workspace, repository) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(repository).path('import').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {}).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const deleteRepository = function (workspace, repositoryName, unpublish) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(repositoryName).path("delete").build();
                return new Promise((resolve, reject) => {
                    $http.delete(`${url}?unpublish=${unpublish}`).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const shareProject = function (
                workspace,
                project,
                repository,
                branch,
                commitMessage,
                username,
                password,
                email,
                shareInRootFolder,
            ) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('share').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        project: project,
                        repository: repository,
                        branch: branch,
                        commitMessage: commitMessage,
                        username: username,
                        password: btoa(password),
                        email: email,
                        shareInRootFolder: shareInRootFolder,
                    }).then((response) => {
                        resolve(response);
                    }, function errorCallback(response) {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const checkoutBranch = function (workspace, project, branch, username, password) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('checkout').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        project: project,
                        branch: branch,
                        username: username,
                        password: btoa(password),
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const createBranch = function (workspace, project, branch, local = true, username, password) {
                let branchType = 'local';
                let body = {};
                if (!local) {
                    branchType = 'remote';
                    body['username'] = username;
                    body['password'] = btoa(password);
                }
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('branches')
                    .path(branchType)
                    .path(branch)
                    .build();
                return new Promise((resolve, reject) => {
                    $http.post(url, body).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const deleteBranch = function (workspace, project, branch, local = true, username, password) {
                let branchType = 'local';
                let body = {};
                if (!local) {
                    branchType = 'remote';
                    body['username'] = username;
                    body['password'] = btoa(password);
                }
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('branches')
                    .path(branchType)
                    .path(branch)
                    .build();
                return new Promise((resolve, reject) => {
                    $http.delete(url, { data: body, headers: { 'Content-Type': 'application/json;charset=utf-8' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const commit = function (
                workspace,
                project,
                commitMessage,
                username,
                password,
                email,
                branch,
            ) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('commit').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        commitMessage: commitMessage,
                        username: username,
                        password: btoa(password),
                        email: email,
                        branch: branch,
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const push = function (
                workspace,
                project,
                commitMessage,
                username,
                password,
                email,
                branch,
            ) {
                const url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('push').build();
                return new Promise((resolve, reject) => {
                    $http.post(url, {
                        commitMessage: commitMessage,
                        username: username,
                        password: btoa(password),
                        email: email,
                        branch: branch,
                        autoAdd: false,
                        autoCommit: false,
                    }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const branches = function (workspace, project, local = true) {
                let branchType = 'local';
                if (!local) branchType = 'remote';
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('branches')
                    .path(branchType)
                    .build();
                return new Promise((resolve, reject) => {
                    $http.get(url, { headers: { describe: 'application/json' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const getUnstagedFiles = function (workspace, project) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('unstaged')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.get(url, { headers: { describe: 'application/json' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const getStagedFiles = function (workspace, project) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('staged')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.get(url, { headers: { describe: 'application/json' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const getOriginUrls = function (workspace, project) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('origin-urls')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.get(url, { headers: { describe: 'application/json' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const setFetchUrl = function (workspace, project, fetchUrl) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('fetch-url')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.post(url, { url: fetchUrl }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const setPushUrl = function (workspace, project, pushUrl) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('push-url')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.post(url, { url: pushUrl }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const addToIndex = function (workspace, project, files) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('add')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.post(url, files.join(',')).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const revertFiles = function (workspace, project, files) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('revert')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.post(url, files.join(',')).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const removeFiles = function (workspace, project, files) {
                const url = UriBuilder()
                    .path(this.gitServiceUrl.split('/'))
                    .path(workspace)
                    .path(project)
                    .path('remove')
                    .build();
                return new Promise((resolve, reject) => {
                    $http.post(url, files.join(',')).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            /*
             * Returns the original file content in git and the modified file content
             */
            const getOriginalModified = function (workspace, project, resourcePath) {
                const url = `${UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).build()}/diff?path=${UriBuilder().path(resourcePath.split('/')).build(false)}`;
                return new Promise((resolve, reject) => {
                    $http.get(url, { headers: { describe: 'application/json' } }).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            const history = function (workspace, project, file) {
                let url = UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path('history').build();
                if (file) {
                    url += "?path=" + file;
                }
                return new Promise((resolve, reject) => {
                    $http.get(url).then((response) => {
                        resolve(response);
                    }, (response) => {
                        reject({ status: response.status, message: getErrorMessage(response) });
                    });
                });
            }.bind(this);

            return {
                listProjects: listProjects,
                cloneRepository: cloneRepository,
                pullRepository: pullRepository,
                pullRepositories: pullRepositories,
                pushRepository: pushRepository,
                pushAllRepositories: pushAllRepositories,
                resetRepository: resetRepository,
                importProjects: importProjects,
                deleteRepository: deleteRepository,
                shareProject: shareProject,
                checkoutBranch: checkoutBranch,
                createBranch: createBranch,
                deleteBranch: deleteBranch,
                commit: commit,
                push: push,
                branches: branches,
                getUnstagedFiles: getUnstagedFiles,
                getStagedFiles: getStagedFiles,
                getOriginUrls: getOriginUrls,
                setFetchUrl: setFetchUrl,
                setPushUrl: setPushUrl,
                addToIndex: addToIndex,
                revertFiles: revertFiles,
                removeFiles: removeFiles,
                getOriginalModified: getOriginalModified,
                history: history,
            };
        }];
    });
