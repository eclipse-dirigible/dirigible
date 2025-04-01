/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const MessagesHolder = Java.type("org.eclipse.dirigible.integration.tests.api.java.messaging.MessagesHolder");

export function onMessage(message) {
    console.log(`${new Date()}### BACKGROUND HANDLER ### - Received a message: [${message}]`);
    console.log(`${new Date()}### BACKGROUND HANDLER ### - Setting message: [${message}]`);
    MessagesHolder.setLatestReceivedMessage(message);
    console.error(`${new Date()}### Message set`);
}

export function onError(error) {
    console.error(`${new Date()}### BACKGROUND HANDLER ### - Received an error: [${error}]`);
    console.error(`${new Date()}### BACKGROUND HANDLER ### - Setting error: [${error}]`);
    MessagesHolder.setLatestReceivedError(error);
    console.error(`${new Date()}### Error set`);
}