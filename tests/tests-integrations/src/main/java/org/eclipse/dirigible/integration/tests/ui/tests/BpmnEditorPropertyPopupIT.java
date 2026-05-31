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
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Verifies the property-panel popup lifecycle inside the BPMN editor.
 *
 * <p>
 * When the user selects a shape and clicks one of the property rows that opens a modal — Class,
 * Expression, Delegate expression, Class fields, Execution listeners, ... — the popup's close
 * handler ultimately calls {@code $scope.$hide()} on the modal scope. After the Angular 1.4.7 →
 * 1.8.2 migration angular-strap was removed, so {@code $modal} is now provided by
 * {@code scripts/services/modal-service.js}; that drop-in MUST add {@code $hide()/$show()} to the
 * modal scope, emit {@code modal.show.before/show/hide.before/hide} events up the scope tree, and
 * honour the {@code prefixEvent} option. If any of those is missing the popup can no longer be
 * closed — the dialog stays open, the editor grays out, and the canvas becomes unclickable.
 *
 * <p>
 * The {@code execution-listeners-popup.html} template was migrated to BlimpKit's
 * {@code <bk-dialog>} as part of the BlimpKit visual alignment, so the expected DOM is now
 * {@code section.fd-dialog.fd-dialog--active} (Fundamental Styles) rather than Bootstrap-3's
 * {@code div.modal.in}. The close ✕ in the header is a
 * {@code <bk-button glyph="sap-icon--decline">}, rendered as a {@code <button>} carrying
 * {@code .sap-icon--decline}.
 */
public class BpmnEditorPropertyPopupIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT_NAME = "BpmnEditorPropertyPopupIT";
    private static final String BPMN_FILE_NAME = "bpmn-new.bpmn";

    @Test
    void executionListenersPopup_opens_and_closes_without_locking_the_editor() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(PROJECT_NAME);
        workbench.createFileInProject(PROJECT_NAME, "Business Process Model");
        workbench.openFile(BPMN_FILE_NAME);

        // Wait for the BPM editor iframe to load + canvas to render.
        browser.findElementInAllFrames(By.id("canvasSection"), Condition.visible);
        browser.findElementInAllFrames(By.id("paletteSection"), Condition.visible);

        // Select the service task that ships in the template — selecting populates the property panel.
        Selenide.$(By.id("svg-my-service-task"))
                .click();
        Selenide.$(By.cssSelector("#propertySection .property-row"))
                .shouldBe(Condition.visible, Duration.ofSeconds(10));

        // The "Execution listeners" property row is one of the wide popups (it lives inside
        // editor-app/configuration/properties/execution-listeners-popup.html, opened by
        // FlowableExecutionListenersCtrl via _internalCreateModal($modal, …)). Clicking its title
        // switches the row to write mode and immediately opens the dialog.
        Selenide.$(By.xpath(
                "//*[@id='propertySection']//span[contains(@class,'title') and contains(normalize-space(.),'Execution listeners')]"))
                .click();

        // Dialog appeared: BlimpKit's bk-dialog renders <section class="fd-dialog"> and adds
        // .fd-dialog--active while it's visible.
        Selenide.$(By.cssSelector("section.fd-dialog.fd-dialog--active"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));

        // Modal scope must expose $hide() — without it the close()/cancel() handlers in
        // properties-execution-listeners-controller.js (both call $scope.$hide()) silently throw
        // and the dialog can never be dismissed. The dialog itself has an isolate scope (bk-dialog
        // directive); the controller's scope sits on a child element with ng-controller, so we
        // look up scope via the close button which is inside the controller's subtree.
        Boolean hideExists = Selenide.executeJavaScript("var btn = document.querySelector("
                + "  'section.fd-dialog.fd-dialog--active .fd-dialog__header button.fd-button');" + "if (!btn) return false;"
                + "var scope = angular.element(btn).scope();" + "while (scope && typeof scope.$hide !== 'function') scope = scope.$parent;"
                + "return scope && typeof scope.$hide === 'function';");
        Assertions.assertTrue(Boolean.TRUE.equals(hideExists),
                "modal scope.$hide is missing — angular-strap-compatible $hide() helper was not added by modal-service.js.");

        // The header ✕ button must be a real, non-zero-sized, interactable target. <bk-button>
        // renders the icon as a child <i class="sap-icon sap-icon--decline">; the button itself
        // is a native <button class="fd-button …"> carrying the click handler.
        Object closeBtnSize = Selenide.executeJavaScript("var b = document.querySelector("
                + "  'section.fd-dialog.fd-dialog--active .fd-dialog__header button.fd-button');" + "if (!b) return null;"
                + "var r = b.getBoundingClientRect();" + "return JSON.stringify({ w: Math.round(r.width), h: Math.round(r.height) });");
        Assertions.assertNotNull(closeBtnSize, "dialog header close (×) button is missing from the DOM.");
        Assertions.assertFalse(closeBtnSize.toString()
                                           .contains("\"w\":0")
                || closeBtnSize.toString()
                               .contains("\"h\":0"),
                "dialog header close (×) button has zero size: " + closeBtnSize);

        // Dismiss the dialog via the Cancel button in the footer — same path the user takes.
        // bk-button[label="Cancel"] compiles to a <button class="fd-button ...">Cancel</button>
        // wrapped in <div class="fd-bar__element">.
        Selenide.$(By.xpath("//section[contains(@class,'fd-dialog--active')]" + "//footer//button[normalize-space(.)='Cancel']"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5))
                .click();

        // Dialog must be gone: no active fd-dialog left in the DOM. modal-service removes the
        // compiled element ~300ms after flipping `modal.visible` to false so the fd-dialog--active
        // class is gone first, then the element itself unmounts.
        Selenide.$(By.cssSelector("section.fd-dialog.fd-dialog--active"))
                .shouldNotBe(Condition.visible, Duration.ofSeconds(5));

        // Canvas is interactive again: re-clicking the service task should re-populate the property
        // panel (proves the iframe still accepts pointer events).
        Selenide.$(By.id("svg-my-service-task"))
                .click();
        Selenide.$(By.cssSelector("#propertySection .property-row"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));
    }
}
