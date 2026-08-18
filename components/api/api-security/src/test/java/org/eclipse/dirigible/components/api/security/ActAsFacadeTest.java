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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The expiry decision behind the act-as (delegated entry) window: an arming that is never exited
 * must stop being honoured on its own (#6694).
 */
class ActAsFacadeTest {

    private static final long NOW = 1_800_000_000_000L;

    @AfterEach
    void restoreTheDefaultWindow() {
        DirigibleConfig.ACT_AS_TTL_SECONDS.setStringValue(DirigibleConfig.ACT_AS_TTL_SECONDS.getDefaultValue());
    }

    @Test
    void a_fresh_arming_is_honoured() {
        assertThat(ActAsFacade.isExpired(Long.toString(NOW - 60_000), NOW)).isFalse();
    }

    @Test
    void an_arming_older_than_the_window_is_dropped() {
        long defaultWindowMillis = Integer.parseInt(DirigibleConfig.ACT_AS_TTL_SECONDS.getDefaultValue()) * 1000L;

        assertThat(ActAsFacade.isExpired(Long.toString(NOW - defaultWindowMillis - 1), NOW)).isTrue();
    }

    @Test
    void the_window_follows_the_configuration() {
        DirigibleConfig.ACT_AS_TTL_SECONDS.setStringValue("60");

        assertThat(ActAsFacade.isExpired(Long.toString(NOW - 59_000), NOW)).isFalse();
        assertThat(ActAsFacade.isExpired(Long.toString(NOW - 61_000), NOW)).isTrue();
    }

    /** Fails closed: an armed state we cannot date is an armed state we cannot trust to end. */
    @Test
    void an_undatable_arming_is_dropped() {
        assertThat(ActAsFacade.isExpired(null, NOW)).isTrue();
        assertThat(ActAsFacade.isExpired("  ", NOW)).isTrue();
        assertThat(ActAsFacade.isExpired("not-a-timestamp", NOW)).isTrue();
    }

}
