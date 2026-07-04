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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;

public class Workbench {

    public static final String PROJECTS_VIEW_ID = "pvtree";
    public static final String PROJECT_NAME_INPUT_ID = "pgfi1";
    private static final String PROJECTS_CONTEXT_MENU_NEW_PROJECT = "New Project";
    private static final String CREATE_PROJECT_BUTTON_TEXT = "Create";

    private final Browser browser;
    private final WelcomeViewFactory welcomeViewFactory;
    private final TerminalFactory terminalFactory;
    private final IRepository repository;

    protected Workbench(Browser browser, WelcomeViewFactory welcomeViewFactory, TerminalFactory terminalFactory, IRepository repository) {
        this.browser = browser;
        this.welcomeViewFactory = welcomeViewFactory;
        this.terminalFactory = terminalFactory;
        this.repository = repository;
    }

    public void publishAll(boolean waitForSynchronizationExecution) {
        clickPublishAll();
        // Wait for the DURABLE effect of publishing - every workspace file landing in the registry -
        // not the transient "Published all projects in" toast. On slow CI runners the cross-frame
        // text sweep can outlast the toast, and the sweep's timeout fallback reloads the page,
        // destroying it for good (the recurring DependsOnIT publish flake - same failure mode as the
        // regenerate toast this replaced in EdmView). Checking only that the project COLLECTION
        // exists is not enough: publish copies file by file, so the check passed on half-published
        // projects and the sample ITs then hit "JS source could not be found, consider publishing
        // it". The whole workspace sub-tree must be contained in /registry/public.
        String workspacePath = IRepositoryStructure.PATH_USERS + "/admin/workspace";
        Awaitility.await()
                  .atMost(300, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> {
                      ICollection workspace = repository.getCollection(workspacePath);
                      if (!workspace.exists()) {
                          return false;
                      }
                      List<String> projects = workspace.getCollectionsNames();
                      return projects.stream()
                                     .allMatch(project -> fullyPublished(workspace.getCollection(project),
                                             repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project)));
                  });

        if (waitForSynchronizationExecution) {
            SynchronizationUtil.waitForSynchronizationExecution();
        }
    }

    /**
     * Whether every resource of the workspace sub-tree exists in the published counterpart. Dot-named
     * collections (a cloned project's {@code .git}) are ignored - the synchronizers never read them, so
     * the tests must not depend on when (or whether) they finish copying.
     */
    private static boolean fullyPublished(ICollection source, ICollection target) {
        if (!target.exists()) {
            return false;
        }
        for (String resource : source.getResourcesNames()) {
            if (!target.getResource(resource)
                       .exists()) {
                return false;
            }
        }
        for (String child : source.getCollectionsNames()) {
            if (child.startsWith(".")) {
                continue;
            }
            if (!fullyPublished(source.getCollection(child), target.getCollection(child))) {
                return false;
            }
        }
        return true;
    }

    public void clickPublishAll() {
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Publish all");
    }

    public WelcomeView openWelcomeView() {
        focusOnOpenedFile("Welcome");
        return welcomeViewFactory.create(browser);
    }

    public WelcomeView focusOnOpenedFile(String fileName) {
        browser.clickOnElementContainingText(HtmlElementType.ANCHOR, fileName);
        return welcomeViewFactory.create(browser);
    }

    public FormView getFormView() {
        return new FormView(browser);
    }

    public void createNewProject(String projectName) {
        browser.rightClickOnElementById(PROJECTS_VIEW_ID);

        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.ROLE, "menuitem",
                PROJECTS_CONTEXT_MENU_NEW_PROJECT);

        browser.enterTextInElementById(PROJECT_NAME_INPUT_ID, projectName);

        browser.clickOnElementWithText(HtmlElementType.BUTTON, CREATE_PROJECT_BUTTON_TEXT);
    }

    public void createFileInProject(String projectName, String newFileType) {
        expandProject(projectName);
        browser.rightClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);

        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-menu__title", newFileType);
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
    }

    /**
     * Creates a Java artefact through the project context menu (New -> Java -> {@code leafLabel}),
     * entering {@code name} in the resulting dialog. Use for the base types (Class, Interface, ...) and
     * the strong-interface skeletons (Controller, Job, Listener, WebSocket, Repository). For Package,
     * {@code name} is the dotted package; for the others it is a simple or fully-qualified type name.
     */
    public void createJavaArtifact(String projectName, String leafLabel, String name) {
        createJavaArtifact(projectName, leafLabel, name, null);
    }

    /**
     * Variant for the Repository skeleton, which additionally prompts for the entity type the
     * repository manages. Pass {@code entity} (simple or fully-qualified); {@code null} for the other
     * kinds, which only prompt for a name.
     */
    public void createJavaArtifact(String projectName, String leafLabel, String name, String entity) {
        expandProject(projectName);
        browser.rightClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);

        browser.clickCascadingMenuItem("Java", leafLabel);

        browser.enterTextInElementById("fdti1", name);
        if (entity != null) {
            browser.enterTextInElementById("fdti2", entity);
        }
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
    }

    public void expandProject(String projectName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);
    }

    public void openFile(String projectName, String fileName) {
        expandProject(projectName);
        openFile(fileName);
    }

    public void openFile(String fileName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, fileName);
    }

    public Terminal openTerminal() {
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Terminal");
        return terminalFactory.create(browser);
    }

}
