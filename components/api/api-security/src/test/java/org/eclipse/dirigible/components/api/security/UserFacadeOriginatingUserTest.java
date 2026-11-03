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

import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.junit.jupiter.api.Test;

/**
 * Outside a request, the user whose action raised the event being handled names the writer, so the
 * audit columns of a row a listener writes are no longer the {@code guest} fallback (#8228).
 */
class UserFacadeOriginatingUserTest {

    @Test
    void the_originating_user_names_a_thread_without_a_request() {
        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
            assertThat(UserFacade.getName()).isEqualTo("alice");
            assertThat(UserFacade.findName()).contains("alice");
        }
    }

    @Test
    void without_one_the_name_is_unknown_and_falls_back_to_guest() {
        assertThat(UserFacade.findName()).isEmpty();
        assertThat(UserFacade.getName()).isEqualTo("guest");
    }
}
