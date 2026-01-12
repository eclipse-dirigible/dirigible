/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.util;

import java.util.concurrent.TimeUnit;

public class SleepUtil {

    public static void sleepMillis(TimeUnit unit, long duration) {
        sleepMillis(unit.toMillis(duration));
    }

    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            throw new IllegalStateException("Failed to fall asleep for [" + millis + "] millis", ex);
        }
    }
}
