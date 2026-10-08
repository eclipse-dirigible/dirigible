/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The intent field's {@code normalize:} step (issue #7726). */
class NormalizeTest {

    @Test
    void a_pasted_phone_is_stored_without_its_separators() {
        assertEquals("+359898123456", Normalize.apply("+359 898 123 456", false, " -()", null));
        assertEquals("+359898123456", Normalize.apply("+359-898-123-456", false, " -()", null));
        assertEquals("+35929876543", Normalize.apply("+359 (2) 987-6543", false, " -()", null));
    }

    @Test
    void trim_strip_and_case_run_in_that_order() {
        assertEquals("BG80BNBG96611020345678", Normalize.apply("  bg80 bnbg 9661 1020 3456 78 ", true, " ", "upper"));
        assertEquals("ab-c", Normalize.apply("  AB-C  ", true, null, "lower"));
        assertEquals("x y", Normalize.apply("  x y  ", true, "", null));
    }

    @Test
    void nothing_declared_leaves_the_value_alone_and_null_stays_null() {
        assertEquals(" a b ", Normalize.apply(" a b ", false, null, null));
        assertNull(Normalize.apply(null, true, " ", "upper"));
    }

    @Test
    void stripping_works_on_code_points_outside_the_basic_plane() {
        assertEquals("ab", Normalize.apply("a😀b", false, "😀", null));
    }
}
