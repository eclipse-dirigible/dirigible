/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * The originating user is bound for exactly one scope and never stays on the (pooled) thread
 * (#8228).
 */
class OriginatingUserTest {

    @Test
    void the_user_is_bound_for_the_scope_only() {
        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
            assertThat(OriginatingUser.get()).contains("alice");
        }

        assertThat(OriginatingUser.get()).isEmpty();
    }

    @Test
    void a_nested_scope_restores_the_outer_user() {
        try (OriginatingUser.Binding outer = OriginatingUser.bind("alice")) {
            try (OriginatingUser.Binding inner = OriginatingUser.bind("bob")) {
                assertThat(OriginatingUser.get()).contains("bob");
            }
            assertThat(OriginatingUser.get()).contains("alice");
        }
    }

    @Test
    void no_user_unbinds_the_outer_one_for_the_scope() {
        try (OriginatingUser.Binding outer = OriginatingUser.bind("alice")) {
            try (OriginatingUser.Binding none = OriginatingUser.bind(null)) {
                assertThat(OriginatingUser.get()).isEmpty();
            }
            try (OriginatingUser.Binding blank = OriginatingUser.bind(" ")) {
                assertThat(OriginatingUser.get()).isEmpty();
            }
            assertThat(OriginatingUser.get()).contains("alice");
        }
    }

    @Test
    void a_throwing_scope_does_not_leave_the_user_behind() {
        assertThatThrownBy(() -> {
            try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
                throw new IllegalStateException("boom");
            }
        }).isInstanceOf(IllegalStateException.class);

        assertThat(OriginatingUser.get()).isEmpty();
    }
}
