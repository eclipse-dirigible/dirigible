/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.util;

import static org.junit.jupiter.api.Assertions.fail;
import java.util.List;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * The Class LogsAsserter.
 */
public class LogsAsserter {

    /** The memory appender. */
    private final InMemoryAppender memoryAppender;

    /**
     * Instantiates a new logs asserter.
     *
     * @param loggerClass the logger class
     * @param loggerLevel the logger level
     */
    public LogsAsserter(Class<?> loggerClass, Level loggerLevel) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        logger.setLevel(loggerLevel);

        memoryAppender = new InMemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(memoryAppender);

        memoryAppender.start();
    }

    /**
     * Assert logged message.
     *
     * @param expectedMessage the expected message
     * @param expectedLevel the expected level
     */
    public void assertLoggedMessage(String expectedMessage, Level expectedLevel) {
        boolean messageLogged = memoryAppender.contains(expectedMessage, expectedLevel);
        if (!messageLogged) {
            List<String> loggedMessages = memoryAppender.getAllLoggedMessages();
            String message = String.format("Missing logged message [%s] with level [%s]. Logged messages: %s", expectedMessage,
                    expectedLevel, loggedMessages);
            fail(message);
        }
    }
}
