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
 * Expression, Delegate expression, Class fields, etc. — angular-strap's {@code $modal} factory
 * used to provide {@code $scope.$hide()} on the modal scope. The {@code text-popup.html} close
 * handler calls {@code $scope.close()} which in turn invokes {@code $scope.$hide()}. After the
 * Angular 1.4.7 → 1.8.2 migration, angular-strap was removed and {@code $modal} is provided by
 * {@code scripts/services/modal-service.js}; that drop-in MUST add {@code $hide()/$show()} to the
 * modal scope, emit {@code modal.show.before/show/hide.before/hide} events up the scope tree,
 * and honour the {@code prefixEvent} option. If any of those is missing the modal can no longer
 * be closed — the backdrop stays, the editor grays out, and the canvas becomes unclickable.
 *
 * <p>
 * The test creates a fresh {@code .bpmn} (template contains {@code MyServiceTask}), selects the
 * service task, opens the "Class" property modal, verifies the Bootstrap-3 backdrop is shown,
 * then dismisses the modal and verifies the backdrop / {@code modal-open} class are gone and the
 * canvas is interactive again.
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
        // editor-app/configuration/properties/execution-listeners-popup.html which is opened by
        // FlowableExecutionListenersCtrl via _internalCreateModal($modal, …)). Clicking its title
        // switches the row to write mode and immediately opens the modal.
        Selenide.$(By.xpath("//*[@id='propertySection']//span[contains(@class,'title') and contains(normalize-space(.),'Execution listeners')]"))
                .click();

        // Modal appeared: Bootstrap-3 modal plugin adds `.in` class to the .modal element and
        // `modal-open` class to the body.
        Selenide.$(By.cssSelector("div.modal.in"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));
        Boolean backdropPresent = Selenide.executeJavaScript("return document.body.classList.contains('modal-open');");
        Assertions.assertTrue(Boolean.TRUE.equals(backdropPresent), "Bootstrap modal-open class missing on <body> — modal did not open.");

        // Modal scope must expose $hide() — without it the close() handler in text-popup.html
        // (which calls $scope.$hide()) silently throws and the modal can never be dismissed.
        Boolean hideExists = Selenide.executeJavaScript("var modalEl = document.querySelector('div.modal.in');"
                + "if (!modalEl) return false;" + "var scope = angular.element(modalEl).scope();" + "return scope && typeof scope.$hide === 'function';");
        Assertions.assertTrue(Boolean.TRUE.equals(hideExists),
                "modal scope.$hide is missing — angular-strap-compatible $hide() helper was not added by modal-service.js.");

        // Dismiss the modal via the Cancel button in the footer — same path the user takes.
        // (The header ✕ button uses Bootstrap-3's `.close` class which BlimpKit's global CSS
        // happens to collapse to zero size; that's a separate visual issue, not part of the
        // backdrop/$hide regression this test guards.)
        Selenide.$(By.xpath("//div[contains(@class,'modal') and contains(@class,'in')]//button[normalize-space(.)='Cancel']"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5))
                .click();

        // Modal must be gone: no `.modal.in`, body no longer has `modal-open`, backdrop is removed.
        Selenide.$(By.cssSelector("div.modal.in"))
                .shouldNotBe(Condition.visible, Duration.ofSeconds(5));
        Boolean backdropCleared = Selenide.executeJavaScript("return !document.body.classList.contains('modal-open')"
                + " && document.querySelectorAll('.modal-backdrop').length === 0;");
        Assertions.assertTrue(Boolean.TRUE.equals(backdropCleared),
                "Bootstrap modal-open / modal-backdrop did not clear — closing the popup left the editor locked.");

        // Canvas is interactive again: re-clicking the service task should re-populate the property
        // panel (proves the iframe still accepts pointer events).
        Selenide.$(By.id("svg-my-service-task"))
                .click();
        Selenide.$(By.cssSelector("#propertySection .property-row"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));
    }
}
