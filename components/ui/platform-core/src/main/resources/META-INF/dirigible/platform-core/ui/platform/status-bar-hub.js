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
class StatusBarHub extends MessageHubApi {
    /**
     * Shows a message in the status bar.
     * @param {string} message - Message text.
     */
    showMessage(message) {
        this.postMessage({
            topic: 'platform.shell.status.message',
            data: message
        });
    }

    /**
     * Triggered when a message should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onMessage(handler) {
        return this.addMessageListener({ topic: 'platform.shell.status.message', handler: handler });
    }

    /**
     * Shows an error message in the status bar.
     * @param {string} message - Message text.
     */
    showError(message) {
        this.postMessage({
            topic: 'platform.shell.status.error',
            data: message
        });
    }

    /**
     * Triggered when an error should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onError(handler) {
        return this.addMessageListener({ topic: 'platform.shell.status.error', handler: handler });
    }

    /**
     * Shows a busy indicator in the status bar.
     * @param {string} text - Status text for the busy indicator.
     */
    showBusy(text) {
        this.postMessage({
            topic: 'platform.shell.status.busy',
            data: text
        });
    }

    /**
     * Hides the busy indicator.
     */
    hideBusy() {
        this.triggerEvent('platform.shell.status.busy');
    }

    /**
     * Triggered when a busy status should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onBusy(handler) {
        return this.addMessageListener({ topic: 'platform.shell.status.busy', handler: handler });
    }

    /**
     * Shows a text label at the end of status bar.
     * @param {string} label - Label text.
     */
    showLabel(label) {
        this.postMessage({
            topic: 'platform.shell.status.label',
            data: label
        });
    }

    /**
     * Triggered when a label should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onLabel(handler) {
        return this.addMessageListener({ topic: 'platform.shell.status.label', handler: handler });
    }
}