/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
class WorkspaceHub extends MessageHubApi {
    /**
     * Sends a message containing information on which workspace has been changed.
     * @param {string} workspace - Name of the changed workspace.
     * @param {Object.<any, any>} params - Any extra parameters.
     */ // @ts-ignore
    announceWorkspaceChanged({ workspace, params } = {}) {
        this.postMessage({
            topic: 'platform.workspace.changed',
            data: {
                workspace: workspace,
                params: params,
            }
        });
    }

    /**
     * Triggered when a workspace has been changed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onWorkspaceChanged(handler) {
        return this.addMessageListener({ topic: 'platform.workspace.changed', handler: handler });
    }

    /**
     * Sends a message containing information on which file has been saved.
     * @param {string} path - Full file path, including file name.
     * @param {string} [status] - Git status of the file.
     * @param {string} [contentType] - File content type.
     */ // @ts-ignore
    announceFileSaved({ path, status, contentType } = {}) {
        this.postMessage({
            topic: 'platform.files.saved',
            data: {
                name: path.substring(path.lastIndexOf('/') + 1),
                path: path,
                status: status,
                contentType: contentType,
            }
        });
    }

    /**
     * Triggered when a file has been saved.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFileSaved(handler) {
        return this.addMessageListener({ topic: 'platform.files.saved', handler: handler });
    }

    /**
     * Sends a message containing information on which file should be saved.
     * @param {string} path - Full file path, including file name.
     * @param {string} [params] - Extra parameters.
     */ // @ts-ignore
    saveFile({ path, params } = {}) {
        this.postMessage({
            topic: 'platform.file.save',
            data: {
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when a file has to be saved.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onSaveFile(handler) {
        return this.addMessageListener({ topic: 'platform.file.save', handler: handler });
    }

    /**
     * Tells all open editors to save their content.
     */ // @ts-ignore
    saveAll() { this.triggerEvent('platform.files.save.all'); }

    /**
     * Triggered when all files should be saved.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onSaveAll(handler) {
        return this.addMessageListener({ topic: 'platform.files.save.all', handler: handler });
    }

    /**
     * Sends a message containing information on which file, folder or project has been published.
     * @param {string} [path] - Full file, folder or project path, including name.
     * @param {Object.<any, any>} [params] - Extra parameters that will be passed the listener.
     */ // @ts-ignore
    announcePublished({ path, params } = {}) {
        this.postMessage({
            topic: 'platform.publisher.published',
            data: {
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when a file, folder or project has been published.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onPublished(handler) {
        return this.addMessageListener({ topic: 'platform.publisher.published', handler: handler });
    }

    /**
     * Sends a message containing information on which file, folder or project has been unpublished.
     * @param {string} [path] - Full file, folder or project path, including name.
     * @param {Object.<any, any>} [params] - Extra parameters that will be passed the listener.
     */ // @ts-ignore
    announceUnpublished({ path, params } = {}) {
        this.postMessage({
            topic: 'platform.publisher.unpublished',
            data: {
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when a file, folder or project has been unpublished.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onUnpublished(handler) {
        return this.addMessageListener({ topic: 'platform.publisher.unpublished', handler: handler });
    }

    /**
     * Sends a message containing information on which file has been selected.
     * @param {string} path - Full file path, including file name.
     * @param {string} contentType - The file content type.
     * @param {Object.<any, any>} [params] - Extra parameters that will be passed the listener.
     */ // @ts-ignore
    announceFileSelected({ path, contentType, params } = {}) {
        this.postMessage({
            topic: 'platform.files.selected',
            data: {
                name: path.substring(path.lastIndexOf('/') + 1, path.length),
                path: path,
                contentType: contentType,
                params: params
            }
        });
    }

    /**
     * Triggered when a file has been deleted.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFileSelected(handler) {
        return this.addMessageListener({ topic: 'platform.files.selected', handler: handler });
    }

    /**
     * Sends a message containing information on which file has been deleted.
     * @param {string} path - Full file path, including file name.
     * @param {Object.<any, any>} [params] - Extra parameters that will be passed the listener.
     */ // @ts-ignore
    announceFileDeleted({ path, params } = {}) {
        this.postMessage({
            topic: 'platform.files.deleted',
            data: {
                // name: path.substring(path.lastIndexOf('/') + 1, path.length),
                path: path,
                params: params
            }
        });
    }

    /**
     * Triggered when a file has been deleted.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFileDeleted(handler) {
        return this.addMessageListener({ topic: 'platform.files.deleted', handler: handler });
    }

    /**
     * Sends a message containing information on which file has been renamed.
     * @param {string} oldPath - Old file path, including file name.
     * @param {string} newPath - New file path, including file name.
     * @param {string} contentType - The file content type.
     */ // @ts-ignore
    announceFileRenamed({ oldPath, newPath, contentType } = {}) {
        this.postMessage({
            topic: 'platform.files.renamed',
            data: {
                // oldName: oldPath.substring(oldPath.lastIndexOf('/') + 1, oldPath.length),
                newName: newPath.substring(newPath.lastIndexOf('/') + 1, newPath.length),
                oldPath: oldPath,
                newPath: newPath,
                contentType: contentType,
            }
        });
    }

    /**
     * Triggered when a file has been renamed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFileRenamed(handler) {
        return this.addMessageListener({ topic: 'platform.files.renamed', handler: handler });
    }

    /**
     * Sends a message containing information on which file has been moved.
     * @param {string} oldPath - Old file path, including file name.
     * @param {string} newPath - New file path, including file name.
     */ // @ts-ignore
    announceFileMoved({ oldPath, newPath } = {}) {
        this.postMessage({
            topic: 'platform.files.moved',
            data: {
                // oldName: oldPath.substring(oldPath.lastIndexOf('/') + 1, oldPath.length),
                newName: newPath.substring(newPath.lastIndexOf('/') + 1, newPath.length),
                oldPath: oldPath,
                newPath: newPath,
            }
        });
    }

    /**
     * Triggered when a file has been moved.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFileMoved(handler) {
        return this.addMessageListener({ topic: 'platform.files.moved', handler: handler });
    }

    /**
     * Sends a message containing information on which folder has been deleted.
     * @param {string} path - Full folder path, including folder name.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    announceFolderDeleted({ path, params } = {}) {
        this.postMessage({
            topic: 'platform.folders.deleted',
            data: {
                // name: path.substring(path.lastIndexOf('/') + 1, path.length),
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when a folder has been deleted.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFolderDeleted(handler) {
        return this.addMessageListener({ topic: 'platform.folders.deleted', handler: handler });
    }

    /**
     * Sends a message containing information on which folder has been renamed.
     * @param {string} oldPath - Old folder path, including folder name.
     * @param {string} newPath - New folder path, including folder name.
     * @param {string} contentType - The folder content type.
     */ // @ts-ignore
    announceFolderRenamed({ oldPath, newPath, contentType } = {}) {
        this.postMessage({
            topic: 'platform.folders.renamed',
            data: {
                // oldName: oldPath.substring(oldPath.lastIndexOf('/') + 1, oldPath.length),
                // newName: newPath.substring(newPath.lastIndexOf('/') + 1, newPath.length),
                oldPath: oldPath,
                newPath: newPath,
                contentType: contentType,
            }
        });
    }

    /**
     * Triggered when a folder has been renamed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFolderRenamed(handler) {
        return this.addMessageListener({ topic: 'platform.folders.renamed', handler: handler });
    }

    /**
     * Sends a message containing information on which folder has been moved.
     * @param {string} oldPath - Old folder path, including folder name.
     * @param {string} newPath - New folder path, including folder name.
     */ // @ts-ignore
    announceFolderMoved({ oldPath, newPath } = {}) {
        this.postMessage({
            topic: 'platform.folders.moved',
            data: {
                oldPath: oldPath,
                newPath: newPath,
            }
        });
    }

    /**
     * Triggered when a file has been moved.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFolderMoved(handler) {
        return this.addMessageListener({ topic: 'platform.folders.moved', handler: handler });
    }

    /**
     * Sends a message containing information on which project has been deleted.
     * @param {string} project - Project name.
     * @param {string} workspace - Workspace name.
     */ // @ts-ignore
    announceProjectDeleted({ project, workspace } = {}) {
        this.postMessage({
            topic: 'platform.projects.deleted',
            data: {
                project: project,
                workspace: workspace,
            }
        });
    }

    /**
     * Triggered when a project has been deleted.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onProjectDeleted(handler) {
        return this.addMessageListener({ topic: 'platform.projects.deleted', handler: handler });
    }

    /**
     * Sends a message containing information on which workspace has been created.
     * @param {string} workspace - Workspace name.
     */ // @ts-ignore
    announceWorkspaceCreated({ workspace } = {}) {
        this.postMessage({
            topic: 'platform.workspaces.created',
            data: {
                workspace: workspace,
            }
        });
    }

    /**
     * Triggered when a workspace has been created.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onWorkspaceCreated(handler) {
        return this.addMessageListener({ topic: 'platform.workspaces.created', handler: handler });
    }

    /**
     * Sends a message containing information on which workspace has been modified.
     * @param {string} workspace - Workspace name.
     */ // @ts-ignore
    announceWorkspaceModified({ workspace } = {}) {
        this.postMessage({
            topic: 'platform.workspaces.modified',
            data: {
                workspace: workspace,
            }
        });
    }

    /**
     * Triggered when a workspace has been modified.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onWorkspaceModified(handler) {
        return this.addMessageListener({ topic: 'platform.workspaces.modified', handler: handler });
    }

    /**
     * Sends a message containing information on which workspace has been deleted.
     * @param {string} workspace - Workspace name.
     */ // @ts-ignore
    announceWorkspaceDeleted({ workspace } = {}) {
        this.postMessage({
            topic: 'platform.workspaces.deleted',
            data: {
                workspace: workspace,
            }
        });
    }

    /**
     * Triggered when a workspace has been created.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onWorkspaceDeleted(handler) {
        return this.addMessageListener({ topic: 'platform.workspaces.deleted', handler: handler });
    }
}