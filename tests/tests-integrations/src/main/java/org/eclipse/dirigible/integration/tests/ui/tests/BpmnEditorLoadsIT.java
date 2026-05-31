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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Smoke test for the BPMN visual modeler UI.
 *
 * <p>
 * Creates a new {@code .bpmn} file via the IDE Workbench's "New File" wizard, opens it, and
 * verifies that the Oryx-based BPMN editor loads without errors — specifically:
 * <ul>
 * <li>The editor tab is present in the IDE tab bar.</li>
 * <li>The canvas section (where Oryx renders the diagram) is visible inside the editor iframe.</li>
 * <li>At least one stencil item (Start event) is rendered in the palette — confirming that the
 * {@code GET /services/bpm/stencil-sets} API returned data and AngularJS processed it.</li>
 * </ul>
 *
 * <p>
 * This guards against regressions introduced by the {@code bpmn-editor-modernization} branch where
 * missing Angular module dependencies silently prevent the editor from bootstrapping.
 */
public class BpmnEditorLoadsIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT_NAME = "BpmnEditorLoadsIT";
    private static final String BPMN_FILE_NAME = "bpmn-new.bpmn";

    @Test
    void bpmnEditor_opens_and_canvas_with_stencil_palette_renders() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(PROJECT_NAME);
        workbench.createFileInProject(PROJECT_NAME, "Business Process Model");
        workbench.openFile(BPMN_FILE_NAME);

        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag",
                BPMN_FILE_NAME);

        browser.findElementInAllFrames(By.id("canvasSection"), Condition.visible);

        // Stencil palette renders
        browser.findElementInAllFrames(By.id("paletteSection"), Condition.visible);

        // Expand "Start Events" group and verify StartNoneEvent item appears
        Selenide.$(By.xpath("//span[contains(.,'Start Events')]"))
                .click();
        browser.findElementInAllFrames(By.id("StartNoneEvent"), Condition.visible);

        // The BPM editor's editor-app/theme/*.css references --sap* CSS variables that come from the
        // active theme's sap-variables-{light,dark}.css. Assert the variable resolves to a non-empty
        // value so we catch any regression where the theme stops shipping the legacy variables.
        Selenide.$(By.id("canvasSection"))
                .shouldBe(Condition.visible);
        Object sapBgColor = Selenide.executeJavaScript(
                "return getComputedStyle(document.documentElement).getPropertyValue('--sapBackgroundColor').trim();");
        org.junit.jupiter.api.Assertions.assertTrue(sapBgColor != null && !sapBgColor.toString()
                                                                                     .isEmpty(),
                "--sapBackgroundColor is undefined inside the BPM editor iframe — theme variables not applied.");

        // angular-strap was removed during the Angular 1.4.7 -> 1.8.2 migration. Its $modal and
        // $popover services are now provided by scripts/services/{modal,popover}-service.js so the
        // property-panel popups (execution-listeners, task-listeners, event-listeners — each opening
        // a modal that contains a "Delegate Expression" input) still work. Verify both factories
        // are registered in the injector so an accidental script-tag drop is caught here.
        Object servicesOk = Selenide.executeJavaScript("var inj = angular.element(document.body).injector();"
                + "return inj.has('$modal') && inj.has('$popover') && typeof inj.get('$modal') === 'function' && typeof inj.get('$popover') === 'function';");
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(servicesOk),
                "$modal and/or $popover factory missing from the editor's AngularJS injector — modal-service.js / popover-service.js not loaded?");
    }
}
