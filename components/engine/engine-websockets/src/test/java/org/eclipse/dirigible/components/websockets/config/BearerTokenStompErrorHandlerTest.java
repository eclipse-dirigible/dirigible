/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * A security failure is answered with a constant ERROR frame; anything else keeps Spring's answer.
 */
class BearerTokenStompErrorHandlerTest {

    private final BearerTokenStompErrorHandler handler = new BearerTokenStompErrorHandler();

    @Test
    void aDeniedFrameIsAnsweredUnauthorized() {
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, "receipt-7");
        MessageDeliveryException failure = new MessageDeliveryException(subscribe, "Failed to send message to channel",
                new AccessDeniedException("Access Denied: destination /topic/private"));

        Message<byte[]> error = handler.handleClientMessageProcessingError(subscribe, failure);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(error);
        assertEquals(StompCommand.ERROR, accessor.getCommand());
        assertEquals(BearerTokenStompErrorHandler.UNAUTHORIZED, accessor.getMessage());
        assertEquals("receipt-7", accessor.getReceiptId());
    }

    @Test
    void aRefusedTokenIsAnsweredUnauthorizedWithoutTheReason() {
        Message<byte[]> connect = frame(StompCommand.CONNECT, null);

        Message<byte[]> error =
                handler.handleClientMessageProcessingError(connect, new BadCredentialsException("Jwt expired at 2026-09-17T10:00:00Z"));

        assertEquals(BearerTokenStompErrorHandler.UNAUTHORIZED, StompHeaderAccessor.wrap(error)
                                                                                   .getMessage());
    }

    @Test
    void anyOtherFailureKeepsTheStandardAnswer() {
        Message<byte[]> send = frame(StompCommand.SEND, null);

        Message<byte[]> error = handler.handleClientMessageProcessingError(send, new IllegalStateException("broker is down"));

        assertEquals("broker is down", StompHeaderAccessor.wrap(error)
                                                          .getMessage());
    }

    private static Message<byte[]> frame(StompCommand command, String receipt) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        if (receipt != null) {
            accessor.setReceipt(receipt);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
