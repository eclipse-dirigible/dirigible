/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The alphabet a tenant id may use. The dot is the interesting rejection: it is legal in a host
 * name but would make the identity provider group {@code <tenantId>.<appId>.<role>} unparseable, so
 * a tenant granted by such a group could never be entered.
 */
class TenantIdsTest {

    @ParameterizedTest
    @ValueSource(strings = {"acme", "globex", "a", "acme-corp", "a1", "1a", "tenant-1-2-3",
            "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabc"})
    void validIds(String tenantId) {
        assertTrue(TenantIds.isValid(tenantId), tenantId + " should be a valid tenant id");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "acme.corp", "codbex.library", "-acme", "acme-", "acme corp", "acme_corp", "acme/corp", "acme:corp",
            "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcd"})
    void invalidIds(String tenantId) {
        assertFalse(TenantIds.isValid(tenantId), tenantId + " should be refused as a tenant id");
    }

    @Test
    void theMessageNamesTheOffendingId() {
        assertTrue(TenantIds.invalidMessage("acme.corp")
                            .contains("acme.corp"));
    }
}
