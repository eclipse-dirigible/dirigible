/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * A logged value must not be able to forge a second log line, and must otherwise stay recognisable
 * to whoever reads the line.
 */
class LoggedValueTest {

    @Test
    void every_kind_of_line_break_collapses_to_one_placeholder() {
        assertEquals("Sales Invoice_forged entry", LoggedValue.of("Sales Invoice\r\nforged entry"));
        assertEquals("a_b_c_d", LoggedValue.of("a\nb\rc d"));
    }

    @Test
    void an_ordinary_name_is_kept_verbatim() {
        assertEquals("Customer.Name = 'Ünïcode & <tags>'", LoggedValue.of("Customer.Name = 'Ünïcode & <tags>'"));
        assertEquals("42", LoggedValue.of(42));
    }

    @Test
    void null_stays_null_so_the_logger_renders_it_as_such() {
        assertNull(LoggedValue.of(null));
    }
}
