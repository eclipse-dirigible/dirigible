/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.console.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * The Console Logging Appender.
 */
public class ConsoleLoggingAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Append.
     *
     * @param event the event
     */
    @Override
    protected void append(ILoggingEvent event) {
        ConsoleLogRecord record = new ConsoleLogRecord(event.getLevel()
                                                            .toString(),
                event.getFormattedMessage(), event.getTimeStamp());
        ConsoleWebsocketHandler.distribute(record);
    }

}
