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
 * API Producer
 */

const MessagingFacade = Java.type("org.eclipse.dirigible.components.api.messaging.MessagingFacade");

export class Producer {

	public static queue(destination: string) {
		return new Queue(destination);
	}

	public static topic(destination: string) {
		return new Topic(destination);
	}
}

class Queue {

	private destination: string;

	constructor(destination: string) {
		this.destination = destination;
	}

	public send(message: string) {
		MessagingFacade.sendToQueue(this.destination, message);
	}
}

class Topic {

	private destination: string;

	constructor(destination: string) {
		this.destination = destination;
	}

	public send(message: string) {
		MessagingFacade.sendToTopic(this.destination, message);
	}
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Producer;
}