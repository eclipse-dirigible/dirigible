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

const KafkaFacade = Java.type("org.eclipse.dirigible.components.api.kafka.KafkaFacade");


export class Consumer {

    public static topic(destination: string, configuration: { [key: string]: string } = {}): Topic {
        return new Topic(destination, configuration);
    }
}

class Topic {
    private destination: string;
    private configuration: { [key: string]: string } = {};

    constructor(destination: string, configuration: { [key: string]: string } = {}) {
        this.destination = destination;
        this.configuration = configuration;
    }

    public startListening(handler: string, timeout: number): void {
        KafkaFacade.startListening(this.destination, handler, timeout, JSON.stringify(this.configuration));
    }

    public stopListening(): void {
        KafkaFacade.stopListening(this.destination, JSON.stringify(this.configuration));
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = Consumer;
}
