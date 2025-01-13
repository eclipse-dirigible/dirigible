/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheFacadeTest {

    private static final String KEY = "key1";
    private static final String VALUE = "value1";

    @Test
    void testCache() {
        assertMissingEntry();

        CacheFacade.set(KEY, VALUE);
        assertThat(CacheFacade.contains(KEY)).isTrue();
        assertThat(CacheFacade.get(KEY)).isEqualTo(VALUE);

        CacheFacade.delete(KEY);
        assertMissingEntry();

        CacheFacade.set(KEY, VALUE);
        CacheFacade.clear();
        assertMissingEntry();

    }

    private static void assertMissingEntry() {
        assertThat(CacheFacade.contains(KEY)).isFalse();
        assertThat(CacheFacade.get(KEY)).isNull();
    }
}
