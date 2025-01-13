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
export function getTemplate() {
	return {
		name: "listener",
		label: "Message Listener",
		extension: "listener",
		data: JSON.stringify({
			name: "mylistener",
			kind: "Q",
			handler: "myproject/myhandler.js",
			description: "My Listener"
		}, null, 4)
	};
};