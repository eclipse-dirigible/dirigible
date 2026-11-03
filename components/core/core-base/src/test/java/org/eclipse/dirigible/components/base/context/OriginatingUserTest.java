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
 * The originating user is bound for exactly one call and never stays on the (pooled) thread
 * (#8228).
 */
class OriginatingUserTest {

    @Test
    void the_user_is_bound_for_the_call_only() {
        String seen = OriginatingUser.callAs("alice", () -> OriginatingUser.get()
                                                                           .orElse(null));

        assertThat(seen).isEqualTo("alice");
        assertThat(OriginatingUser.get()).isEmpty();
    }

    @Test
    void a_nested_call_restores_the_outer_user() {
        String afterInner = OriginatingUser.callAs("alice", () -> {
            assertThat(OriginatingUser.callAs("bob", () -> OriginatingUser.get()
                                                                          .orElse(null))).isEqualTo("bob");
            return OriginatingUser.get()
                                  .orElse(null);
        });

        assertThat(afterInner).isEqualTo("alice");
    }

    @Test
    void no_user_unbinds_the_outer_one_for_the_call() {
        boolean empty = OriginatingUser.callAs("alice", () -> OriginatingUser.callAs(null, () -> OriginatingUser.get()
                                                                                                                .isEmpty()));

        assertThat(empty).isTrue();
        assertThat(OriginatingUser.callAs(" ", () -> OriginatingUser.get()
                                                                    .isEmpty())).isTrue();
    }

    @Test
    void a_throwing_call_does_not_leave_the_user_behind() {
        assertThatThrownBy(() -> OriginatingUser.callAs("alice", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(OriginatingUser.get()).isEmpty();
    }
}
