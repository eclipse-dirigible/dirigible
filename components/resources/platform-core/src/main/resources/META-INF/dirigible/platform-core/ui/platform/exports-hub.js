/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
class ExportsHub extends MessageHubApi {
    /**
     * Event to refresh the exports list.
     */
    refresh() {
        this.triggerEvent('platform.shell.exports.refresh');
    }

    /**
     * Triggered when the exports should be refreshed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onRefresh(handler) {
        return this.addMessageListener({ topic: 'platform.shell.exports.refresh', handler: handler });
    }
}