/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.PredefinedProjectIT;
import org.eclipse.dirigible.tests.base.TestProject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapping on arrival, end to end (dirigible #6769): the Intent Editor's Generate, then the
 * Workbench's Publish all, then the three outcomes of a gated and mapped arrival over both a
 * webhook and the broker - an envelope ingested with its business keys resolved to foreign keys,
 * one this application does not understand acknowledged and ignored, and one naming a register row
 * that does not exist rejected rather than stored half-resolved.
 */
public class IntentInboundMappingSampleIT extends PredefinedProjectIT {

    @Autowired
    private SampleIntentInboundMappingTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}
