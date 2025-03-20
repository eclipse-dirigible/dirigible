/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.api.javascript.cms.CmsSuiteIT;
import org.eclipse.dirigible.integration.tests.ui.tests.camel.CamelExtractTransformLoadJdbcIT;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Ordered Test Suite")
@SelectClasses({MultitenancyIT.class, CmsSuiteIT.class, BPMStarterTemplateIT.class, ApproveLeaveRequestBpmIT.class,
        CamelExtractTransformLoadJdbcIT.class})
class OrderedTestSuite {
    // use this suite class to run tests in specific order if needed
    // it is not configured to be executed automatically by the maven plugins
}


