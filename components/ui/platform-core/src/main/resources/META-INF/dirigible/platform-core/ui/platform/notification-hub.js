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
class NotificationHub extends MessageHubApi {
    /**
     * Shows a notification.
     * @param {('information'|'warning'|'negative'|'positive')} type - Type of notification.
     * @param {string} title - Notification title.
     * @param {string} description - Notification description.
     */
    show({ type, title, description } = {}) {
        this.postMessage({
            topic: 'platform.shell.notification',
            data: {
                type: type,
                title: title,
                description: description,
            }
        });
    }

    /**
     * Triggered when a notification should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onShow(handler) {
        return this.addMessageListener({ topic: 'platform.shell.notification', handler: handler });
    }
}