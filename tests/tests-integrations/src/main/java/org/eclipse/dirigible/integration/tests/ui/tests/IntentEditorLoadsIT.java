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

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

import org.openqa.selenium.By;

/**
 * Browser smoke test for the Intent Editor. The HTTP-only {@code IntentEngineIT} exercises the
 * {@code /parse} and {@code /generate} services exhaustively but cannot catch the editor page
 * failing to bootstrap in a browser - which is exactly the class of regression that slips through
 * (a missing {@code ng-editor} platform-links category, or the config script not being loaded, both
 * leave the services green while the editor is dead). This test opens a real {@code .intent} file
 * and asserts:
 * <ul>
 * <li>the file is routed to the Intent Editor (the editor tab appears),</li>
 * <li>the AngularJS {@code intentEditor} module actually bootstrapped (its injector resolves -
 * directly catching the {@code $injector:modulerr} that a missing dependency causes),</li>
 * <li>the source textarea and the live diagram SVG render (so the {@code /parse} round-trip and
 * Mermaid both work end to end inside the iframe),</li>
 * <li>clicking Generate writes the model files into the workspace project.</li>
 * </ul>
 */
public class IntentEditorLoadsIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "IntentEditorIT";
    private static final String INTENT_FILE = "app.intent";
    private static final String GENERATED_EDM_PATH = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + PROJECT + "/library.edm";

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private IRepository repository;

    @Test
    void intentEditor_opens_bootstraps_and_generates() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(PROJECT);

        Workbench workbench = ide.openWorkbench();
        workbench.openFile(PROJECT, INTENT_FILE);

        // The file routed to the Intent Editor - its tab is present.
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", INTENT_FILE);

        // Switch into the editor iframe; the source textarea is the body, not the error message page.
        browser.findElementInAllFrames(By.cssSelector("textarea.intent-text"), Condition.visible);

        // The AngularJS module bootstrapped - a missing platform-links dependency would have thrown
        // $injector:modulerr and left no injector on the ng-app element.
        Object bootstrapped = Selenide.executeJavaScript(
                "var el = document.querySelector('[ng-app=\"intentEditor\"]');" + "return !!(el && angular.element(el).injector());");
        Assertions.assertTrue(Boolean.TRUE.equals(bootstrapped),
                "intentEditor AngularJS module failed to bootstrap inside the editor iframe.");

        // The live diagram rendered from the parsed model (proves /parse + Mermaid work in-frame).
        Selenide.$(By.cssSelector(".intent-diagram-pane svg"))
                .shouldBe(Condition.visible);

        // Generate writes the model files into the workspace project.
        Selenide.$(By.xpath("//button[contains(normalize-space(.), 'Generate')]"))
                .shouldBe(Condition.enabled)
                .click();

        Awaitility.await()
                  .atMost(30, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> repository.getResource(GENERATED_EDM_PATH)
                                         .exists());
    }
}
