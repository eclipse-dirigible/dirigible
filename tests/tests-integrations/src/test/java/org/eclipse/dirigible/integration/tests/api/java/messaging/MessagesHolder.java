/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * used by <b>background-handler.js as well</b>
 */
public class MessagesHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesHolder.class);

    private static String latestReceivedMessage;
    private static String latestReceivedError;

    public static String getLatestReceivedMessage() {
        LOGGER.info("Getting latest received message [{}]", latestReceivedMessage);
        return latestReceivedMessage;
    }

    public static void setLatestReceivedMessage(String latestReceivedMessage) {
        LOGGER.info("Setting latest received message from [{}] to [{}]", MessagesHolder.latestReceivedMessage, latestReceivedMessage);
        MessagesHolder.latestReceivedMessage = latestReceivedMessage;
    }

    public static void clearLatestReceivedMessage() {
        LOGGER.info("Clearing latest received message [{}]", latestReceivedMessage);
        latestReceivedMessage = null;
    }

    public static String getLatestReceivedError() {
        LOGGER.info("Getting latest received error [{}]", latestReceivedError);
        return latestReceivedError;
    }

    public static void setLatestReceivedError(String latestReceivedError) {
        LOGGER.info("Setting latest received error from [{}] to [{}]", MessagesHolder.latestReceivedError, latestReceivedError);
        MessagesHolder.latestReceivedError = latestReceivedError;
    }

    public static void clearLatestReceivedError() {
        LOGGER.info("Clearing latest received error [{}]", latestReceivedError);
        latestReceivedError = null;
    }
}
