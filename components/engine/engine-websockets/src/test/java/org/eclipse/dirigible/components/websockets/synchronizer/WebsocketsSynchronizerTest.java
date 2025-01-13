/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import org.eclipse.dirigible.components.websockets.domain.Websocket;
import org.eclipse.dirigible.components.websockets.synchronizer.WebsocketsSynchronizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * The Class WebsocketsSynchronizerTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class WebsocketsSynchronizerTest {

    /** The websockets synchronizer. */
    @Autowired
    private WebsocketsSynchronizer websocketsSynchronizer;


    /**
     * Checks if is accepted path.
     */
    @Test
    public void isAcceptedPath() {
        assertTrue(websocketsSynchronizer.isAccepted(Path.of("/a/b/c/e1.websocket"), null));
    }

    /**
     * Checks if is accepted artefact.
     */
    @Test
    public void isAcceptedArtefact() {
        assertTrue(websocketsSynchronizer.isAccepted(
                new Websocket("/a/b/c/w1.websocket", "name1", "description", "endpoint1", "handler1", "engine1").getType()));
    }

    /**
     * Load.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void load() throws ParseException {
        String content =
                "{\"location\":\"/control/control.websocket\",\"handler\":\"control/handler.js\",\"endpoint\":\"mywebsocket\",\"description\":\"Control Websocket\",\"createdBy\":\"system\",\"createdAt\":\"2017-07-06T2:24:12+0000\"}";
        List<Websocket> list = websocketsSynchronizer.parse("/test/test.websocket", content.getBytes());
        assertNotNull(list);
        assertEquals("/test/test.websocket", list.get(0)
                                                 .getLocation());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
