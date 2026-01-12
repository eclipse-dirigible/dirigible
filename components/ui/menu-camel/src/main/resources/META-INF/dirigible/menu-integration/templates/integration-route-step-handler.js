/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
    const fileContent = `
import { IntegrationMessage } from "@aerokit/sdk/integrations"

export function onMessage(message: IntegrationMessage) {
    return message;
}`;
	return {
		name: 'route-step-handler',
		label: 'Route Step Handler',
		extension: 'ts',
		data: fileContent
	};
};
