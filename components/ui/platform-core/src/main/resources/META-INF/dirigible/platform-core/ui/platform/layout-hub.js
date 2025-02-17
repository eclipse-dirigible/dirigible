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
class LayoutHub extends MessageHubApi {
    constructor(layoutId, setId = false) {
        super();
        if (layoutId !== undefined && layoutId !== null) {
            this.layoutId = `.${layoutId}`;
            if (setId) window['layoutId'] = layoutId;
        } else {
            this.layoutId = window['layoutId'] ?? '';
            if (!this.layoutId) {
                let current = window;
                while (current !== top) {
                    current = current.parent;
                    if (current['layoutId']) {
                        this.layoutId = `.${current['layoutId']}`;
                    }
                }
            }
        }
    }

    /**
     * Returns a list of the currently opened views.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    getOpenedViews({ params } = {}) {
        const callbackTopic = `platform.layout${this.layoutId}.views.opened.${new Date().valueOf()}`;
        this.postMessage({
            topic: `platform.layout${this.layoutId}.views.opened`,
            data: {
                topic: callbackTopic,
                params: params,
            },
        });
        return new Promise((resolve) => {
            const callbackListener = this.addMessageListener({
                topic: callbackTopic,
                handler: (data) => {
                    this.removeMessageListener(callbackListener);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Triggered when a list of opened views is requested.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onGetOpenedViews(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.views.opened`, handler: handler });
    }

    /**
     * Returns { isOpen: true/false } object.
     * @param {string} id - View id.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    isViewOpen({ id, params } = {}) {
        const callbackTopic = `platform.layout${this.layoutId}.views.is-open.${new Date().valueOf()}`;
        this.postMessage({
            topic: `platform.layout${this.layoutId}.views.is-open`,
            data: {
                topic: callbackTopic,
                id: id,
                params: params,
            },
        });
        return new Promise((resolve) => {
            const callbackListener = this.addMessageListener({
                topic: callbackTopic,
                handler: (data) => {
                    this.removeMessageListener(callbackListener);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Triggered when a check for an opened view is requested.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onIsViewOpen(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.views.is-open`, handler: handler });
    }

    /**
     * Opens a view inside the layout. If the view is already opened, it will become visible.
     * @param {string} id - View id.
     * @param {object} [params] - Custom parameters that will be set to the view's data-parameters attribute.
     */ // @ts-ignore
    openView({ id, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.view.open`,
            data: {
                id: id,
                params: params,
            }
        });
    }

    /**
     * Triggered when a view should be opened.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onOpenView(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.view.open`, handler: handler });
    }

    /**
     * Tells a view that it should gain focus from the inside.
     * @param {string} id - View id.
     * @param {object} [params] - Custom parameters that will be set to the view's data-parameters attribute.
     */
    focusView({ id, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.view.focus`,
            data: {
                id: id,
                params: params,
            }
        });
    }

    /**
     * Triggered when a view should gain focus.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFocusView(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.view.focus`, handler: handler });
    }

    /**
     * Closes a view inside the layout.
     * @param {string} id - View id.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    closeView({ id, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.view.close`,
            data: {
                id: id,
                params: params,
            }
        });
    }

    /**
     * Triggered when a view should be closed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onCloseView(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.view.close`, handler: handler });
    }

    /**
     * Closes all other views inside the layout.
     * @param {string} id - View id.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    closeOtherViews({ id, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.view.close.others`,
            data: {
                id: id,
                params: params,
            }
        });
    }

    /**
     * Triggered when other views should be closed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onCloseOtherViews(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.view.close.others`, handler: handler });
    }

    /**
     * Closes all views inside the layout.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    closeAllViews({ params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.view.close.all`,
            data: {
                params: params
            }
        });
    }

    /**
     * Triggered when all views should be closed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onCloseAllViews(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.view.close.all`, handler: handler });
    }

    /**
     * Marks a view in the layout as dirty.
     * @param {string} id - View id.
     * @param {boolean} dirty - Dirty state.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    setViewDirty({ id, dirty, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.view.dirty`,
            data: {
                id: id,
                dirty: dirty,
                params: params,
            }
        });
    }

    /**
     * Triggered when a vie's dirty state should be changed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onViewDirty(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.view.dirty`, handler: handler });
    }

    /**
     * Sends a message containing information on which file and editor has to be opened.
     * @param {string} path - Full file path, including file name.
     * @param {string} contentType - The file content type.
     * @param {string} [editorId] - The ID of the preffered editor.
     * @param {Object.<any, any>} [params] - Extra parameters that will be passed to the view parameters of the editor.
     */ // @ts-ignore
    openEditor({ path, contentType, editorId, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.open`,
            data: {
                name: path.substring(path.lastIndexOf('/') + 1, path.length),
                path: path,
                contentType: contentType,
                editorId: editorId,
                params: params,
            }
        });
    }

    /**
     * Triggered when an editor has to be opened.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onOpenEditor(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.open`, handler: handler });
    }

    /**
     * Sends a message containing information on which editor has to be closed.
     * @param {string} path - Full file path, including file name.
     * @param {Object.<any, any>} [params] - Extra parameters.
     */ // @ts-ignore
    closeEditor({ path, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.close`,
            data: {
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when an editor has to be closed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onCloseEditor(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.close`, handler: handler });
    }

    /**
     * Sends a message when all editors should be closed.
     * @param {Object.<any, any>} [params] - Extra parameters.
     */ // @ts-ignore
    closeAllEditors({ params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.close.all`,
            data: {
                params: params,
            }
        });
    }

    /**
     * Triggered when all editors have to be closed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onCloseAllEditors(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.close.all`, handler: handler });
    }

    /**
     * Tells an editor that it has been focused.
     * @param {string} id - Editor tab id.
     * @param {string} [path] - Full file path, including file name.
     * @param {object} [params] - Custom parameters that will be set to the view's data-parameters attribute.
     */
    focusEditor({ id, path, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editor.focus`,
            data: {
                id: id,
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when an editor gains focus.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFocusEditor(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editor.focus`, handler: handler });
    }

    /**
     * Sends a message containing information on which editor has to be set to dirty.
     * @param {string} path - Full file path, including file name.
     * @param {boolean} dirty - Editor dirty state.
     * @param {Object.<any, any>} [params] - Extra parameters.
     */ // @ts-ignore
    setEditorDirty({ path, dirty, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.set-dirty`,
            data: {
                path: path,
                dirty: dirty,
                params: params,
            }
        });
    }

    /**
     * Triggered when an editor has to be set as dirty.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onSetEditorDirty(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.set-dirty`, handler: handler });
    }

    /**
     * Gets the dirty state of an editor.
     * @param {string} path - Full file path, including file name.
     * @param {Object.<any, any>} [params] - Extra parameters.
     */ // @ts-ignore
    isEditorDirty({ path, params } = {}) {
        const callbackTopic = `platform.layout${this.layoutId}.editors.is-dirty.${new Date().valueOf()}`;
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.is-dirty`,
            data: {
                topic: callbackTopic,
                path: path,
                params: params,
            },
        });
        return new Promise((resolve) => {
            const callbackListener = this.addMessageListener({
                topic: callbackTopic,
                handler: (data) => {
                    this.removeMessageListener(callbackListener);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Triggered when an editor's dirty state is requested.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onIsEditorDirty(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.is-dirty`, handler: handler });
    }

    /**
     * Checks if an editor is open.
     * @param {string} path - Full file path, including file name.
     * @param {Object.<any, any>} [params] - Extra parameters.
     */ // @ts-ignore
    isEditorOpen({ path, params } = {}) {
        const callbackTopic = `platform.layout${this.layoutId}.editors.is-open.${new Date().valueOf()}`;
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.is-open`,
            data: {
                topic: callbackTopic,
                path: path,
                params: params,
            },
        });
        return new Promise((resolve) => {
            const callbackListener = this.addMessageListener({
                topic: callbackTopic,
                handler: (data) => {
                    this.removeMessageListener(callbackListener);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Triggered when performing a check for an open editor.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onIsEditorOpen(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.is-open`, handler: handler });
    }

    /**
     * Gets all file paths from the currently opened editors.
     * @param {string} basePath - If provided, it will only return files with a matching base path.
     * @return {Promise} - Returns a promise with a list of paths as a parameter.
     */ // @ts-ignore
    getCurrentlyOpenedEditors({ basePath = '/' } = {}) {
        const callbackTopic = `platform.layout${this.layoutId}.editors.opened.${new Date().valueOf()}`;
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.opened`,
            data: {
                basePath: basePath,
                topic: callbackTopic,
            }
        });
        return new Promise((resolve) => {
            const callbackListener = this.addMessageListener({
                topic: callbackTopic,
                handler: (data) => {
                    this.removeMessageListener(callbackListener);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Triggered when a list of opened files is requested.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onGetCurrentlyOpenedEditors(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.opened`, handler: handler });
    }

    /**
     * Tells an editor that it should reload its view parameters.
     * @param {string} path - Full file path, including file name.
     * @param {object} [params] - Custom parameters.
     */ // @ts-ignore
    reloadEditorParams({ path, params } = {}) {
        this.postMessage({
            topic: `platform.layout${this.layoutId}.editors.reload-params`,
            data: {
                path: path,
                params: params,
            }
        });
    }

    /**
     * Triggered when an editor should reload its view parameters.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onReloadEditorParams(handler) {
        return this.addMessageListener({ topic: `platform.layout${this.layoutId}.editors.reload-params`, handler: handler });
    }
}