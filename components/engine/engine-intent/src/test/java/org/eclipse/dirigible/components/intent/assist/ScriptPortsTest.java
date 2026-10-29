/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A TypeScript source and a JavaScript module are ported to the Java file of the same folder and
 * name; a declaration, a plain {@code .js} and Java itself are not.
 */
class ScriptPortsTest {

    @Test
    void aTypeScriptSourceAndAJavaScriptModuleArePorted() {
        assertTrue(ScriptPorts.isPortable("widgets/api/SalariesService.ts"));
        assertTrue(ScriptPorts.isPortable("jobs/count-days.mjs"));
    }

    @Test
    void aDeclarationAPlainJavaScriptFileAndJavaAreNot() {
        assertFalse(ScriptPorts.isPortable("types/orders.d.ts"));
        assertFalse(ScriptPorts.isPortable("navigations/salaries/service.js"));
        assertFalse(ScriptPorts.isPortable("custom/Fees.java"));
    }

    @Test
    void thePortIsTheJavaFileOfTheSameFolderAndName() {
        assertEquals("widgets/api/SalariesService.java", ScriptPorts.javaPathOf("widgets/api/SalariesService.ts"));
        assertEquals("jobs/count-days.java", ScriptPorts.javaPathOf("jobs/count-days.mjs"));
    }

    @Test
    void theLanguageNamesTheFence() {
        assertEquals("typescript", ScriptPorts.languageOf("widgets/api/SalariesService.ts"));
        assertEquals("javascript", ScriptPorts.languageOf("jobs/count-days.mjs"));
    }
}
