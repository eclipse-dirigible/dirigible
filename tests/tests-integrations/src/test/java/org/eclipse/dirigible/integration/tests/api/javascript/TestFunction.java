/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.javascript;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.dirigible.graalium.core.graal.globals.GlobalFunction;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.DynamicTest;

class TestFunction implements GlobalFunction {

    private final List<DynamicTest> dynamicTests = new ArrayList<>();

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public Object execute(Value... arguments) {
        String testName = arguments[0].asString();
        Value testFunc = arguments[1];
        DynamicTest dynamicTest = DynamicTest.dynamicTest(testName, testFunc::execute);
        dynamicTests.add(dynamicTest);
        return null;
    }

    public List<DynamicTest> getDynamicTests() {
        return dynamicTests;
    }
}
