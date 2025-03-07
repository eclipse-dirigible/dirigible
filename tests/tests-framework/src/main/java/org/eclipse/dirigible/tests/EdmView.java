/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class EdmView {

    private final Browser browser;
    private final WorkbenchFactory workbenchFactory;

    public EdmView(Browser browser, WorkbenchFactory workbenchFactory) {
        this.browser = browser;
        this.workbenchFactory = workbenchFactory;
    }

    public void regenerate(String projectName, String edmFileName) {
        Workbench workbench = workbenchFactory.create(browser);
        workbench.openFile(projectName, edmFileName);

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Regenerate");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Generated from model");
    }
}

