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

import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CreateNewFileIT extends UserInterfaceIntegrationTest {

    @BeforeEach
    void setUp() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());
    }

    @ParameterizedTest
    @EnumSource(NewFileOption.class)
    void test(NewFileOption newFileOption) {
        Workbench workbench = ide.openWorkbench();
        workbench.createFileInProject(this.getClass()
                                          .getSimpleName(),
                newFileOption.getOptionName());

        workbench.openFile(newFileOption.getNewFileName());

        assertFileTabIsOpen(newFileOption);
    }

    private void assertFileTabIsOpen(NewFileOption newFileOption) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag",
                newFileOption.getNewFileName());
    }

}
