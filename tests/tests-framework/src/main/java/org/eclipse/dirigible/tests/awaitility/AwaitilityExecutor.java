/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.awaitility;

import org.eclipse.dirigible.tests.restassured.CallableNoResultAndNoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class AwaitilityExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwaitilityExecutor.class);

    public static void execute(String failMessage, CallableNoResultAndNoException callable) {
        try {
            callable.call();
        } catch (Exception ex) {
            fail(failMessage, ex);
        }
    }

    public static void execute(String failMessage, CallableNoResultAndNoException callable, int timeoutSeconds) {
        try {
            await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                   .pollInterval(500, TimeUnit.MILLISECONDS)
                   .until(() -> {
                       try {
                           callable.call();
                           return true;
                       } catch (Throwable ex) {
                           LOGGER.warn("An error occur. Will try again.", ex);
                           return false;
                       }
                   });
            callable.call();
        } catch (Exception ex) {
            fail(failMessage, ex);
        }
    }
}
