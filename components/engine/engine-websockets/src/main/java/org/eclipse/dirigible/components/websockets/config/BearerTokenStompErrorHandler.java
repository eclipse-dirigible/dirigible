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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * Answers a frame that failed authentication or authorization with an ERROR frame saying
 * {@code Unauthorized} and nothing more. Spring's own handler copies the exception text into the
 * frame, which for a refused token or a denied destination spells out what was checked; the client
 * gets the outcome, the log gets the reason.
 */
class BearerTokenStompErrorHandler extends StompSubProtocolErrorHandler {

    /** The message of every ERROR frame answering a security failure. */
    static final String UNAUTHORIZED = "Unauthorized";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BearerTokenStompErrorHandler.class);

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    /**
     * Handle client message processing error.
     *
     * @param clientMessage the frame that failed; may be {@code null}
     * @param ex the failure, possibly wrapped by the channel
     * @return the ERROR frame to send
     */
    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable cause = ex instanceof MessageDeliveryException && ex.getCause() != null ? ex.getCause() : ex;
        if (!(cause instanceof AuthenticationException) && !(cause instanceof AccessDeniedException)) {
            return super.handleClientMessageProcessingError(clientMessage, ex);
        }
        StompHeaderAccessor clientHeaderAccessor =
                clientMessage != null ? MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class) : null;
        // a refused token or a denied destination is something an operator should see without turning
        // debug on; the refusal closes the connection, so there is one line per refused session
        LOGGER.info("Refusing a STOMP frame [{}] of session [{}]", clientHeaderAccessor != null ? clientHeaderAccessor.getCommand() : null,
                clientHeaderAccessor != null ? clientHeaderAccessor.getSessionId() : null, cause);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(UNAUTHORIZED);
        accessor.setLeaveMutable(true);
        if (clientHeaderAccessor != null && clientHeaderAccessor.getReceipt() != null) {
            accessor.setReceiptId(clientHeaderAccessor.getReceipt());
        }
        return handleInternal(accessor, EMPTY_PAYLOAD, cause, clientHeaderAccessor);
    }
}
