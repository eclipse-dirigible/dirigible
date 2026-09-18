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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * The rules every client frame is held to: a principal to connect, the own user queue to subscribe
 * to, the application destinations to send to - and nothing else.
 */
class WebsocketChannelSecurityTest {

    private final AuthorizationManager<Message<?>> authorization = WebsocketConfig.inboundAuthorization();
    private final Authentication jane = new TestingAuthenticationToken("jane", "n/a", "ROLE_DEVELOPER");
    private final Authentication anonymous =
            new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    @Test
    void aConnectNeedsAPrincipal() {
        assertGranted(jane, frame(StompCommand.CONNECT, null));
        assertDenied(anonymous, frame(StompCommand.CONNECT, null));
    }

    @Test
    void aClientMaySubscribeToItsOwnUserQueueOnly() {
        assertGranted(jane, frame(StompCommand.SUBSCRIBE, "/user/queue/reply/it"));
        assertGranted(jane, frame(StompCommand.SUBSCRIBE, "/user/queue/errors/it"));
        assertDenied(anonymous, frame(StompCommand.SUBSCRIBE, "/user/queue/reply/it"));
        assertDenied(jane, frame(StompCommand.SUBSCRIBE, "/topic/announcements"));
        assertDenied(jane, frame(StompCommand.SUBSCRIBE, "/queue/reply/it-user0"), "the queue the broker addresses another user by");
        assertDenied(jane, frame(StompCommand.SUBSCRIBE, "/user/topic/x"));
    }

    @Test
    void aClientMaySendToTheApplicationDestinationsOnly() {
        assertGranted(jane, frame(StompCommand.SEND, "/ws/stomp/it"));
        assertDenied(anonymous, frame(StompCommand.SEND, "/ws/stomp/it"));
        assertDenied(jane, frame(StompCommand.SEND, "/topic/announcements"), "a direct publish to a broker destination");
        assertDenied(jane, frame(StompCommand.SEND, "/queue/reply/it-user0"));
        assertDenied(jane, frame(StompCommand.SEND, "/user/queue/reply/it"));
    }

    @Test
    void housekeepingFramesNeedAPrincipalToo() {
        assertGranted(jane, frame(StompCommand.DISCONNECT, null));
        assertGranted(jane, frame(StompCommand.UNSUBSCRIBE, null));
        assertDenied(anonymous, frame(StompCommand.DISCONNECT, null));
    }

    private void assertGranted(Authentication authentication, Message<byte[]> frame) {
        AuthorizationResult result = authorization.authorize(() -> authentication, frame);
        assertNotNull(result);
        assertTrue(result.isGranted(), () -> "expected " + describe(frame) + " to be granted");
    }

    private void assertDenied(Authentication authentication, Message<byte[]> frame) {
        assertDenied(authentication, frame, describe(frame));
    }

    private void assertDenied(Authentication authentication, Message<byte[]> frame, String reason) {
        AuthorizationResult result = authorization.authorize(() -> authentication, frame);
        assertNotNull(result);
        assertFalse(result.isGranted(), () -> "expected " + reason + " to be denied");
    }

    private static String describe(Message<byte[]> frame) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(frame);
        return accessor.getCommand() + " " + accessor.getDestination();
    }

    private static Message<byte[]> frame(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
