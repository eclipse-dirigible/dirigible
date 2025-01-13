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
export function onOpen() {
  console.log("Connection openned.");
}

export function onMessage(message, from) {
  console.log(`Message received: ${message}, from: ${from}`);
  return `Hello from WebSocket! [${message}]`;
}

export function onError(error) {
  console.error(`Error: ${error}`);
}

export function onClose() {
  console.warn("Connection closed.");
}