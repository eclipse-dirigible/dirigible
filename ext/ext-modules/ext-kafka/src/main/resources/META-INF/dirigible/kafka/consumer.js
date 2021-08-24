/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/** Consumer API for Kafka */

exports.topic = function(destination, configuration) {
	var topic = new Topic();
	topic.destination = destination;
    topic.configuration = configuration;
	return topic;
};

function Topic() {
	this.startListening = function(handler, timeout) {
        org.eclipse.dirigible.api.kafka.KafkaFacade.startListening(this.destination, handler, timeout, this.configuration);
	};

    this.stopListening = function(handler, timeout) {
        org.eclipse.dirigible.api.kafka.KafkaFacade.stopListening(this.destination, this.configuration);
	};
};