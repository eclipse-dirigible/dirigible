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
class RepositoryHub extends MessageHubApi {
    /**
     * Sends a message containing information on what has changed.
     * @param {string} [data] - Any information.
     */ // @ts-ignore
    announceRepositoryModified({ data } = {}) {
        this.postMessage({
            topic: 'platform.repository.modified',
            data: data
        });
    }

    /**
     * Triggered when a repository has been modified.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onRepositoryModified(handler) {
        return this.addMessageListener({ topic: 'platform.repository.modified', handler: handler });
    }
}