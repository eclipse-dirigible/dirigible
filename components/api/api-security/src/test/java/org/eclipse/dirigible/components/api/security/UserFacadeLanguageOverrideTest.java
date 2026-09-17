/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The thread-bound language override that lets an in-process render (a document snapshot, a mailed
 * PDF attachment) resolve the multilingual overlay in the language the artefact is produced in,
 * where there is no request to carry Accept-Language (#6947).
 */
class UserFacadeLanguageOverrideTest {

    @AfterEach
    void clearTheOverride() {
        UserFacade.clearLanguage();
    }

    @Test
    void the_override_is_returned_over_the_absent_request_language() {
        UserFacade.setLanguage("bg");

        assertThat(UserFacade.getLanguage()).isEqualTo("bg");
    }

    @Test
    void clearing_the_override_falls_back_to_the_request_language() {
        UserFacade.setLanguage("bg");
        UserFacade.clearLanguage();

        // No valid HTTP request in a plain unit test, so with the override cleared the language is
        // unresolved.
        assertThat(UserFacade.getLanguage()).isNull();
    }

    @Test
    void a_null_language_leaves_the_override_unset() {
        UserFacade.setLanguage(null);

        assertThat(UserFacade.getLanguage()).isNull();
    }

    @Test
    void a_blank_language_leaves_the_override_unset() {
        UserFacade.setLanguage("   ");

        assertThat(UserFacade.getLanguage()).isNull();
    }
}
