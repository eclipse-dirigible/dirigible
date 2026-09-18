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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;

/**
 * A bearer session is told {@code Unauthorized} when its token expires, unless it ended first.
 */
@SuppressWarnings("unchecked")
class BearerTokenStompSessionTerminatorTest {

    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final MessageChannel clientOutboundChannel = mock(MessageChannel.class);
    private final ScheduledFuture<?> deadline = mock(ScheduledFuture.class);
    private final Instant expiresAt = Instant.now()
                                             .plusSeconds(300);

    private BearerTokenStompSessionTerminator terminator;

    @BeforeEach
    void setUp() {
        doReturn(deadline).when(scheduler)
                          .schedule(any(Runnable.class), any(Instant.class));
        when(clientOutboundChannel.send(any())).thenReturn(true);
        terminator = new BearerTokenStompSessionTerminator(scheduler, clientOutboundChannel);
    }

    @Test
    void theSessionIsToldUnauthorizedAtTheDeadline() {
        ArgumentCaptor<Runnable> end = ArgumentCaptor.forClass(Runnable.class);
        terminator.endAt("session-1", "jane", expiresAt);
        verify(scheduler).schedule(end.capture(), eq(expiresAt));

        end.getValue()
           .run();

        ArgumentCaptor<Message<?>> frame = ArgumentCaptor.forClass(Message.class);
        verify(clientOutboundChannel).send(frame.capture());
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(frame.getValue());
        assertEquals(StompCommand.ERROR, accessor.getCommand(), "Spring closes the session after an ERROR frame");
        assertEquals("session-1", accessor.getSessionId());
        assertEquals(BearerTokenStompErrorHandler.UNAUTHORIZED, accessor.getMessage());

        // the deadline is spent, so the DISCONNECT that follows the close has nothing to cancel
        terminator.sessionEnded("session-1");
        verify(deadline, never()).cancel(anyBoolean());
    }

    @Test
    void aSessionThatEndsFirstCancelsItsDeadline() {
        terminator.endAt("session-1", "jane", expiresAt);

        terminator.sessionEnded("session-1");

        verify(deadline).cancel(false);
        verifyNoInteractions(clientOutboundChannel);
    }

    @Test
    void aSecondDeadlineOfTheSameSessionReplacesTheFirst() {
        ScheduledFuture<?> first = mock(ScheduledFuture.class);
        ScheduledFuture<?> second = mock(ScheduledFuture.class);
        doReturn(first, second).when(scheduler)
                               .schedule(any(Runnable.class), any(Instant.class));

        terminator.endAt("session-1", "jane", expiresAt);
        terminator.endAt("session-1", "jane", expiresAt.plusSeconds(60));

        verify(first).cancel(false);
        verify(second, never()).cancel(anyBoolean());
    }

    @Test
    void aSessionWithoutADeadlineIsNothingToForget() {
        assertDoesNotThrow(() -> terminator.sessionEnded("unknown"));
        verifyNoInteractions(scheduler, clientOutboundChannel);
    }

    @Test
    void aSendThatFailsIsLoggedNotThrown() {
        doThrow(new MessageDeliveryException("the session is gone")).when(clientOutboundChannel)
                                                                    .send(any());
        ArgumentCaptor<Runnable> end = ArgumentCaptor.forClass(Runnable.class);
        terminator.endAt("session-1", "jane", expiresAt);
        verify(scheduler).schedule(end.capture(), eq(expiresAt));

        assertDoesNotThrow(() -> end.getValue()
                                    .run());
    }
}
