/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes the Owners' change requests to the configured queue. There is nothing to write first
 * and nothing to compensate: the replica changes only when the provisioning system says what it
 * did, so a request the broker refused is simply answered 503 and the Owner tries again.
 */
@Component
@Conditional(TenantUsersEnabledCondition.class)
class TenantUserChangePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUserChangePublisher.class);

    /** The serializer. */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The producer. */
    private final MessageProducer producer;

    /**
     * Instantiates the publisher.
     *
     * @param producer the producer
     */
    TenantUserChangePublisher(MessageProducer producer) {
        this.producer = producer;
    }

    /**
     * Publishes a change request. Synchronized, because the JMS session behind the producer is
     * single-threaded and request threads publish concurrently.
     *
     * @param request the request
     */
    synchronized void publish(TenantUserChangeRequest request) {
        String queue = TenantUsersSettings.changeQueue();
        String json;
        try {
            json = JSON.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the change request [" + request.requestId() + "]", e);
        }
        try {
            producer.sendMessageToQueue(queue, json);
        } catch (Exception e) {
            LOGGER.error("Could not publish the change request [{}] to [{}]", LogSanitizer.sanitize(request.requestId()),
                    LogSanitizer.sanitize(queue), e);
            throw new TenantUsersException(HttpStatus.SERVICE_UNAVAILABLE, "PUBLISH_FAILED",
                    "The change could not be sent - try again in a moment");
        }
        LOGGER.info("Published change request [{}]: [{}] of [{}] in tenant [{}] with roles {} to [{}]",
                LogSanitizer.sanitize(request.requestId()), request.action(), LogSanitizer.sanitize(request.email()),
                LogSanitizer.sanitize(request.tenantId()), request.roles(), LogSanitizer.sanitize(queue));
    }
}
