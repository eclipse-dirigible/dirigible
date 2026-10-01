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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import jakarta.jms.JMSException;

class TenantUserChangePublisherTest {

    private static final String QUEUE = "global:acme.changes";

    private MessageProducer producer;
    private TenantUserChangePublisher publisher;

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.setStringValue(QUEUE);
        producer = mock(MessageProducer.class);
        publisher = new TenantUserChangePublisher(producer);
    }

    @AfterEach
    void tearDown() {
        Configuration.remove(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey());
    }

    @Test
    void aRequestGoesToTheConfiguredQueueAsJson() throws Exception {
        publisher.publish(request());

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(producer).sendMessageToQueue(eq(QUEUE), json.capture());
        assertTrue(json.getValue()
                       .startsWith("{\"requestId\":\"r-1\",\"type\":\"tenant.user.change.requested\",\"version\":1,\"action\":\"INVITE\""),
                json.getValue());
        assertTrue(json.getValue()
                       .contains("\"roles\":[\"Owner\",\"User\"]"),
                json.getValue());
    }

    @Test
    void aBrokerFailureIsA503() throws Exception {
        doThrow(new JMSException("the broker is down")).when(producer)
                                                       .sendMessageToQueue(anyString(), anyString());

        TenantUsersException refusal = assertThrows(TenantUsersException.class, () -> publisher.publish(request()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, refusal.status());
        assertEquals("PUBLISH_FAILED", refusal.reason());
    }

    private static TenantUserChangeRequest request() {
        return new TenantUserChangeRequest("r-1", TenantUserChangeRequest.TYPE, TenantUserChangeRequest.VERSION,
                TenantUserChangeRequest.Action.INVITE, "acme", "library", "ann@example.com", List.of("Owner", "User"), null,
                "owner@acme.test", "2026-09-30T10:00:00Z");
    }
}
