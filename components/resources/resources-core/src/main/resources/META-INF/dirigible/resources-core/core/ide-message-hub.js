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
angular.module('ideMessageHub', [])
    .provider('messageHub', function MessageHubProvider() {
        this.eventIdPrefix = '';
        this.eventIdDelimiter = '.';
        this.$get = [function messageHubFactory() {
            const messageHub = new FramesMessageHub();
            const isNullOrUndefined = function (value) {
                if (value === null || value === undefined) return true;
                return false;
            };
            const isNullOrUndefinedOrEmpty = function (value) {
                if (value === null || value === undefined || value.trim() === '') return true;
                return false;
            };
            const trigger = function (eventId, absolute = false) {
                if (!eventId)
                    throw Error('eventId argument must be a valid string, identifying an existing event');
                if (!absolute && this.eventIdPrefix !== '') eventId = this.eventIdPrefix + this.eventIdDelimiter + eventId;
                messageHub.post({}, eventId);
            }.bind(this);
            const post = function (eventId, data, absolute = false) {
                if (!eventId)
                    throw Error('eventId argument must be a valid string, identifying an existing event');
                if (!absolute && this.eventIdPrefix !== "") eventId = this.eventIdPrefix + this.eventIdDelimiter + eventId;
                messageHub.post({ data: data }, eventId);
            }.bind(this);
            const onMessage = function (eventId, callbackFunc, absolute = false) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                if (!absolute && this.eventIdPrefix !== "") eventId = this.eventIdPrefix + this.eventIdDelimiter + eventId;
                return messageHub.subscribe(callbackFunc, eventId);
            }.bind(this);
            const showStatusBusy = function (message) {
                if (!message)
                    throw Error("Status: you must provide a message");
                messageHub.post({
                    message: message,
                }, 'ide.status.busy');
            };
            const hideStatusBusy = function () {
                messageHub.post({
                    message: '',
                }, 'ide.status.busy');
            };
            const setStatusMessage = function (message) {
                if (isNullOrUndefinedOrEmpty(message))
                    throw Error("Status: you must provide a message");
                messageHub.post({
                    message: message,
                }, 'ide.status.message');
            };
            const setStatusError = function (message) {
                if (isNullOrUndefinedOrEmpty(message))
                    throw Error("Status: you must provide a message");
                messageHub.post({
                    message: message,
                }, 'ide.status.error');
            };
            const setStatusCaret = function (label) {
                if (isNullOrUndefined(label))
                    throw Error("Status: you must provide a label");
                messageHub.post({
                    text: label,
                }, 'ide.status.caret');
            };
            const showAlert = function (title, message, type) {
                if (isNullOrUndefinedOrEmpty(message))
                    throw Error("Alert: you must provide a message");
                if (isNullOrUndefinedOrEmpty(title))
                    throw Error("Alert: you must provide a title");
                messageHub.post({
                    title: title,
                    message: message,
                    type: type
                }, 'ide.alert');
            };
            const showAlertSuccess = function (title, message) {
                showAlert(title, message, "success");
            };
            const showAlertInfo = function (title, message) {
                showAlert(title, message, "info");
            };
            const showAlertWarning = function (title, message) {
                showAlert(title, message, "warning");
            };
            const showAlertError = function (title, message) {
                showAlert(title, message, "error");
            };
            const showDialog = function (
                title = "",
                body = "",
                buttons = [{
                    id: "b1",
                    type: "normal", // normal, emphasized, negative, transparent
                    label: "Ok",
                }],
                callbackTopic = null,
                loader = false,
                header = "",
                subheader = "",
                footer = ""
            ) {
                if (buttons.length === 0)
                    throw Error("Dialog: There must be at least one button");
                messageHub.post({
                    header: header,
                    subheader: subheader,
                    title: title,
                    body: body,
                    footer: footer,
                    loader: loader,
                    buttons: buttons,
                    callbackTopic: callbackTopic
                }, 'ide.dialog');
            };
            const showDialogAsync = function (
                title = "",
                body = "",
                buttons = [{
                    id: "b1",
                    type: "normal", // normal, emphasized, negative, transparent
                    label: "Ok",
                }],
                loader = false,
                header = "",
                subheader = "",
                footer = ""
            ) {
                return new Promise((resolve, reject) => {
                    if (buttons.length === 0)
                        reject(new Error("Dialog: There must be at least one button"));

                    const callbackTopic = `ide.dialog.${new Date().valueOf()}`;

                    messageHub.post({
                        header: header,
                        subheader: subheader,
                        title: title,
                        body: body,
                        footer: footer,
                        loader: loader,
                        buttons: buttons,
                        callbackTopic: callbackTopic
                    }, 'ide.dialog');

                    const handler = messageHub.subscribe(function (msg) {
                        messageHub.unsubscribe(handler);
                        resolve(msg);
                    }, callbackTopic);
                });
            };
            const showFormDialog = function (
                id,
                title = "",
                items = [],
                buttons = [{
                    id: "b1",
                    type: "normal", // normal, emphasized, transparent
                    label: "Ok",
                }],
                callbackTopic = null,
                loadingMessage = "",
                header = "",
                subheader = "",
                footer = "",
            ) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Form Dialog: You must specify a dialog id");
                if (items.length === 0)
                    throw Error("Form Dialog: There must be at least one form item");
                if (buttons.length === 0)
                    throw Error("Form Dialog: There must be at least one button");
                if (isNullOrUndefinedOrEmpty(callbackTopic))
                    throw Error("Form Dialog: There must be a callback topic");
                messageHub.post({
                    id: id,
                    header: header,
                    subheader: subheader,
                    title: title,
                    items: items,
                    loadingMessage: loadingMessage,
                    footer: footer,
                    buttons: buttons,
                    callbackTopic: callbackTopic,
                }, 'ide.formDialog.show');
            };
            const updateFormDialog = function (
                id,
                items = [],
                loadingMessage,
                subheader = "",
                footer,
            ) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Form Dialog: You must specify a dialog id");
                if (items.length === 0)
                    throw Error("Form Dialog: There must be at least one form item");
                messageHub.post({
                    id: id,
                    subheader: subheader,
                    footer: footer,
                    items: items,
                    loadingMessage: loadingMessage,
                }, 'ide.formDialog.update');
            };
            const hideFormDialog = function (id) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Form Dialog: You must specify a dialog id");
                messageHub.post({ id: id }, 'ide.formDialog.hide');
            };
            const showLoadingDialog = function (
                id,
                title = "",
                status = ""
            ) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Loading Dialog: You must specify a dialog id");
                messageHub.post({
                    id: id,
                    title: title,
                    status: status
                }, 'ide.loadingDialog.show');
            };
            const updateLoadingDialog = function (
                id,
                status = "",
            ) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Loading Dialog: You must specify a dialog id");
                messageHub.post({
                    id: id,
                    status: status,
                }, 'ide.loadingDialog.update');
            };
            const hideLoadingDialog = function (id) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Loading Dialog: You must specify a dialog id");
                messageHub.post({ id: id }, 'ide.loadingDialog.hide');
            };
            const showBusyDialog = function (
                id,
                text = '',
                callbackTopic = '',
            ) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Busy Dialog: You must specify a dialog id");
                messageHub.post({
                    id: id,
                    text: text,
                    callbackTopic: callbackTopic
                }, 'ide.busyDialog.show');
            };
            const hideBusyDialog = function (id) {
                if (isNullOrUndefinedOrEmpty(id))
                    throw Error("Busy Dialog: You must specify a dialog id");
                messageHub.post({ id: id }, 'ide.busyDialog.hide');
            };
            const showSelectDialog = function (
                title,
                listItems,
                callbackTopic,
                isSingleChoice = true,
                hasSearch = false
            ) {
                if (isNullOrUndefinedOrEmpty(title))
                    throw Error("Select dialog: Title must be specified");
                if (isNullOrUndefined(listItems) && !Array.isArray(listItems))
                    throw Error("Select dialog: You must provide a list of strings.");
                else if (listItems.length === 0)
                    throw Error("Select dialog: List is empty");
                if (isNullOrUndefinedOrEmpty(callbackTopic))
                    throw Error("Select dialog: Callback topic must pe specified");
                messageHub.post({
                    title: title,
                    listItems: listItems,
                    callbackTopic: callbackTopic,
                    isSingleChoice: isSingleChoice,
                    hasSearch: hasSearch
                }, 'ide.selectDialog');
            };
            const showDialogWindow = function (
                dialogWindowId = "",
                params = {},
                callbackTopic = null,
                closable = true,
                serviceData = null
            ) {
                if (isNullOrUndefined(params) || !(typeof params === 'object' && !Array.isArray(params)))
                    throw Error("showDialogWindow: params must be an object");
                messageHub.post({
                    dialogWindowId: dialogWindowId,
                    params: params,
                    callbackTopic: callbackTopic,
                    closable: closable,
                    serviceData: serviceData
                }, 'ide.dialogWindow');
            };
            const closeDialogWindow = function (dialogWindowId = "") {
                if (isNullOrUndefinedOrEmpty(dialogWindowId))
                    throw Error("closeDialogWindow: you must provide an ID");
                messageHub.post({
                    dialogWindowId: dialogWindowId,
                }, 'ide.dialogWindow.close');
            };
            const openView = function (viewId, params) {
                if (isNullOrUndefinedOrEmpty(viewId))
                    throw Error("openView: viewId must be specified");
                if (!isNullOrUndefined(params) && !(typeof params === 'object' && !Array.isArray(params)))
                    throw Error("openView: params must be an object");
                messageHub.post({
                    viewId: viewId,
                    params: params,
                }, 'ide-core.openView');
            };
            const openPerspective = function (link, params) {
                if (isNullOrUndefinedOrEmpty(link))
                    throw Error("openPerspective: link must be specified");
                messageHub.post({
                    link: link,
                    params: params,
                }, 'ide-core.openPerspective');
            };
            const openEditor = function (
                resourcePath,
                resourceLabel,
                contentType,
                editorId,
                extraArgs
            ) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("openEditor: resourcePath must be specified");
                messageHub.post({
                    resourcePath: resourcePath,
                    resourceLabel: resourceLabel,
                    contentType: contentType,
                    editorId: editorId,
                    extraArgs: extraArgs,
                }, 'ide-core.openEditor');
            };
            const isEditorOpen = function (resourcePath) {
                return new Promise((resolve, reject) => {
                    if (isNullOrUndefinedOrEmpty(resourcePath))
                        reject(new Error("isEditorOpen: resourcePath must be specified"));

                    const callbackTopic = `core.editors.isOpen.${new Date().valueOf()}`;

                    messageHub.post({
                        resourcePath: resourcePath,
                        callbackTopic: callbackTopic
                    }, 'core.editors.isOpen');

                    const handler = messageHub.subscribe(function (msg) {
                        messageHub.unsubscribe(handler);
                        resolve(msg);
                    }, callbackTopic);
                });
            };
            /**
             * Returnes a list of all files whose path starts with 'basePath', from the currently opened editors.
             * If basePath is not specified, all files will be listed.
             */
            const getCurrentlyOpenedFiles = function (basePath = '/') {
                return new Promise((resolve, reject) => {
                    if (isNullOrUndefinedOrEmpty(basePath))
                        reject(new Error("getOpenedFiles: resourcePath cannot be null, undefined or empty"));

                    const callbackTopic = `core.editors.openedFiles.${new Date().valueOf()}`;

                    messageHub.post({
                        basePath: basePath,
                        callbackTopic: callbackTopic
                    }, 'core.editors.openedFiles');

                    const handler = messageHub.subscribe(function (msg) {
                        messageHub.unsubscribe(handler);
                        resolve(msg);
                    }, callbackTopic);
                });
            };
            /**
             * Trigger the reloading of view parameters for a specific editor.
             * resourcePath: String - The full file path.
            */
            const editorReloadParameters = function (resourcePath) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("editorReloadParameters: resourcePath must be specified");
                messageHub.post({
                    resourcePath: resourcePath,
                }, 'core.editors.reloadParams');
            };
            const onEditorReloadParameters = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'core.editors.reloadParams');
            };
            const setEditorDirty = function (
                resourcePath,
                isDirty,
            ) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("setEditorDirty: resourcePath must be specified");
                if (isNullOrUndefined(isDirty))
                    throw Error("setEditorDirty: isDirty must be specified");
                messageHub.post({
                    resourcePath: resourcePath,
                    isDirty: isDirty,
                }, 'ide-core.setEditorDirty');
            };
            const setFocusedEditor = function (resourcePath) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("setFocusedEditor: resourcePath must be specified");
                messageHub.post({ resourcePath: resourcePath }, 'ide-core.setFocusedEditor');
            };
            const setEditorFocusGain = function (resourcePath) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("setFocusedEditor: resourcePath must be specified");
                messageHub.post({ resourcePath: resourcePath }, 'ide-core.setEditorFocusGain');
            };
            const onEditorFocusGain = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide-core.setEditorFocusGain');
            };
            const closeEditor = function (
                resourcePath,
            ) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("closeEditor: resourcePath must be specified");
                messageHub.post({
                    resourcePath: resourcePath,
                }, 'ide-core.closeEditor');
            };
            const closeOtherEditors = function (
                resourcePath,
            ) {
                if (isNullOrUndefinedOrEmpty(resourcePath))
                    throw Error("closeEditor: resourcePath must be specified");
                messageHub.post({
                    resourcePath: resourcePath,
                }, 'ide-core.closeOtherEditors');
            };
            const closeAllEditors = function () {
                messageHub.post({}, 'ide-core.closeAllEditors');
            };
            const setTabFocus = function (tabId) {
                if (isNullOrUndefinedOrEmpty(tabId))
                    throw Error("setTabFocusGain: tabId must be specified");
                messageHub.post({ tabId: tabId }, 'ide-core.setTabFocus');
            };
            const onSetTabFocus = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide-core.setTabFocus');
            };
            const unsubscribe = function (handler) {
                messageHub.unsubscribe(handler);
            };
            /**
             * fileDescriptor object:
             * {
             *   name: 'example.js',
             *   path: '/project/folder/example.js',
             *   contentType: 'text/javascript',
             *   workspace: 'example'
             * }
            */
            const announceFileSelected = function (fileDescriptor) {
                if (!isNullOrUndefined(fileDescriptor) && !(typeof fileDescriptor === 'object' && !Array.isArray(fileDescriptor)))
                    throw Error('You must provide an appropriate file descriptor object, containing name, path, contentType and workspace');
                messageHub.post(fileDescriptor, 'ide.file.selected');
            };
            const onFileSelected = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.selected');
            };
            const announceFileOpened = function (fileDescriptor) {
                messageHub.post(fileDescriptor, 'ide.file.opened');
            };
            const onFileOpened = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.opened');
            };
            const announceFileCreated = function (fileDescriptor) {
                if (!isNullOrUndefined(fileDescriptor) && !(typeof fileDescriptor === 'object' && !Array.isArray(fileDescriptor)))
                    throw Error('You must provide an appropriate file descriptor object, containing name, path, contentType and workspace');
                messageHub.post(fileDescriptor, 'ide.file.created');
            };
            const onFileCreated = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.created');
            };
            const announceFileDeleted = function (fileDescriptor) {
                if (!isNullOrUndefined(fileDescriptor) && !(typeof fileDescriptor === 'object' && !Array.isArray(fileDescriptor)))
                    throw Error('You must provide an appropriate file descriptor object, containing name, path, contentType and workspace');
                messageHub.post(fileDescriptor, 'ide.file.deleted');
            };
            const onFileDeleted = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.deleted');
            };
            /**
             * fileDescriptor object for rename:
             * {
             *   oldName: 'sample.js',
             *   name: 'example.js',
             *   oldPath: '/project/folder/sample.js',
             *   path: '/project/folder/example.js',
             *   contentType: 'text/javascript',
             *   workspace: 'example'
             * }
            */
            const announceFileRenamed = function (fileDescriptor) {
                if (!isNullOrUndefined(fileDescriptor) && !(typeof fileDescriptor === 'object' && !Array.isArray(fileDescriptor)))
                    throw Error('You must provide an appropriate file descriptor object, containing name, path, contentType and workspace');
                messageHub.post(fileDescriptor, 'ide.file.renamed');
            };
            const onFileRenamed = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.renamed');
            };
            const announceFileSaved = function (fileDescriptor) {
                if (!isNullOrUndefined(fileDescriptor) && !(typeof fileDescriptor === 'object' && !Array.isArray(fileDescriptor)))
                    throw Error('You must provide an appropriate file descriptor object, containing name, path, contentType and workspace');
                messageHub.post(fileDescriptor, 'ide.file.saved');
            };
            const onFileSaved = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.saved');
            };
            /**
             * fileDescriptor object for move:
             * {
             *   name: 'example.js',
             *   path: '/project/folder/example.js',
             *   oldPath: '/project/folder/sample.js',
             *   workspace: 'example'
             * }
            */
            const announceFileMoved = function (fileDescriptor) {
                if (!isNullOrUndefined(fileDescriptor) && !(typeof fileDescriptor === 'object' && !Array.isArray(fileDescriptor)))
                    throw Error('You must provide an appropriate file descriptor object, containing name, path, oldPath and workspace');
                messageHub.post(fileDescriptor, 'ide.file.moved');
            };
            const onFileMoved = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.moved');
            };
            const announceFileCopied = function (fileDescriptor) {
                messageHub.post(fileDescriptor, 'ide.file.copied');
            };
            const onFileCopied = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.file.copied');
            };
            const announcePublish = function (fileDescriptor) {
                messageHub.post(fileDescriptor, 'ide.event.publish');
            };
            const onPublish = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.event.publish');
            };
            const announceUnpublish = function (fileDescriptor) {
                messageHub.post(fileDescriptor, 'ide.event.unpublish');
            };
            const onUnpublish = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.event.unpublish');
            };
            const announceWorkspaceChanged = function (workspace) {
                if (workspace !== undefined && !(typeof workspace === 'object' && !Array.isArray(workspace) && workspace !== null) && !workspace.hasOwnProperty('name'))
                    throw Error('You must provide an appropriate workspace object, containing the "name" key');
                messageHub.post({ data: workspace }, 'ide.workspace.changed');
            };
            const onWorkspaceChanged = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.workspace.changed');
            };
            const announceWorkspacesModified = function () {
                trigger('ide.workspaces.modified', true);
            };
            const onWorkspacesModified = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.workspaces.modified');
            };
            const announceRepositoryModified = function () {
                trigger('ide.repository.modified', true);
            };
            const onRepositoryModified = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.repository.modified');
            };
            const onThemeChanged = function (callbackFunc) {
                if (typeof callbackFunc !== 'function')
                    throw Error('Callback argument must be a function');
                return messageHub.subscribe(callbackFunc, 'ide.themeChange');
            };
            return {
                showStatusBusy: showStatusBusy,
                hideStatusBusy: hideStatusBusy,
                setStatusMessage: setStatusMessage,
                setStatusError: setStatusError,
                setStatusCaret: setStatusCaret,
                showAlertSuccess: showAlertSuccess,
                showAlertInfo: showAlertInfo,
                showAlertWarning: showAlertWarning,
                showAlertError: showAlertError,
                showDialog: showDialog,
                showDialogAsync: showDialogAsync,
                showFormDialog: showFormDialog,
                updateFormDialog: updateFormDialog,
                hideFormDialog: hideFormDialog,
                showLoadingDialog: showLoadingDialog,
                updateLoadingDialog: updateLoadingDialog,
                hideLoadingDialog: hideLoadingDialog,
                showBusyDialog: showBusyDialog,
                hideBusyDialog: hideBusyDialog,
                showSelectDialog: showSelectDialog,
                showDialogWindow: showDialogWindow,
                closeDialogWindow: closeDialogWindow,
                openView: openView,
                openPerspective: openPerspective,
                openEditor: openEditor,
                isEditorOpen: isEditorOpen,
                getCurrentlyOpenedFiles: getCurrentlyOpenedFiles,
                editorReloadParameters: editorReloadParameters,
                onEditorReloadParameters: onEditorReloadParameters,
                setEditorDirty: setEditorDirty,
                setFocusedEditor: setFocusedEditor,
                setEditorFocusGain: setEditorFocusGain,
                onEditorFocusGain: onEditorFocusGain,
                closeEditor: closeEditor,
                closeOtherEditors: closeOtherEditors,
                closeAllEditors: closeAllEditors,
                setTabFocus: setTabFocus,
                onSetTabFocus: onSetTabFocus,
                triggerEvent: trigger,
                'postMessage': post,
                onDidReceiveMessage: onMessage,
                unsubscribe: unsubscribe,
                announceFileSelected: announceFileSelected,
                onFileSelected: onFileSelected,
                announceFileOpened: announceFileOpened,
                onFileOpened: onFileOpened,
                announceFileCreated: announceFileCreated,
                onFileCreated: onFileCreated,
                announceFileDeleted: announceFileDeleted,
                onFileDeleted: onFileDeleted,
                announceFileRenamed: announceFileRenamed,
                onFileRenamed: onFileRenamed,
                announceFileSaved: announceFileSaved,
                onFileSaved: onFileSaved,
                announceFileMoved: announceFileMoved,
                onFileMoved: onFileMoved,
                announceFileCopied: announceFileCopied,
                onFileCopied: onFileCopied,
                announcePublish: announcePublish,
                onPublish: onPublish,
                announceUnpublish: announceUnpublish,
                onUnpublish: onUnpublish,
                announceWorkspaceChanged: announceWorkspaceChanged,
                onWorkspaceChanged: onWorkspaceChanged,
                announceWorkspacesModified: announceWorkspacesModified,
                onWorkspacesModified: onWorkspacesModified,
                announceRepositoryModified: announceRepositoryModified,
                onRepositoryModified: onRepositoryModified,
                onThemeChanged: onThemeChanged,
            };
        }];
    });
