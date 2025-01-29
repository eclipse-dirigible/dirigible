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
class ShellHub extends MessageHubApi {
    /**
     * Shows a perspective inside the shell.
     * @param {string} id - Perspective id.
     * @param {object} [params] - Extra parameters.
     */ // @ts-ignore
    showPerspective({ id, params } = {}) {
        this.postMessage({
            topic: 'platform.shell.perspective.show',
            data: {
                id: id,
                params: params,
            }
        });
    }

    /**
     * Triggered when a perspective should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onShowPerspective(handler) {
        return this.addMessageListener({ topic: 'platform.shell.perspective.show', handler: handler });
    }
}