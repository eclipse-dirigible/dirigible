/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.engine.cms.access.CmsAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The escapes that keep the platform working once a single grant exists.
 * <p>
 * Content seeding, a scheduled job, a workflow delegate minting a print snapshot and a message
 * listener storing an attachment all write to the CMS with no user and no roles behind them.
 * Denying them the moment an administrator restricts one folder would break seeding rather than
 * secure it, so enforcement engages only for an actual request, never in anonymous mode, and never
 * when it is switched off. Each is asserted here against a service that denies everything, so a
 * passing case can only be the escape.
 */
class DocumentAccessEvaluatorTest {

    private static final String CMS_ROLES_ENABLED = "DIRIGIBLE_CMS_ROLES_ENABLED";
    private static final String PATH = "/reports/q1.pdf";

    /** Denies every path, method and role - the most restrictive rule set there is. */
    private final CmsAccessService denyingService = mock(CmsAccessService.class);

    private final DocumentAccessEvaluator evaluator = new DocumentAccessEvaluator(denyingService);

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @BeforeEach
    void denyEverything() {
        when(denyingService.isAllowed(anyString(), anyString(), any())).thenReturn(false);
    }

    @AfterEach
    void restoreTheKillSwitch() {
        Configuration.remove(CMS_ROLES_ENABLED);
    }

    @Test
    void aRestrictiveRuleAppliesToARequest() {
        assertFalse(evaluator.isReadable(PATH, request), "a request is the one context enforcement applies to");
        assertFalse(evaluator.isWritable(PATH, request));
    }

    @Test
    void thereIsNothingToEnforceWithoutARequest() {
        assertTrue(evaluator.isReadable(PATH, null), "seeding and delegates run with no request, no user and no roles");
        assertTrue(evaluator.isWritable(PATH, null));
    }

    @Test
    void anonymousModeSkipsEnforcementEntirely() {
        try (MockedStatic<Configuration> configuration = mockStatic(Configuration.class, CALLS_REAL_METHODS)) {
            // The mode is the only thing stubbed - every other configuration lookup stays real, so
            // the enforcing case below proves the escape is what carries the assertion after it.
            configuration.when(Configuration::isAnonymousModeEnabled)
                         .thenReturn(false);
            assertFalse(evaluator.isReadable(PATH, request));

            configuration.when(Configuration::isAnonymousModeEnabled)
                         .thenReturn(true);
            assertTrue(evaluator.isReadable(PATH, request), "there is no caller to hold a role in anonymous mode");
            assertTrue(evaluator.isWritable(PATH, request));
        }
    }

    @Test
    void theKillSwitchDisablesEnforcementWholesale() {
        Configuration.set(CMS_ROLES_ENABLED, "false");

        assertTrue(evaluator.isReadable(PATH, request));
        assertTrue(evaluator.isWritable(PATH, request));
    }

    @Test
    void theFlagsFollowTheSameDecision() {
        assertFalse(evaluator.flags(PATH, request)
                             .readable(),
                "an unreadable path is not rendered at all");

        DocumentAccessEvaluator.AccessFlags escaped = evaluator.flags(PATH, null);
        assertTrue(escaped.readable());
        assertFalse(escaped.readOnly(), "writing is allowed too, so nothing is marked read-only");
    }
}
