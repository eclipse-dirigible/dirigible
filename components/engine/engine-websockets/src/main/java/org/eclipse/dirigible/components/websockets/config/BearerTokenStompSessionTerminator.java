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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Ends a bearer STOMP session when its token does.
 *
 * <p>
 * Refusing the frames a client sends after its token expired, which
 * {@code BearerTokenStompInterceptor} does, leaves a client that only listens untouched: nothing it
 * does reaches the inbound channel, and the broker keeps delivering to it. So the expiry is a
 * deadline as well. When the CONNECT of a bearer session is accepted, an ERROR frame is scheduled
 * for the instant the token expires, and Spring's STOMP handler closes the WebSocket after it has
 * sent an ERROR frame. A session that ends before the deadline cancels it.
 *
 * <p>
 * The frame says {@code Unauthorized}, like every ERROR frame that answers a security failure; the
 * reason is in the log.
 */
@Component
public class BearerTokenStompSessionTerminator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerTokenStompSessionTerminator.class);

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    private final TaskScheduler scheduler;
    private final MessageChannel clientOutboundChannel;
    private final Map<String, ScheduledFuture<?>> deadlines = new ConcurrentHashMap<>();

    /**
     * Instantiates the terminator.
     *
     * @param scheduler the scheduler of the message broker
     * @param clientOutboundChannel the channel carrying frames to the clients
     */
    BearerTokenStompSessionTerminator(@Qualifier("messageBrokerTaskScheduler") TaskScheduler scheduler,
            @Qualifier("clientOutboundChannel") MessageChannel clientOutboundChannel) {
        this.scheduler = scheduler;
        this.clientOutboundChannel = clientOutboundChannel;
    }

    /**
     * Ends a session at an instant, unless it ends on its own first.
     *
     * @param sessionId the STOMP session
     * @param user the name of the session's user, for the log
     * @param expiresAt the instant the token of the session expires
     */
    void endAt(String sessionId, String user, Instant expiresAt) {
        ScheduledFuture<?> deadline = scheduler.schedule(() -> end(sessionId, user), expiresAt);
        ScheduledFuture<?> previous = deadlines.put(sessionId, deadline);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    /**
     * Forgets the deadline of a session that ended on its own.
     *
     * @param sessionId the STOMP session
     */
    void sessionEnded(String sessionId) {
        ScheduledFuture<?> deadline = deadlines.remove(sessionId);
        if (deadline != null) {
            deadline.cancel(false);
        }
    }

    private void end(String sessionId, String user) {
        deadlines.remove(sessionId);
        LOGGER.info("The bearer token of the STOMP session [{}] of user [{}] expired - ending the session", sessionId, user);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setSessionId(sessionId);
        accessor.setMessage(BearerTokenStompErrorHandler.UNAUTHORIZED);
        accessor.setLeaveMutable(true);
        Message<byte[]> error = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
        try {
            clientOutboundChannel.send(error);
        } catch (MessagingException ex) {
            LOGGER.warn("The STOMP session [{}] of user [{}] could not be told that its token expired", sessionId, user, ex);
        }
    }
}
