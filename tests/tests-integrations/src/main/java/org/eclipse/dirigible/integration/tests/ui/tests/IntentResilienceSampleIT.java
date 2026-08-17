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
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Deploys and verifies {@link SampleIntentResilienceTestProject} - the step-resilience sample
 * (dirigible #6762) driven through the browser IDE exactly as a developer would: the Intent
 * Editor's Generate, then the Workbench's Publish all, then the two provisioning outcomes over
 * REST. Tagged {@code slow}: the sample's declared PT10S retry cycles put ~2 minutes of deliberate
 * waiting on the clock.
 */
@Tag("slow")
public class IntentResilienceSampleIT extends PredefinedProjectIT {

    @Autowired
    private SampleIntentResilienceTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}
