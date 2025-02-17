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
class MessageHubApi {
    constructor({
        hubWindow = top || window,
        targetOrigin = location.origin,
        allowedOrigins = [location.origin],
    } = {}) {
        if (allowedOrigins === undefined || allowedOrigins.length === 0)
            console.warn('MessageHub: allowedOrigins is not used. This is a security risk.');
        this.hubWindow = hubWindow;
        this.targetOrigin = targetOrigin;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * @param {string} topic - Sends an empty message to a topic.
     */
    triggerEvent(topic) {
        if (topic) this.hubWindow.postMessage({ topic: topic }, this.targetOrigin);
        else throw new Error('MessageHub: triggerEvent - topic parameter is required');
    }

    /**
     * @param {string} topic - The topic to listen to.
     * @param data - This is the actual message/data that will be transmitted.
     */ // @ts-ignore
    postMessage({ topic = undefined, data } = {}) {
        if (data !== null && data !== undefined) {
            this.hubWindow.postMessage({ topic: topic, data: data }, this.targetOrigin);
        } else throw new Error('MessageHub: postMessage - data parameter is required');
    }

    /**
     * @param {string} topic - The topic to listen to.
     * @param handler - Callback function that will handle the incomming message/data.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */ // @ts-ignore
    addMessageListener({ topic, handler } = {}) {
        if (!topic)
            throw new Error('MessageHub: addMessageListener - topic parameter is required');
        if (!handler || (typeof handler !== 'function'))
            throw new Error('MessageHub: addMessageListener - handler parameter is required and must be a function');
        const innerHandler = function (msgHandler, msgTopic, event) {
            if (this.allowedOrigins.indexOf(event.origin) > -1 && event.data.topic == msgTopic) {
                msgHandler(event.data.data);
            }
        }.bind(this, handler, topic);
        this.hubWindow.addEventListener('message', innerHandler, false);
        return innerHandler;
    }

    removeMessageListener(handlerReference) {
        this.hubWindow.removeEventListener('message', handlerReference, false);
    }
}