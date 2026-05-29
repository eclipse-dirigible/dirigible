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

public class BpmnEditorIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "BpmnEditorIT";
    private static final String BPMN_FILE = "bpmn-new.bpmn";

    @Test
    void bpmnEditor_select_rename_and_save() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(PROJECT);
        workbench.createFileInProject(PROJECT, "Business Process Model");
        workbench.openFile(BPMN_FILE);

        // Editor loads: canvas and stencil palette are visible
        browser.findElementInAllFrames(By.id("canvasSection"), Condition.visible);
        browser.findElementInAllFrames(By.id("paletteSection"), Condition.visible);

        // Expand "Activities" group and verify UserTask is initialized as draggable
        Selenide.$(By.xpath("//span[contains(.,'Activities')]"))
                .click();
        Selenide.$(By.id("UserTask"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5))
                .shouldHave(Condition.cssClass("ui-draggable"));

        // Click on the existing service task shape (from the BPMN template) to select it
        Selenide.$(By.id("svg-my-service-task"))
                .click();
        Selenide.$(By.cssSelector("#propertySection .property-row"))
                .shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Inline rename via double-click on the shape
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
        Selenide.$(By.cssSelector("#propertySection input.form-control"))
                .shouldBe(Condition.visible, Duration.ofSeconds(5));
        // Set the property value via the Angular scope, then fire blur to trigger inputBlurred()
        // → updatePropertyInModel() → executeCommands() → canvas.update().
        //
        // Why scope.$apply(): we are outside Angular's digest cycle here; without $apply the
        // property.value change would not propagate to the model before inputBlurred() reads it.
        Selenide.executeJavaScript("var input = document.querySelector('#propertySection input.form-control');" + "if (input) {"
                + "  var scope = angular.element(input).scope();" + "  scope.$apply(function() { scope.property.value = 'Renamed Task'; });"
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
