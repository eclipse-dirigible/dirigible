/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests;

import org.eclipse.dirigible.integration.tests.api.SecurityIT;
import org.eclipse.dirigible.integration.tests.api.java.messaging.MessagingFacadeIT;
import org.eclipse.dirigible.integration.tests.api.rest.DisabledMultitenantModeIT;
import org.eclipse.dirigible.integration.tests.api.rest.EnabledMultitenantModeIT;
import org.eclipse.dirigible.integration.tests.api.rest.ODataAPIIT;
import org.eclipse.dirigible.integration.tests.ui.tests.CreateNewProjectIT;
import org.eclipse.dirigible.integration.tests.ui.tests.CsvimIT;
import org.eclipse.dirigible.integration.tests.ui.tests.DependsOnIT;
import org.eclipse.dirigible.integration.tests.ui.tests.MailIT;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({//
        MessagingFacadeIT.class, //
        DisabledMultitenantModeIT.class, //
        EnabledMultitenantModeIT.class, //
        ODataAPIIT.class, //
        SecurityIT.class, //
        CreateNewProjectIT.class, //
        DependsOnIT.class, //
        MailIT.class, //
        CsvimIT.class})
public class CommonTestSuite {
    // use this suite class to run tests in specific order if needed
    // it is not configured to be executed automatically by the maven plugins
}


