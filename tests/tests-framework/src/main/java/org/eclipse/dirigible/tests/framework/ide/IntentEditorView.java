/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.openqa.selenium.By;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * The Intent Editor page object - {@link EdmView}'s sibling for {@code *.intent} files: opens the
 * file from the Workbench tree (the editor derives its coordinates from the tab's file path, so
 * there is no direct-URL shortcut) and clicks its Generate button, which derives the model files
 * AND runs the code recipes in one call - exactly the client's journey in the browser IDE.
 */
@Lazy
@Component
public class IntentEditorView {

    /**
     * The Generate button's title - unambiguous across frames, unlike the visible text "Generate" which
     * the EDM modeler's "Regenerate" would also match in the cross-frame sweep.
     */
    private static final String GENERATE_BUTTON_TITLE = "Generate the model files into the project";

    /** The shell's modal busy message shown while the generation request is in flight. */
    private static final String GENERATING_BUSY_MESSAGE = "Generating model files and code";

    private final Browser browser;
    private final WorkbenchFactory workbenchFactory;
    private final IRepository repository;

    IntentEditorView(Browser browser, WorkbenchFactory workbenchFactory, IRepository repository) {
        this.browser = browser;
        this.workbenchFactory = workbenchFactory;
        this.repository = repository;
    }

    /**
     * Open the intent file in the Intent Editor and run its Generate.
     *
     * @param projectName the workspace project
     * @param intentFileName the intent file at the project root (conventionally {@code app.intent})
     */
    public void generate(String projectName, String intentFileName) {
        Workbench workbench = workbenchFactory.create(browser);
        workbench.openFile(projectName, intentFileName);

        // The frame sweep enters the editor's iframe; the Monaco source pane visible = editor loaded.
        browser.findElementInAllFrames(By.cssSelector(".intent-monaco .monaco-editor"), Condition.visible);
        // The button is ng-disabled while the initial parse runs; the click helper waits it out.
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, GENERATE_BUTTON_TITLE);

        // Wait for the DURABLE effect of the generation - the code recipes' output landing in the
        // project's gen folder - not the transient status toast (the EdmView doctrine: on slow CI
        // runners the cross-frame text sweep can outlast a toast, and its timeout fallback reloads
        // the page).
        String genPath = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + projectName + "/gen";
        Awaitility.await()
                  .atMost(120, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> repository.hasCollection(genPath));

        // The files exist server-side BEFORE the HTTP response that closes the shell's modal busy
        // dialog, so wait for the overlay to be gone - a caller's next click (e.g. the Workbench's
        // "Publish all") would otherwise be intercepted by it. Selenide's own wait polls on the
        // calling thread - the WebDriver is thread-bound, so an Awaitility poll thread cannot touch
        // it (which is also why the gen-folder wait above goes through the repository).
        Selenide.switchTo()
                .defaultContent();
        By busyMessage = By.xpath("//*[contains(text(), '" + GENERATING_BUSY_MESSAGE + "')]");
        Selenide.$(busyMessage)
                .should(Condition.disappear, Duration.ofSeconds(60));
    }
}
