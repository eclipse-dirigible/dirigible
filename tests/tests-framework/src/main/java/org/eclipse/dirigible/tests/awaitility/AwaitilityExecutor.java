/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.awaitility;

import org.eclipse.dirigible.tests.restassured.CallableNoResultAndNoException;

import static org.junit.jupiter.api.Assertions.fail;

public class AwaitilityExecutor {

    public static void execute(String failMessage, CallableNoResultAndNoException callable) {
        try {
            callable.call();
        } catch (Exception ex) {
            fail(failMessage, ex);
        }
    }
}
