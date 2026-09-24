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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.jms.JMSException;

class TenantUserInvitationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final String QUEUE = "global:it.requests";

    private ApplicationUserService users;
    private MessageProducer producer;
    private TenantUserInvitationService service;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.setStringValue(QUEUE);
        DirigibleConfig.APP_ID.setStringValue("library");
        users = mock(ApplicationUserService.class);
        producer = mock(MessageProducer.class);
        service = new TenantUserInvitationService(users, producer, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        Configuration.remove(DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.getKey());
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
    }

    private static ApplicationUserState state(String email, String role, String requestId) {
        return new ApplicationUserState(1L, "acme", email, List.of(), "PENDING", null, requestId, role, "SENT", NOW.toString(),
                "owner@example.com", NOW.toString(), "owner@example.com", NOW.toString(), null, NOW.toString());
    }

    @Test
    void anInvitationIsRecordedThenPublishedWithTheContractEnvelope() throws Exception {
        when(users.prepareInvitation(eq("acme"), eq("new.user@example.com"), eq("User"), eq("owner@example.com"), anyString(),
                eq(NOW))).thenAnswer(
                        call -> new ApplicationUserService.PreparedInvitation(1L, true, null,
                                state("new.user@example.com", "User", call.getArgument(4))));

        ApplicationUserState result = service.invite("acme", new InvitationRequest(" New.User@Example.com", "User"), "owner@example.com");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(producer).sendMessageToQueue(eq(QUEUE), body.capture());
        JsonNode message = json.readTree(body.getValue());
        assertEquals(result.requestId(), message.get("messageId")
                                                .asText());
        assertEquals("user.assignment.requested", message.get("type")
                                                         .asText());
        assertEquals(1, message.get("version")
                               .asInt());
        assertEquals("acme", message.get("tenantId")
                                    .asText());
        assertEquals("library", message.get("appId")
                                       .asText());
        assertEquals("new.user@example.com", message.get("email")
                                                    .asText());
        assertEquals("User", message.get("role")
                                    .asText());
        assertEquals("owner@example.com", message.get("requestedBy")
                                                 .asText());
        assertEquals(NOW.toString(), message.get("requestedAt")
                                            .asText());
    }

    @Test
    void aFailedPublishIsUndoneAndAnsweredUnavailable() throws Exception {
        ApplicationUserService.PreparedInvitation prepared =
                new ApplicationUserService.PreparedInvitation(1L, true, null, state("x@example.com", "User", "m1"));
        when(users.prepareInvitation(any(), any(), any(), any(), any(), any())).thenReturn(prepared);
        doThrow(new JMSException("broker down")).when(producer)
                                                .sendMessageToQueue(anyString(), anyString());

        TenantUsersException refusal = assertThrows(TenantUsersException.class,
                () -> service.invite("acme", new InvitationRequest("x@example.com", "User"), "owner@example.com"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, refusal.status());
        assertEquals("PUBLISH_FAILED", refusal.reason());
        verify(users).undoInvitation(prepared);
    }

    @Test
    void aRoleThatIsNotGrantableIsRefusedBeforeAnythingIsRecorded() {
        TenantUsersException refusal = assertThrows(TenantUsersException.class,
                () -> service.invite("acme", new InvitationRequest("x@example.com", "Admin"), "o"));

        assertEquals(HttpStatus.BAD_REQUEST, refusal.status());
        assertEquals("INVALID_ROLE", refusal.reason());
        verify(users, never()).prepareInvitation(any(), any(), any(), any(), any(), any());
    }

    @Test
    void anAddressThatIsNotAnEmailIsRefused() {
        TenantUsersException refusal =
                assertThrows(TenantUsersException.class, () -> service.invite("acme", new InvitationRequest("not-an-email", "User"), "o"));

        assertEquals("INVALID_EMAIL", refusal.reason());
    }

    @Test
    void aResendPublishesTheSameId() throws Exception {
        when(users.prepareResend("acme", 1L, "owner@example.com", NOW)).thenReturn(
                new ApplicationUserService.PreparedInvitation(1L, false, null, state("x@example.com", "User", "m-kept")));

        service.resend("acme", 1L, "owner@example.com");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(producer).sendMessageToQueue(eq(QUEUE), body.capture());
        assertEquals("m-kept", json.readTree(body.getValue())
                                   .get("messageId")
                                   .asText());
    }
}
