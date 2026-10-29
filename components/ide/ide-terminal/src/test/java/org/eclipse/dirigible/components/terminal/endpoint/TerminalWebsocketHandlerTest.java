/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The terminal page opens a backend session without a token; the handler sets the backend's.
 */
class TerminalWebsocketHandlerTest {

    private static final String TOKEN = "ZGlyaWdpYmxlOnNlY3JldA==";

    @Test
    void sets_the_token_on_the_message_opening_the_session() {
        ByteBuffer forwarded = TerminalWebsocketHandler.withAuthToken(bytes("{\"AuthToken\":\"\",\"columns\":80,\"rows\":24}"), TOKEN);

        JsonObject handshake = JsonParser.parseString(text(forwarded))
                                         .getAsJsonObject();
        assertEquals(TOKEN, handshake.get("AuthToken")
                                     .getAsString());
        assertEquals(80, handshake.get("columns")
                                  .getAsInt());
        assertEquals(24, handshake.get("rows")
                                  .getAsInt());
    }

    @Test
    void sets_the_token_when_the_page_sent_none() {
        ByteBuffer forwarded = TerminalWebsocketHandler.withAuthToken(bytes("{\"columns\":80,\"rows\":24}"), TOKEN);

        assertEquals(TOKEN, JsonParser.parseString(text(forwarded))
                                      .getAsJsonObject()
                                      .get("AuthToken")
                                      .getAsString());
    }

    @Test
    void passes_input_and_resize_messages_on_unchanged() {
        ByteBuffer input = bytes("0ls -la\n");
        ByteBuffer resize = bytes("1{\"columns\":120,\"rows\":40}");

        assertSame(input, TerminalWebsocketHandler.withAuthToken(input, TOKEN));
        assertSame(resize, TerminalWebsocketHandler.withAuthToken(resize, TOKEN));
        assertEquals("0ls -la\n", text(input));
    }

    @Test
    void passes_an_empty_message_on_unchanged() {
        ByteBuffer empty = ByteBuffer.allocate(0);

        assertSame(empty, TerminalWebsocketHandler.withAuthToken(empty, TOKEN));
    }

    private static ByteBuffer bytes(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String text(ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer.duplicate())
                                     .toString();
    }
}
