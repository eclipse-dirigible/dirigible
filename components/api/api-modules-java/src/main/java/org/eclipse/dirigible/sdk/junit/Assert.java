/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.junit;

import java.util.Objects;

/**
 * xUnit-style assertion helpers that throw {@link AssertionError} on failure, so they integrate
 * with every JVM test runner without further wiring (JUnit 5, TestNG, Spock, plain {@code @Test}
 * methods invoked from a controller). Suitable for smoke tests and ad-hoc verification scripts
 * written as plain Java client code.
 * <p>
 * For full-blown test suites — fixtures, parameterised tests, lifecycle hooks, parallel execution —
 * pull JUnit Jupiter into the project and use {@code org.junit.jupiter.api.Assertions} directly.
 * This class deliberately covers only the small overlap that is identical to the TS
 * {@code @aerokit/sdk/junit} surface.
 */
public final class Assert {

    private Assert() {}

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected condition to be false");
        }
    }

    public static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: <" + expected + ">, actual: <" + actual + ">");
        }
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " — expected: <" + expected + ">, actual: <" + actual + ">");
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError("Expected values to differ; both were: <" + actual + ">");
        }
    }

    public static void assertNotEquals(String message, Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError(message + " — both values were: <" + actual + ">");
        }
    }

    public static void assertNull(Object value) {
        if (value != null) {
            throw new AssertionError("Expected null, was: <" + value + ">");
        }
    }

    public static void assertNotNull(Object value) {
        if (value == null) {
            throw new AssertionError("Expected non-null");
        }
    }

    public static void fail() {
        throw new AssertionError("Test failure");
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
