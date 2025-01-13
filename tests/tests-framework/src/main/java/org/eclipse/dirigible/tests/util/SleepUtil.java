/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepUtil.class);

    public static void sleepSeconds(int seconds) {
        sleepMillis(seconds * 1000L);
    }

    public static void sleepMillis(long millis) {
        sleepMillis(millis, "Failed to fall asleep for [" + millis + "] millis");
    }

    public static void sleepMillis(long millis, String errorMessage) {
        LOGGER.info("Falling asleep for [{}] millis...", millis);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
