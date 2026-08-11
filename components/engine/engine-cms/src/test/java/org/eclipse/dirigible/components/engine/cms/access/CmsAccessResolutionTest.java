/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.access;

import static org.eclipse.dirigible.components.engine.cms.access.CmsAccessGrant.METHOD_READ;
import static org.eclipse.dirigible.components.engine.cms.access.CmsAccessGrant.METHOD_WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/**
 * The CMS access semantics. These tests ARE the specification: the implementation being replaced
 * had three disagreeing answers to the questions below, so each rule is pinned here rather than
 * left to be inferred from whichever caller one happens to read.
 */
class CmsAccessResolutionTest {

    private static final Predicate<String> READER = role -> Set.of("Reader")
                                                               .contains(role);
    private static final Predicate<String> NOBODY = role -> false;

    @Test
    void anUnconstrainedPathIsOpen() {
        assertTrue(CmsAccessResolution.isAllowed(List.of(), "/reports/q1.pdf", METHOD_READ, NOBODY));
    }

    @Test
    void aGrantIsInheritedByEverythingUnderIt() {
        List<CmsAccessGrant> grants = List.of(new CmsAccessGrant("/reports", METHOD_READ, "Reader"));

        assertTrue(CmsAccessResolution.isAllowed(grants, "/reports/2026/q1.pdf", METHOD_READ, READER));
        assertFalse(CmsAccessResolution.isAllowed(grants, "/reports/2026/q1.pdf", METHOD_READ, NOBODY));
    }

    @Test
    void aSiblingBranchIsUnaffected() {
        List<CmsAccessGrant> grants = List.of(new CmsAccessGrant("/reports", METHOD_READ, "Reader"));

        assertTrue(CmsAccessResolution.isAllowed(grants, "/invoices/2026.pdf", METHOD_READ, NOBODY));
    }

    @Test
    void holdingAnyGrantedRoleSuffices() {
        List<CmsAccessGrant> grants =
                List.of(new CmsAccessGrant("/reports", METHOD_READ, "Auditor"), new CmsAccessGrant("/reports", METHOD_READ, "Reader"));

        // The JavaScript listing required EVERY listed role, which made a second grant a restriction
        // instead of an alternative. Any granted role is what the platform's HTTP constraints mean.
        assertTrue(CmsAccessResolution.isAllowed(grants, "/reports", METHOD_READ, READER));
    }

    @Test
    void theMostSpecificPathDecidesSoAChildCanBeOpened() {
        List<CmsAccessGrant> grants = List.of(new CmsAccessGrant("/reports", METHOD_READ, "Auditor"),
                new CmsAccessGrant("/reports/public", METHOD_READ, "Reader"));

        assertTrue(CmsAccessResolution.isAllowed(grants, "/reports/public/q1.pdf", METHOD_READ, READER));
        assertFalse(CmsAccessResolution.isAllowed(grants, "/reports/private/q1.pdf", METHOD_READ, READER));
    }

    @Test
    void readAndWriteAreResolvedIndependently() {
        List<CmsAccessGrant> grants =
                List.of(new CmsAccessGrant("/reports", METHOD_READ, "Reader"), new CmsAccessGrant("/reports", METHOD_WRITE, "Editor"));

        assertTrue(CmsAccessResolution.isAllowed(grants, "/reports", METHOD_READ, READER));
        assertFalse(CmsAccessResolution.isAllowed(grants, "/reports", METHOD_WRITE, READER));
    }

    @Test
    void aRootGrantCoversEverything() {
        List<CmsAccessGrant> grants = List.of(new CmsAccessGrant("/", METHOD_READ, "Reader"));

        assertTrue(CmsAccessResolution.isAllowed(grants, "/anything/at/all", METHOD_READ, READER));
        assertFalse(CmsAccessResolution.isAllowed(grants, "/anything/at/all", METHOD_READ, NOBODY));
    }

    @Test
    void ancestryRunsFromTheRootToThePath() {
        assertEquals(List.of("/", "/a", "/a/b", "/a/b/c.txt"), CmsAccessResolution.ancestry("/a/b/c.txt"));
        assertEquals(List.of("/"), CmsAccessResolution.ancestry("/"));
    }

    @Test
    void pathsAreNormalizedSoAuthoringVariantsMatch() {
        assertEquals("/reports", CmsAccessResolution.normalize("reports"));
        assertEquals("/reports", CmsAccessResolution.normalize("/reports/"));
        assertEquals("/reports/q1", CmsAccessResolution.normalize("//reports//q1//"));
        assertEquals("/", CmsAccessResolution.normalize(null));
        assertEquals("/", CmsAccessResolution.normalize("  "));
    }

    @Test
    void aGrantAuthoredWithoutALeadingSeparatorStillApplies() {
        List<CmsAccessGrant> grants = List.of(new CmsAccessGrant("reports/", METHOD_READ, "Reader"));

        assertTrue(CmsAccessResolution.isAllowed(grants, "/reports/q1.pdf", METHOD_READ, READER));
        assertFalse(CmsAccessResolution.isAllowed(grants, "/reports/q1.pdf", METHOD_READ, NOBODY));
    }

    @Test
    void decidingReturnsOnlyTheDeepestMatchingGrants() {
        List<CmsAccessGrant> grants =
                List.of(new CmsAccessGrant("/a", METHOD_READ, "Outer"), new CmsAccessGrant("/a/b", METHOD_READ, "Inner"));

        List<CmsAccessGrant> deciding = CmsAccessResolution.deciding(grants, "/a/b/c", METHOD_READ);

        assertEquals(1, deciding.size());
        assertEquals("Inner", deciding.get(0)
                                      .role());
    }
}
