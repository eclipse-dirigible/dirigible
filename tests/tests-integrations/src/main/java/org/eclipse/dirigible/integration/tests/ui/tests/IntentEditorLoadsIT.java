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

import org.junit.jupiter.api.Tag;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.GitPerspective;
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
 * leave the services green while the editor is dead). This test clones the
 * {@code dirigiblelabs/sample-intent-model} sample project (the same clone-a-real-repo pattern the
 * {@code SampleProjectRepositoryIT} subclasses use), opens its {@code app.intent}, and asserts:
 * <ul>
 * <li>the file is routed to the Intent Editor (the editor tab appears),</li>
 * <li>the AngularJS {@code intentEditor} module actually bootstrapped (its injector resolves -
 * directly catching the {@code $injector:modulerr} that a missing dependency causes),</li>
 * <li>the Monaco source editor and the live mxGraph diagram render - including the parsed entity's
 * label inside the diagram - so the {@code /parse} round-trip and the mxGraph rendering both work
 * end to end inside the iframe,</li>
 * <li>clicking Generate writes the model files into the workspace project.</li>
 * </ul>
 * The diagram uses fixed brand colours that read on both the light and dark themes (like the
 * EDM/schema modelers), so it renders identically in either theme and needs no theme-switch probe.
 */
@Tag("smoke")
public class IntentEditorLoadsIT extends UserInterfaceIntegrationTest {

    private static final String REPOSITORY_URL = "https://github.com/dirigiblelabs/sample-intent-model.git";
    private static final String PROJECT = "sample-intent-model";
    private static final String INTENT_FILE = "app.intent";
    private static final String GENERATED_EDM_PATH = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + PROJECT + "/library.edm";

    @Autowired
    private IRepository repository;

    @Test
    void intentEditor_opens_bootstraps_and_generates() {
        ide.openHomePage();
        GitPerspective gitPerspective = ide.openGitPerspective();
        gitPerspective.cloneRepository(REPOSITORY_URL);

        Workbench workbench = ide.openWorkbench();
        workbench.openFile(PROJECT, INTENT_FILE);

        // The file routed to the Intent Editor - its tab is present.
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", INTENT_FILE);

        // Switch into the editor iframe; the Monaco source editor is the body, not the error page.
        browser.findElementInAllFrames(By.cssSelector(".intent-monaco .monaco-editor"), Condition.visible);

        // The AngularJS module bootstrapped - a missing platform-links dependency would have thrown
        // $injector:modulerr and left no injector on the ng-app element.
        Object bootstrapped = Selenide.executeJavaScript(
                "var el = document.querySelector('[ng-app=\"intentEditor\"]');" + "return !!(el && angular.element(el).injector());");
        Assertions.assertTrue(Boolean.TRUE.equals(bootstrapped),
                "intentEditor AngularJS module failed to bootstrap inside the editor iframe.");

        // The live mxGraph diagram rendered from the parsed model (proves /parse + mxGraph work
        // in-frame): the entities section produces an mxGraph SVG carrying the parsed entity's label.
        Selenide.$(By.cssSelector(".intent-diagram svg"))
                .shouldBe(Condition.visible);
        Selenide.$(By.xpath("//div[contains(@class, 'intent-diagram')]//*[contains(text(), 'Book')]"))
                .shouldBe(Condition.visible);
        // The "Glue & Outputs" diagram visualizes the declarative glue: the sample's loanUpdated
        // notification appears as an icon card, proving the glue collections are diagrammed too.
        Selenide.$(By.xpath("//div[contains(@class, 'intent-diagram')]//*[contains(text(), 'loanUpdated')]"))
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

        // The generated EDM must render its entity boxes - the canvas was empty before the
        // mxGraphModel layout was added. Switch into the EDM editor's graph frame and assert an
        // entity title is drawn.
        workbench.openFile("library.edm");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag",
                "library.edm");
        browser.findElementInAllFrames(By.id("graphContainer"), Condition.visible);
        Selenide.$(By.xpath("//*[contains(text(), 'Book')]"))
                .shouldBe(Condition.visible);

        // The generated BPMN must render its process nodes - likewise empty before the bpmndi layout.
        workbench.openFile("LoanApproval.bpmn");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag",
                "LoanApproval.bpmn");
        browser.findElementInAllFrames(By.id("canvasSection"), Condition.visible);
        // The canvas renders the task's human-readable name (the userTask `name`), not its id.
        Selenide.$(By.xpath("//*[contains(text(), 'Librarian Review')]"))
                .shouldBe(Condition.visible);
    }
}
