/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class EdmView {

    private final Browser browser;
    private final WorkbenchFactory workbenchFactory;
    private final IRepository repository;

    public EdmView(Browser browser, WorkbenchFactory workbenchFactory, IRepository repository) {
        this.browser = browser;
        this.workbenchFactory = workbenchFactory;
        this.repository = repository;
    }

    public void regenerate(String projectName, String edmFileName) {
        Workbench workbench = workbenchFactory.create(browser);
        workbench.openFile(projectName, edmFileName);

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Regenerate");
        // Wait for the DURABLE effect of the regeneration - the generated sources landing in the
        // project's gen folder - not the transient "Generated from model" toast. On slow CI runners
        // the cross-frame text sweep can outlast the toast, and the sweep's timeout fallback reloads
        // the page, destroying it for good (failure screenshots showed the toast on screen and the
        // gen folder already created while the assertion still failed - the recurring DependsOnIT
        // flake that also hit master).
        String genPath = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + projectName + "/gen";
        Awaitility.await()
                  .atMost(120, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> repository.hasCollection(genPath));
    }
}
