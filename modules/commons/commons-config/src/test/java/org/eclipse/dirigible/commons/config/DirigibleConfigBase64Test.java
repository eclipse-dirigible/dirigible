/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.After;
import org.junit.Test;

/**
 * Verifies that a Base64-only configuration refuses a plain-text value with a message naming the
 * variable, instead of aborting boot with a bare decoder error.
 */
public class DirigibleConfigBase64Test {

    private static final String KEY = DirigibleConfig.BASIC_ADMIN_PASS.getKey();

    @After
    public void restoreDefault() {
        Configuration.remove(KEY);
    }

    @Test
    public void plainTextValueIsRefusedNamingTheVariable() {
        // a plain value with a non-Base64 character ('-'), as in the issue
        Configuration.set(KEY, "qahunt-pw");
        try {
            DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value();
            fail("A non-Base64 value should be refused");
        } catch (InvalidConfigException ex) {
            assertEquals(KEY, ex.getConfigKey());
            assertTrue("message should name the variable: " + ex.getMessage(), ex.getMessage()
                                                                                 .contains(KEY));
            assertTrue("message should mention Base64: " + ex.getMessage(), ex.getMessage()
                                                                              .contains("Base64"));
        }
    }

    @Test
    public void validBase64IsDecoded() {
        Configuration.set(KEY, Base64.getEncoder()
                                     .encodeToString("s3cr3t".getBytes(StandardCharsets.UTF_8)));

        assertEquals("s3cr3t", DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value());
    }
}
