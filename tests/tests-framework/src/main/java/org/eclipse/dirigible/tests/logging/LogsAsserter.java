/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Fail.fail;

public class LogsAsserter {

    private final InMemoryAppender memoryAppender;

    public LogsAsserter(Class<?> loggerClass, Level loggerLevel) {
        this(loggerClass.getName(), loggerLevel);
    }

    public LogsAsserter(String loggerName, Level loggerLevel) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(loggerLevel);

        memoryAppender = new InMemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(memoryAppender);

        memoryAppender.start();
    }

    public void assertLoggedMessage(String expectedMessage, Level expectedLevel) {
        boolean messageLogged = containsMessage(expectedMessage, expectedLevel);
        if (!messageLogged) {
            List<String> loggedMessages = memoryAppender.getLoggedMessages();
            String message = String.format("Missing logged message [%s] with level [%s]. Logged messages: %s", expectedMessage,
                    expectedLevel, loggedMessages);
            fail(message);
        }
    }

    public boolean containsMessage(String expectedMessage, Level expectedLevel) {
        return memoryAppender.contains(expectedMessage, expectedLevel);
    }

    public List<String> getLoggedMessages() {
        return memoryAppender.getLoggedMessages();
    }
}
