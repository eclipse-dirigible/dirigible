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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The Class InMemoryAppender.
 */
class InMemoryAppender extends ListAppender<ILoggingEvent> {

    boolean contains(String string, Level level) {
        return this.list.stream()
                        .anyMatch(event -> event.toString()
                                                .contains(string)
                                && event.getLevel()
                                        .equals(level));
    }

    List<String> getLoggedMessages() {
        return list.stream()
                   .map(e -> e.getMessage())
                   .collect(Collectors.toList());
    }
}
