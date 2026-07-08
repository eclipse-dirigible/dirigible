/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base for Selenide/Chrome UI integration tests. Tagged {@code "ui"} (inherited by every subclass,
 * incl. the sample-project ITs) so the heavy browser-driven suite runs in the nightly/master full
 * run and is excluded from the per-PR smoke run. A UI test that must run on every PR opts back in
 * with an explicit {@code @Tag("smoke")} on the class; the smoke selector is the tag expression
 * {@code "!ui | smoke"}.
 */
@Tag("ui")
public abstract class UserInterfaceIntegrationTest extends IntegrationTest {

    @Autowired
    protected Browser browser;

    @Autowired
    protected IDE ide;

    @AfterEach
    final void closeBrowser() {
        browser.close();
    }
}
