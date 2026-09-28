/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.sdk.db.ConfirmationRequiredException;
import org.eclipse.dirigible.sdk.db.Warning;
import org.junit.jupiter.api.Test;

class WarningsTest {

    @Test
    void confirmed_codes_are_the_comma_separated_header_trimmed() {
        assertEquals(Set.of("Customer.duplicate.0", "Product.compare.1"),
                Warnings.confirmedCodes(" Customer.duplicate.0 ,Product.compare.1,, "));
        assertEquals(Set.of(), Warnings.confirmedCodes(null));
        assertEquals(Set.of(), Warnings.confirmedCodes("  "));
    }

    @Test
    void a_write_with_no_http_caller_is_not_stopped() {
        // A process step or a job has nobody to ask - the soft tier advises a person, it never
        // refuses a server-side write.
        assertDoesNotThrow(() -> Warnings.requireConfirmed(List.of(new Warning("Customer.duplicate.0", "exists"))));
    }

    @Test
    void the_exception_carries_every_warning_and_joins_their_messages() {
        ConfirmationRequiredException e = new ConfirmationRequiredException(List.of(new Warning("a", "First"), new Warning("b", "Second")));
        assertEquals("First; Second", e.getMessage());
        assertEquals(2, e.getWarnings()
                         .size());
    }

}
