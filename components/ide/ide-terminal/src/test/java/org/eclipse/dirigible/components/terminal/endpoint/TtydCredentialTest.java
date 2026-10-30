/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * Every platform instance starts the terminal backend with a credential of its own.
 */
class TtydCredentialTest {

    @Test
    void generates_a_user_and_password_pair() {
        String credential = TtydCredential.generate()
                                          .credential();

        assertTrue(credential.matches("dirigible:[A-Za-z0-9_-]{43}"), credential);
    }

    @Test
    void generates_a_different_password_each_time() {
        assertNotEquals(TtydCredential.generate()
                                      .credential(),
                TtydCredential.generate()
                              .credential());
    }

    @Test
    void the_token_is_the_base64_form_of_the_credential() {
        TtydCredential credential = TtydCredential.generate();

        assertEquals(credential.credential(), new String(Base64.getDecoder()
                                                               .decode(credential.token()),
                StandardCharsets.UTF_8));
    }
}
