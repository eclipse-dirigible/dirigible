/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs a test class against an intent application in-process - the unit-test level for the
 * hand-written Java of {@code custom/} (dirigible #7643). The project's generated {@code gen/} and
 * hand-written {@code custom/} classes run on a private in-memory H2 database exactly as the
 * platform runs them: entities through their generated repositories (calculated fields,
 * validations, checks, events), components wired by the platform's own bean container, document
 * numbers from the project's {@code .numbers} series. There is no web server, no synchronizer, no
 * broker and no IDE, so a test takes milliseconds.
 *
 * <p>
 * The classes come from the test classpath: the module's build compiles the project folder as a
 * test source root, so {@code gen/}, {@code custom/} and the tests in {@code custom/test/} build
 * together - the platform itself never compiles {@code custom/test/}. The application starts once
 * per JVM and project; before each test every table is emptied, identities and number series
 * restart, and the recorded messages are cleared. Tests of one JVM share the application's static
 * entry points, so slice tests must not run in parallel.
 *
 * <pre>
 * {@literal @}IntentSlice
 * class LineVatActionTest {
 *     {@literal @}Test
 *     void roundsHalfUpToTheCent(Slice slice) {
 *         InvoiceLineEntity line = slice.given(InvoiceLineEntity.class)
 *                                       .with("Quantity", 1).with("UnitPrice", "2.90").with("VatRate", 5)
 *                                       .saved();
 *         assertEquals(new BigDecimal("0.15"), line.VatAmount);
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(IntentSliceExtension.class)
public @interface IntentSlice {

    /**
     * The project folder - the one holding {@code gen/} and {@code custom/} - relative to the working
     * directory. The default suits a module whose root is the project.
     *
     * @return the project folder
     */
    String project() default ".";
}
