/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The outputs a regeneration removes because the template no longer writes them there (#7889).
 */
class ModelTemplateAdaptersTest {

    private static final String REPORT_FILE_TEMPLATE = "template-application-ui-harmonia-java/template/template-report-file.js";

    @Test
    void aReportsCasePreservedPageFolderIsRetired() {
        Map<String, Object> parameters =
                Map.of("genFolderName", "OverdueInvoices", "javaGenFolderName", "overdueinvoices", "name", "OverdueInvoices");
        assertEquals(List.of("OverdueInvoices/reports/OverdueInvoices"),
                ModelTemplateAdapters.retiredOutputs(REPORT_FILE_TEMPLATE, parameters));
    }

    @Test
    void aReportWhoseFolderNeedsNoSanitizingRetiresNothing() {
        Map<String, Object> parameters = Map.of("genFolderName", "revenue", "javaGenFolderName", "revenue", "name", "Revenue");
        assertTrue(ModelTemplateAdapters.retiredOutputs(REPORT_FILE_TEMPLATE, parameters)
                                        .isEmpty());
    }

    @Test
    void anotherTemplateRetiresNothing() {
        Map<String, Object> parameters = Map.of("genFolderName", "Sales", "javaGenFolderName", "sales", "name", "Sales");
        assertTrue(ModelTemplateAdapters.retiredOutputs("template-application-ui-harmonia-java/template/template.js", parameters)
                                        .isEmpty());
    }

    /**
     * A relative moment as a DATE parameter's initial (#8506) is handed to the repository template for
     * per-request resolution, in its canonical spelling; a fixed date, a bare CURRENT_DATE (its own
     * template branch), a non-date parameter and a malformed moment are not.
     */
    @Test
    void aRelativeMomentInitialIsResolvedByTheRepository() {
        assertEquals("CURRENT_DATE-P30D", ModelTemplateAdapters.reportMoment("DATE", "CURRENT_DATE-P30D"));
        assertEquals("START_OF_MONTH+P1M-P1D", ModelTemplateAdapters.reportMoment("DATE", "start_of_month + P1M - P1D"));
        assertEquals("START_OF_YEAR", ModelTemplateAdapters.reportMoment("DATE", "START_OF_YEAR"));
        assertEquals("START_OF_QUARTER-P1M2W", ModelTemplateAdapters.reportMoment("DATE", "START_OF_QUARTER-P1M2W"));
        assertNull(ModelTemplateAdapters.reportMoment("DATE", "CURRENT_DATE"));
        assertNull(ModelTemplateAdapters.reportMoment("DATE", "2026-01-01"));
        assertNull(ModelTemplateAdapters.reportMoment("INTEGER", "START_OF_YEAR"));
        assertNull(ModelTemplateAdapters.reportMoment("DATE", "CURRENT_DATE-PT1H"));
        assertNull(ModelTemplateAdapters.reportMoment("DATE", "START_OF_WEEK"));
        assertNull(ModelTemplateAdapters.reportMoment("DATE", "CURRENT_DATE-P"));
    }
}
