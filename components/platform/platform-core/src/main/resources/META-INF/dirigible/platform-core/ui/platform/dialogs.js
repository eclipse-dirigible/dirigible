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
angular.module('platformDialogs', ['blimpKit', 'platformView']).directive('dialogs', (Extensions) => ({
    restrict: 'E',
    replace: true,
    transclude: false,
    link: (scope) => {
        let cachedWindows;
        Extensions.getWindows().then((response) => {
            cachedWindows = response.data;
        }, (error) => {
            console.log(error);
        });
        // @ts-ignore
        const dialogHub = new DialogHub();

        scope.messageBoxes = [];
        const alertListener = dialogHub.onAlert((data) => {
            scope.$apply(() => scope.messageBoxes.push(data));
        });
        scope.closeAlert = (buttonId) => {
            if (scope.messageBoxes[0].topic) dialogHub.postMessage({ topic: scope.messageBoxes[0].topic, data: buttonId });
            scope.messageBoxes.shift();
        };

        scope.dialogs = [];
        const dialogListener = dialogHub.onDialog((data) => {
            scope.$apply(() => scope.dialogs.push(data));
        });
        scope.closeDialog = (buttonId) => {
            if (buttonId) dialogHub.postMessage({ topic: scope.dialogs[0].topic, data: buttonId });
            else dialogHub.triggerEvent(scope.dialogs[0].topic);
            scope.dialogs.shift();
        };

        scope.busyDialog = { id: '', message: '' };
        const busyDialogListener = dialogHub.onBusyDialog((data) => {
            scope.$apply(() => {
                if (data.close && scope.busyDialog.id === data.id) {
                    scope.busyDialog.id = '';
                    scope.busyDialog.message = '';
                } else if (!scope.busyDialog.id) {
                    scope.busyDialog.id = data.id;
                    scope.busyDialog.message = data.message;
                } else if (scope.busyDialog.id === data.id) {
                    scope.busyDialog.message = data.message;
                }
            });
        });

        scope.formDialog = { form: {} };
        scope.formDialogs = [];
        const formDialogListener = dialogHub.onFormDialog((data) => {
            scope.$apply(() => scope.formDialogs.push(data));
        });
        const isEnabled = (enabledOn) => {
            if (Object.prototype.hasOwnProperty.call(enabledOn, 'value')) {
                return scope.formDialogs[0].form[enabledOn.key].value === enabledOn.value;
            } else if (scope.formDialog.form[`n${enabledOn.key}`]) {
                return scope.formDialog.form[`n${enabledOn.key}`].$valid;
            }
            return true;
        };
        const isDisabled = (disabledOn) => {
            if (Object.prototype.hasOwnProperty.call(disabledOn, 'value')) {
                return scope.formDialogs[0].form[disabledOn.key].value === disabledOn.value;
            } else if (scope.formDialog.form[`n${disabledOn.key}`]) {
                return !scope.formDialog.form[`n${disabledOn.key}`].$valid;
            }
            return false;
        };
        scope.formItemDisabled = (enabledOn, disabledOn) => {
            let disabled = false;
            if (enabledOn) disabled = !isEnabled(enabledOn);
            if (disabledOn) disabled = isDisabled(disabledOn);
            return disabled;
        };
        const isVisible = (visibleOn) => {
            if (Object.prototype.hasOwnProperty.call(visibleOn, 'value')) {
                return scope.formDialogs[0].form[visibleOn.key].value === visibleOn.value;
            } else if (scope.formDialog.form[`n${visibleOn.key}`]) {
                return scope.formDialog.form[`n${visibleOn.key}`].$valid;
            }
            return true;
        };
        const isHidden = (hiddenOn) => {
            if (Object.prototype.hasOwnProperty.call(hiddenOn, 'value')) {
                return scope.formDialogs[0].form[hiddenOn.key].value === hiddenOn.value;
            } else if (scope.formDialog.form[`n${hiddenOn.key}`]) {
                return !scope.formDialog.form[`n${hiddenOn.key}`].$valid;
            }
            return true;
        };
        scope.formItemVisible = (visibleOn, hiddenOn) => {
            let visible = true;
            if (visibleOn) visible = isVisible(visibleOn);
            if (hiddenOn) visible = !isHidden(hiddenOn);
            return visible;
        };
        scope.closeFormDialog = (submit) => {
            if (submit) {
                const formData = {};
                for (let i = 0, keys = Object.keys(scope.formDialogs[0].form); i < keys.length; i++) {
                    // @ts-ignore
                    formData[keys[i]] = scope.formDialogs[0].form[keys[i]].value;
                }
                dialogHub.postMessage({
                    topic: scope.formDialogs[0].topic,
                    data: formData
                });
            } else dialogHub.triggerEvent(scope.formDialogs[0].topic);
            scope.formDialogs.shift();
        };

        scope.dialogWindows = [];
        const dialogWindowListener = dialogHub.onWindow((data) => {
            if (data.close) {
                scope.$apply(() => {
                    scope.closeDialogWindow();
                });
            } else if (data.id) {
                const viewConfig = cachedWindows.find(v => v.id === data.id);
                scope.$apply(() => {
                    if (!viewConfig) {
                        scope.messageBoxes.push({
                            title: 'Unknown window',
                            message: `Window with id '${data.id}' does not exist.`,
                            type: AlertTypes.Error,
                            buttons: [{ id: 'close', label: 'Close' }]
                        });
                    } else {
                        scope.dialogWindows.push({
                            ...data,
                            ...viewConfig,
                        });
                    }
                });
            } else scope.$apply(() => scope.dialogWindows.push(data));
        });
        scope.closeDialogWindow = () => {
            if (scope.dialogWindows[0].callbackTopic) dialogHub.triggerEvent(scope.dialogWindows[0].callbackTopic);
            scope.dialogWindows.shift();
        };

        scope.$on('$destroy', () => {
            dialogHub.removeMessageListener(alertListener);
            dialogHub.removeMessageListener(dialogListener);
            dialogHub.removeMessageListener(busyDialogListener);
            dialogHub.removeMessageListener(formDialogListener);
            dialogHub.removeMessageListener(dialogWindowListener);
        });
    },
    templateUrl: '/services/web/platform-core/ui/templates/dialogs.html',
}));