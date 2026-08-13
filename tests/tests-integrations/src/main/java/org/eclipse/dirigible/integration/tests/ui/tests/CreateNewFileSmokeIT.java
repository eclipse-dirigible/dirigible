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

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * The per-PR slice of the new-file journey: one representative {@link NewFileOption} per
 * heavyweight editor family - Monaco (TypeScript service), the BPMN modeler, the EDM modeler and
 * the form builder - so a PR that breaks the create-file dialog or an editor bootstrap fails fast.
 * The exhaustive sweep over every option stays in {@link CreateNewFileIT}, which runs in the
 * nightly/master {@code ui} shard; keep this list short, the smoke gate pays for it on every PR.
 */
@Tag("smoke")
public class CreateNewFileSmokeIT extends UserInterfaceIntegrationTest {

    private static final List<NewFileOption> REPRESENTATIVE_OPTIONS = List.of(NewFileOption.TYPESCRIPT_SERVICE,
            NewFileOption.BUSINESS_PROCESS_MODEL, NewFileOption.ENTITY_DATA_MODEL, NewFileOption.FORM_DEFINITION);

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        for (NewFileOption newFileOption : REPRESENTATIVE_OPTIONS) {
            workbench.createFileInProject(this.getClass()
                                              .getSimpleName(),
                    newFileOption.getOptionName());

            workbench.openFile(newFileOption.getNewFileName());

            assertFileTabIsOpen(newFileOption);
        }
    }

    private void assertFileTabIsOpen(NewFileOption newFileOption) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag",
                newFileOption.getNewFileName());
    }

}
