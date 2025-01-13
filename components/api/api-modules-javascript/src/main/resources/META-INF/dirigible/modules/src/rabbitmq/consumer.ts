/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * RabbitMQ Consumer
 *
 */

const RabbitMQFacade = Java.type("org.eclipse.dirigible.components.api.rabbitmq.RabbitMQFacade");

export class Consumer {

	public static startListening(queue: string, handler: string): void {
		RabbitMQFacade.startListening(queue, handler);
	}

	public static stopListening(queue: string, handler: string): void {
		RabbitMQFacade.stopListening(queue, handler);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Consumer;
}
