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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The complete BPMN visual modeler journey in ONE editor session: boot, property-panel popup
 * lifecycle, and shape rename + save. These used to be three separate IT classes
 * (BpmnEditorLoadsIT, BpmnEditorPropertyPopupIT, BpmnEditorIT), each paying its own Dirigible boot,
 * browser and ~30s editor bring-up to look at the same freshly created {@code .bpmn} file - the
 * three aspects are sequential stations of one user journey, so they are asserted in one pass.
 *
 * <p>
 * Editor boot (former BpmnEditorLoadsIT): the Oryx-based editor loads without errors - the editor
 * tab is present, the canvas and stencil palette render (proving {@code GET
 * /services/bpm/stencil-sets} returned data and AngularJS processed it), the theme CSS variables
 * resolve inside the editor iframe, and the $modal/$popover + Bootstrap-3 jQuery plugin stack that
 * replaced angular-strap after the Angular 1.8.2 migration is complete. Guards against regressions
 * where missing Angular module dependencies silently prevent the editor from bootstrapping.
 *
 * <p>
 * Property-panel popup lifecycle (former BpmnEditorPropertyPopupIT): the wide property popups
 * (execution listeners et al.) open as a BlimpKit {@code <bk-dialog>}
 * ({@code section.fd-dialog--active}), the modal scope exposes the angular-strap-compatible
 * {@code $hide()}, the header close button is a real interactable target, Cancel dismisses the
 * dialog, and the canvas stays interactive afterwards. If {@code modal-service.js} misses any of
 * that, the popup can never be closed - the dialog stays open and the editor grays out.
 *
 * <p>
 * Rename + save (former BpmnEditorIT body): inline rename via double-click on the shape, rename via
 * the property panel, then save through the Flowable toolbar action - asserting the status-bar
 * "saved" message and the final "Published" message propagate across frames.
 */
public class BpmnEditorIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "BpmnEditorIT";
    private static final String BPMN_FILE = "bpmn-new.bpmn";

    @Test
    void bpmnEditor_boots_popups_close_and_rename_save_publishes() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(PROJECT);
        workbench.createFileInProject(PROJECT, "Business Process Model");
        workbench.openFile(BPMN_FILE);

        assertEditorBoots();
        assertPropertyPopupOpensAndCloses();
        assertRenameAndSavePublishes();
    }

    private void assertEditorBoots() {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", BPMN_FILE);

        // Editor loads: canvas and stencil palette are visible
        browser.findElementInAllFrames(By.id("canvasSection"), Condition.visible);
        browser.findElementInAllFrames(By.id("paletteSection"), Condition.visible);

        // Expand "Start Events" group and verify StartNoneEvent item appears
        Selenide.$(By.xpath("//span[contains(.,'Start Events')]"))
                .click();
        browser.findElementInAllFrames(By.id("StartNoneEvent"), Condition.visible);

        // The BPM editor's editor-app/theme/*.css references CSS variables that come from the
        // active theme's *-{auto,light,dark}.css file. Assert the variable resolves to a non-empty
        // value so we catch any regression where the theme stops shipping the legacy variables.
        Selenide.$(By.id("canvasSection"))
                .shouldBe(Condition.visible);
        Object sapBgColor =
                Selenide.executeJavaScript("return getComputedStyle(document.documentElement).getPropertyValue('--background').trim();");
        Assertions.assertTrue(sapBgColor != null && !sapBgColor.toString()
                                                               .isEmpty(),
                "--background is undefined inside the BPM editor iframe — theme variables not applied.");

        // angular-strap was removed during the Angular 1.4.7 -> 1.8.2 migration. Its $modal and
        // $popover services are now provided by scripts/services/{modal,popover}-service.js (each
        // delegating to Bootstrap-3's jQuery .modal/.popover plugin). The property-panel popups —
        // execution-listeners, task-listeners, event-listeners; each opens a modal containing a
        // "Delegate Expression" input — depend on this whole chain. Verify both factories are in
        // the injector AND the underlying Bootstrap-3 jQuery plugins are present, so an accidental
        // drop of any of the four script tags is caught here.
        Object stackOk = Selenide.executeJavaScript("var inj = angular.element(document.body).injector();"
                + "return inj.has('$modal') && typeof inj.get('$modal') === 'function'"
                + "    && inj.has('$popover') && typeof inj.get('$popover') === 'function'" + "    && typeof jQuery.fn.modal === 'function'"
                + "    && typeof jQuery.fn.popover === 'function';");
        Assertions.assertTrue(Boolean.TRUE.equals(stackOk),
                "Modal/popover stack incomplete — $modal/$popover factories or Bootstrap-3 jQuery plugins missing inside the BPM editor iframe.");
    }

    private void assertPropertyPopupOpensAndCloses() {
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

    private void assertRenameAndSavePublishes() {
        // Expand "Activities" group and verify UserTask is initialized as draggable
        Selenide.$(By.xpath("//span[contains(.,'Activities')]"))
                .click();
        Selenide.$(By.id("UserTask"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5))
                .shouldHave(Condition.cssClass("ui-draggable"));

        // Inline rename via double-click on the shape (already selected by the popup station)
        Selenide.$(By.id("svg-my-service-task"))
                .doubleClick();
        Selenide.$(By.id("shapeTextInput"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));
        // Set value and commit using the RenameShapes plugin instance directly.
        //
        // Why not setValue() or sendKeys(): both cause Chrome to focus/scroll the textarea which
        // fires EVENT_CANVAS_SCROLL → Oryx hideField() → destroy(), removing the textarea before
        // we can commit. Dispatching a synthetic mousedown on documentElement is also unreliable.
        //
        // Why not FLOWABLE.eventBus.editor: bootEditor() sets it correctly, but the editorFactory
        // promise callback in editor-controller.js later overwrites it with $rootScope.editor which
        // is never assigned in the Dirigible boot path (no $routeParams.modelId), so it ends up
        // undefined. Use the Angular $injector to reach editorManager directly instead.
        //
        // loadedPlugins is populated by ORYX.Editor.loadPlugins() (100 ms timeout in constructor);
        // find() locates the RenameShapes instance; hide(undefined) passes the guard (!e || …)
        // and calls updateValueFunction() then destroy(), committing the rename.
        // Diagnostic JS: find the plugin, check state, and commit if possible
        String renameInfo = (String) Selenide.executeJavaScript("var r={};" + "r.domTf=!!document.getElementById('shapeTextInput');"
                + "try{" + "  var em=angular.element(document.body).injector().get('editorManager');"
                + "  var ed=em.getEditor(); r.hasEd=!!ed;" + "  r.lpLen=ed.loadedPlugins.length;"
                + "  var p=ed.loadedPlugins.find(function(x){return x.type==='ORYX.Plugins.RenameShapes';});"
                + "  r.hasP=!!p; r.hasTf=!!(p&&p.shownTextField);"
                + "  if(p&&p.shownTextField){p.shownTextField.value='My User Task';p.hide();r.ok=true;}" + "}catch(e){r.err=e.message;}"
                + "return JSON.stringify(r);");
        Assertions.assertTrue(renameInfo != null && renameInfo.contains("\"ok\":true"),
                "Inline rename commit failed — diagnostic: " + renameInfo);
        // Give Oryx's canvas.update() a moment to propagate the renamed tspan to the SVG DOM
        // before findElementInAllFrames starts switching frame contexts.
        Selenide.sleep(500);
        // SVG tspan elements are in the SVG namespace; //tspan matches only no-namespace elements.
        // Use local-name() to be namespace-agnostic.
        browser.findElementInAllFrames(By.xpath("//*[local-name()='tspan' and contains(.,'My User Task')]"));

        // Rename via properties panel — click the Name property row to open its write-mode input,
        // then commit via Angular scope. Selenide's setValue() triggers a Chrome focus/scroll that
        // fires ng-blur before the value is committed; using the scope API is reliable.
        Selenide.$(By.id("svg-my-service-task"))
                .click();
        Selenide.$(By.cssSelector("#propertySection .property-row"))
                .shouldBe(Condition.visible, Duration.ofSeconds(10));
        Selenide.$(By.xpath("//*[@id='propertySection']//span[contains(@class,'title') and starts-with(normalize-space(.),'Name')]"))
                .click();
        // The string-property write template used to render <input class="form-control"> (Bootstrap-3);
        // after the BlimpKit property-panel migration it renders <bk-input> which compiles to
        // <input class="fd-input fd-input--compact">. Match either, so the test stays
        // robust against further class tweaks.
        Selenide.$(By.cssSelector("#propertySection input.fd-input, #propertySection input.form-control"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));
        // Set the property value via the Angular scope, then fire blur to trigger inputBlurred()
        // → updatePropertyInModel() → executeCommands() → canvas.update().
        //
        // Why scope.$apply(): we are outside Angular's digest cycle here; without $apply the
        // property.value change would not propagate to the model before inputBlurred() reads it.
        Selenide.executeJavaScript(
                "var input = document.querySelector('#propertySection input.fd-input, #propertySection input.form-control');"
                        + "if (input) {" + "  var scope = angular.element(input).scope();"
                        + "  scope.$apply(function() { scope.property.value = 'Renamed Task'; });"
                        + "  input.dispatchEvent(new Event('blur', {bubbles: true}));" + "}");
        Selenide.sleep(500);
        browser.findElementInAllFrames(By.xpath("//*[local-name()='tspan' and contains(.,'Renamed Task')]"));

        // Save via Angular injector (editor iframe context). Calling saveModel through the
        // injector is equivalent to clicking the toolbar save button but avoids click/focus
        // ambiguity. We also patch statusBarHub.showMessage so we can assert it was called
        // (the original still fires, propagating the message to the top frame via postMessage).
        //
        // Why not button click: the .success() callback in saveSilently can silently swallow
        // errors (console.error only), and click events in nested iframes sometimes fail to
        // reach Angular without a visible error on the Selenide side.
        //
        // Why patch statusBarHub: it lets us distinguish "save HTTP POST failed (no message)"
        // from "message sent but cross-frame postMessage didn't propagate" — giving a clear
        // assertion failure message in either case.
        Selenide.executeJavaScript("window._bpmSaveMsg = undefined;" + "var _origShow = statusBarHub.showMessage.bind(statusBarHub);"
                + "statusBarHub.showMessage = function(m) { window._bpmSaveMsg = m; _origShow(m); };"
                + "var _inj = angular.element(document.body).injector();" + "FLOWABLE.TOOLBAR.ACTIONS.saveModel({"
                + "  '$rootScope': _inj.get('$rootScope')," + "  '$http':      _inj.get('$http'),"
                + "  'editorManager': _inj.get('editorManager')" + "});");

        // Poll up to 15 s for the async HTTP POST + statusBarHub.showMessage to complete.
        for (int i = 0; i < 30; i++) {
            Selenide.sleep(500);
            if (Boolean.TRUE.equals(Selenide.executeJavaScript("return window._bpmSaveMsg !== undefined;"))) {
                break;
            }
        }
        String bpmSaveMsg = (String) Selenide.executeJavaScript("return window._bpmSaveMsg;");
        Assertions.assertTrue(bpmSaveMsg != null && bpmSaveMsg.contains("saved"),
                "statusBarHub.showMessage was not called with 'saved' — save likely failed; actual message: " + bpmSaveMsg);

        // Status bar is in the shell-ide top frame. statusBarHub.showMessage uses
        // window.top.postMessage, so the message is already en route; a short retry suffices.
        Selenide.switchTo()
                .defaultContent();
        // After save, the projects view receives workspaceHub.announceFileSaved and publishes the
        // file; "Published '...'" is the final status-bar message. Asserting on it proves both
        // cross-frame postMessage propagation and successful publish.
        Selenide.$(By.cssSelector(".statusbar-message .statusbar--text"))
                .shouldHave(Condition.partialText("Published"), Duration.ofSeconds(10));
    }
}
