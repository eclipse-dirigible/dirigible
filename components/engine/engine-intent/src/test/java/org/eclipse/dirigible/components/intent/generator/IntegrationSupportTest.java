/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntegrationSupportTest {

    @Test
    void mapsHttpMethodsCaseInsensitively() {
        assertEquals("post", IntegrationSupport.clientMethod("POST"));
        assertEquals("get", IntegrationSupport.clientMethod("get"));
        assertEquals("post", IntegrationSupport.clientMethod(null), "method defaults to POST");
        assertTrue(IntegrationSupport.isSupportedMethod("PATCH"));
        assertFalse(IntegrationSupport.isSupportedMethod("FETCH"));
    }

    @Test
    void onlyWriteMethodsCarryABody() {
        assertTrue(IntegrationSupport.hasBody("POST"));
        assertTrue(IntegrationSupport.hasBody("put"));
        assertFalse(IntegrationSupport.hasBody("GET"));
        assertFalse(IntegrationSupport.hasBody("DELETE"));
    }

    @Test
    void urlIsAConfigLookupOrALiteral() {
        assertEquals("Configurations.get(\"WAREHOUSE_URL\")", IntegrationSupport.urlExpression("@config:WAREHOUSE_URL"));
        assertEquals("\"https://hooks.example.com/orders\"", IntegrationSupport.urlExpression("https://hooks.example.com/orders"));
    }
}
