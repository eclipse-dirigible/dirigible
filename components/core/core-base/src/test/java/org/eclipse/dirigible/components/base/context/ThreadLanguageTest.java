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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A language bound for a block restores the one bound before it (#8776): a render inside an event
 * handler must leave the handler bound to the event's language, not to none.
 */
class ThreadLanguageTest {

    @AfterEach
    void clear() {
        ThreadLanguage.clear();
    }

    @Test
    void a_block_restores_the_language_bound_before_it() {
        ThreadLanguage.set("bg");

        try (ThreadLanguage.Scope scope = ThreadLanguage.bind("de")) {
            assertThat(ThreadLanguage.get()).contains("de");
        }

        assertThat(ThreadLanguage.get()).contains("bg");
    }

    @Test
    void a_block_without_a_language_before_it_leaves_none_after_it() {
        try (ThreadLanguage.Scope scope = ThreadLanguage.bind("de")) {
            assertThat(ThreadLanguage.get()).contains("de");
        }

        assertThat(ThreadLanguage.get()).isEmpty();
    }

    @Test
    void a_blank_language_binds_none_for_the_block_and_still_restores() {
        ThreadLanguage.set("bg");

        try (ThreadLanguage.Scope scope = ThreadLanguage.bind(" ")) {
            assertThat(ThreadLanguage.get()).isEmpty();
        }

        assertThat(ThreadLanguage.get()).contains("bg");
    }
}
