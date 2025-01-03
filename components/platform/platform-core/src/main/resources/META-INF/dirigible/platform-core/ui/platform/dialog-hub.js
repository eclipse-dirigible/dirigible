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
const AlertTypes = {
    Confirmation: 'confirmation',
    Error: 'error',
    Success: 'success',
    Warning: 'warning',
    Information: 'information'
};

class DialogHub extends MessageHubApi {
    constructor(viewId) {
        super();
        this.viewId = viewId || location.pathname;
    }

    /**
     * Shows an alert dialog.
     * @param {string} title - The title for the alert.
     * @param {string} message - The message that will be displayed inside the alert.
     * @param {string} [type] - The type of the alert. See 'AlertTypes'.
     * @param {boolean} [preformatted] - Newline formatting of the message. If set to true, text will be displayed as-is without formatting it. Defaults to false.
     * @param {Object[]} [buttons] - List of objects, describing a button. The object must contain an 'id', 'label' and optionally 'state' (See ButtonStates).
     * @return {Promise} - Returns a promise if the alert has user-defined buttons and gives the button id as a parameter.
     */ // @ts-ignore
    showAlert({ title, message, type, preformatted, buttons } = {}) {
        const callbackTopic = buttons ? `platform.alert.${new Date().valueOf()}` : undefined;
        this.postMessage({
            topic: 'platform.alert',
            data: {
                title: title,
                message: message,
                type: type,
                preformatted: preformatted,
                buttons: buttons || [{ id: 'close', label: 'Close', type: undefined }],
                topic: callbackTopic
            }
        });
        if (callbackTopic) {
            return new Promise((resolve, reject) => {
                for (let i = 0; i < buttons.length; i++) {
                    if (!buttons[i].id) reject(new Error('Dialogs: showAlert - buttons must have an ID'));
                }
                const callbackListener = this.addMessageListener({
                    topic: callbackTopic,
                    handler: (data) => {
                        this.removeMessageListener(callbackListener);
                        resolve(data);
                    }
                });
            });
        }
    }

    /**
     * Triggered when an alert should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onAlert(handler) {
        return this.addMessageListener({ topic: 'platform.alert', handler: handler });
    }

    /**
     * Shows a dialog.
     * @param {string} [header] - The header of the dialog.
     * @param {string} title - The title for the dialog.
     * @param {string} [subheader] - The subheader of the dialog.
     * @param {string} message - The message that will be displayed inside the dialog.
     * @param {boolean} [preformatted] - Newline formatting of the message. If set to true, text will be displayed as-is without formatting it. Defaults to false.
     * @param {Object[]} [buttons] - List of objects, describing a button. The object must contain an 'id', 'label' and optionally 'state' (See ButtonStates).
     * @param {boolean} [closeButton=true] - Should the dialog have a close button in the title bar. Defaults to true.
     * @return {Promise} - Returns a promise if the dialog has user-defined buttons and gives the button id as a parameter.
     */ // @ts-ignore
    showDialog({ header, title, subheader, message, preformatted, buttons, closeButton = true } = {}) {
        const callbackTopic = buttons ? `platform.dialog.${new Date().valueOf()}` : undefined;
        this.postMessage({
            topic: 'platform.dialog',
            data: {
                header: header,
                title: title,
                subheader: subheader,
                message: message,
                preformatted: preformatted,
                buttons: buttons || [{ id: 'close', label: 'Close', type: undefined }],
                closeButton: closeButton,
                topic: callbackTopic,
            }
        });
        if (callbackTopic) {
            return new Promise((resolve, reject) => {
                for (let i = 0; i < buttons.length; i++) {
                    if (!buttons[i].id) reject(new Error('Dialogs: showDialog - buttons must have an ID'));
                }
                const callbackListener = this.addMessageListener({
                    topic: callbackTopic,
                    handler: (data) => {
                        this.removeMessageListener(callbackListener);
                        resolve(data);
                    }
                });
            });
        }
    }

    /**
     * Triggered when a dialog should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onDialog(handler) {
        return this.addMessageListener({ topic: 'platform.dialog', handler: handler });
    }

    /**
     * Shows a busy dialog. Can also be used to update an existing busy dialog.
     * @param {string} message - The message that will be displayed inside the dialog.
     */ // @ts-ignore
    showBusyDialog(message) {
        this.postMessage({ topic: 'platform.dialog.busy', data: { id: this.viewId, message: message } });
    }

    /**
     * Triggered when a busy dialog should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onBusyDialog(handler) {
        return this.addMessageListener({ topic: 'platform.dialog.busy', handler: handler });
    }

    /**
     * Closes the current busy dialog.
     */ // @ts-ignore
    closeBusyDialog() {
        this.postMessage({ topic: 'platform.dialog.busy', data: { id: this.viewId, close: true } });
    }

    /**
     * Definition of a form item object. A form item can describe several types of form inputs. Some properties are common for all and some are type specific.
     * @typedef {Object} FormItem
     * @property {('input'|'textarea'|'checkbox'|'radio'|'dropdown')} controlType - Type of input. Common property.
     * @property {string} label - Label for the input. Common property.
     * @property {string|number|object} [value] - Value of the input. Common property.
     * @property {('text'|'password'|'number'|'date'|'color'|'email'|'tel'|'time'|'url'|'datetime-local')} type - Type for the input. Specific to the 'input' control type.
     * @property {string} [placeholder] - Placeholder text for the 'input', 'textarea' and 'dropdown' control types.
     * @property {boolean} [required] - Sets the input as required. Common for all control types except 'checkbox'.
     * @property {boolean} [disabled] - Sets the input as disabled. Common for all control types. On 'radio', it disables all options.
     * @property {boolean} [focus] - Sets the input as focus, once the dialog appears. Common for all control types except 'checkbox', 'radio' and 'dropdown'.
     * @property {number} [maxlength] - Maximum character length for the input. Common for the 'input' and 'textarea' control types.
     * @property {number} [minlength] - Minimum character length for the input. Common for the 'input' and 'textarea' control types.
     * @property {number|string} [max] - Maximum value for the input. Only for the 'input' control of the 'number', 'date', 'time' and 'datetime-local' types.
     * @property {number|string} [min] - Minimum value for the input. Only for the 'input' control of the 'number', 'date', 'time' and 'datetime-local' types.
     * @property {number} [rows=3] - Number of rows. Only for the 'input' control of the 'textarea'.
     * @property {number} [step] - Value step for the input. Only for the 'input' control of the 'number' type.
     * @property {{excluded: string[], patterns: string[]}} [inputRules] - Validation rules for the input. The 'excluded' array can contain strings that the input should not match. The 'patterns' array can contain regex expressions for validating the input. Common for the 'input' and 'textarea' control types.
     * @property {boolean} [submitOnEnter] - If the user hits the enter key, while this input is focused, the form will get submitted. Valid only for the 'input' control type.
     * @property {Array.<{label: string, value: string|number}>} options - Options for the control type 'dropdown'.
     * @property {string} [errorMsg] - The error hint that will be displayed, when the selected value is invalid. Common for all control types except 'checkbox' and 'radio'.
     * @property {{key: string, value: string|number|boolean}} [enabledOn] - Enables a control based on the state of another control. The 'key' property is the id of the target control. The 'value' is the value of the target control. If you only provide the key, it will get enabled when the target control has any valid value. If you also provide the value, it will get enabled when the target control's value matches.
     * @property {{key: string, value: string|number|boolean}} [disabledOn] - Same as 'enabledOn' but with an opposite effect.
     * @property {{key: string, value: string|number|boolean}} [visibleOn] - Same as 'enabledOn' but instead of disabling/enabling the control, it will show/hide it.
     * @property {{key: string, value: string|number|boolean}} [hiddenOn] - Same as 'visibleOn' but with an opposite effect.
     */

    /**
     * Shows a form dialog.
     * @param {string} [header] - The header of the dialog.
     * @param {string} title - The title for the dialog.
     * @param {string} [subheader] - The subheader of the dialog.
     * @param {Object.<string, FormItem>} form - Object containing form item definitions. The key for each definition is used as id.
     * @param {string} [submitLabel] - Label for the submit button.
     * @param {string} [cancelLabel] - Label for the cancel button.
     * @param {string} [width] - Custom width for the window. This maps to the CSS width propery.
     * @param {string} [height] - Custom height for the window. This maps to the CSS height propery.
     * @param {string} [maxWidth] - Custom max width for the window. This maps to the CSS max-width propery.
     * @param {string} [maxHeight] - Custom max height for the window. This maps to the CSS max-height propery.
     * @param {string} [minWidth] - Custom min width for the window. This maps to the CSS min-width propery.
     * @param {string} [minHeight] - Custom min height for the window. This maps to the CSS min-height propery.
     * @return {Promise<Object.<string, string|number|boolean>>} - Returns a promise which gives back the selected values of the form.
     */ // @ts-ignore
    showFormDialog({ header, title, subheader, form, submitLabel, cancelLabel, width, height, maxWidth, maxHeight, minWidth, minHeight } = {}) {
        const callbackTopic = `platform.dialog.form.${new Date().valueOf()}`;
        this.postMessage({
            topic: 'platform.dialog.form',
            data: {
                header: header,
                title: title,
                subheader: subheader,
                form: form,
                submitLabel: submitLabel,
                cancelLabel: cancelLabel,
                width: width,
                height: height,
                maxWidth: maxWidth,
                maxHeight: maxHeight,
                minWidth: minWidth,
                minHeight: minHeight,
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
     * Triggered when a form dialog should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onFormDialog(handler) {
        return this.addMessageListener({ topic: 'platform.dialog.form', handler: handler });
    }

    /**
     * Shows a window.
     * @param {boolean} [hasHeader=true] - If the dialog should have a header.
     * @param {string} [header] - The header of the dialog.
     * @param {string} [title] - The title for the window. Use this only when providing a custom path, instead of widnow id.
     * @param {string} [subheader] - The subheader of the dialog.
     * @param {string} [id] - ID of the view that should be opened inside the dialog window. Alternatevly, you can use 'path' instead and provide a direct URL.
     * @param {string} [path] - When showing a custom view, you can provide a direct URL.
     * @param {Object} [params] - Parameters that will be provided as a 'data-parameters' attribute that the view can read.
     * @param {string} [width=95%] - Custom width for the window. This maps to the CSS width propery.
     * @param {string} [heigh=90%] - Custom height for the window. This maps to the CSS height propery.
     * @param {string} [maxWidth=1280px] - Custom max width for the window. This maps to the CSS max-width propery.
     * @param {string} [maxHeight=768px] - Custom max height for the window. This maps to the CSS max-height propery.
     * @param {string} [minWidth] - Custom min width for the window. This maps to the CSS min-width propery.
     * @param {string} [minHeight] - Custom min height for the window. This maps to the CSS min-height propery.
     * @param {string} [callbackTopic] - Callback topic for when the window has been closed.
     * @param {boolean} [closeButton=true] - Should the dialog have a close button in the title bar.
     */ // @ts-ignore
    showWindow({ hasHeader = true, header, title, subheader, id, path, params, width = '95%', height = '90%', maxWidth = '1280px', maxHeight = '768px', minWidth, minHeight, callbackTopic, closeButton = true } = {}) {
        this.postMessage({
            topic: 'platform.dialog.window',
            data: {
                hasHeader: hasHeader,
                header: header,
                label: title,
                subheader: subheader,
                id: id,
                path: path,
                params: JSON.stringify({
                    ...params,
                    container: 'window',
                }),
                closeButton: closeButton,
                width: width,
                height: height,
                maxWidth: maxWidth,
                maxHeight: maxHeight,
                minWidth: minWidth,
                minHeight: minHeight,
                callbackTopic: callbackTopic,
            }
        });
    }

    /**
     * Closes the currently shown window. Usually used inside the window itself when the `closeButton` option is set to false.
     */ // @ts-ignore
    closeWindow() {
        this.postMessage({ topic: 'platform.dialog.window', data: { close: true } });
    }

    /**
     * Triggered when a window should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onWindow(handler) {
        return this.addMessageListener({ topic: 'platform.dialog.window', handler: handler });
    }
}