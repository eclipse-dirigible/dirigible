/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@Lazy
@Component
public class IDE {
    private static final Logger LOGGER = LoggerFactory.getLogger(IDE.class);

    private static final String LOGIN_PAGE_TITLE = "Please sign in";
    private static final String ROOT_PATH = "/";
    private static final String USERNAME_FIELD_ID = "username";
    private static final String PASSWORD_FIELD_ID = "password";
    private static final String SUBMIT_TYPE = "submit";
    private static final String SIGN_IN_BUTTON_TEXT = "Sign in";
    private final Browser browser;
    private final RestAssuredExecutor restAssuredExecutor;
    private final String username;
    private final String password;
    private final ProjectUtil projectUtil;
    private final WorkbenchFactory workbenchFactory;
    private final DatabaseFactory databaseFactory;

    @Autowired
    IDE(Browser browser, RestAssuredExecutor restAssuredExecutor, ProjectUtil projectUtil, WorkbenchFactory workbenchFactory, DatabaseFactory databaseFactory) {
        this(browser, DirigibleTestTenant.createDefaultTenant()
                                         .getUsername(),
                DirigibleTestTenant.createDefaultTenant()
                                   .getPassword(),
                restAssuredExecutor, projectUtil, workbenchFactory, databaseFactory);
    }

    IDE(Browser browser, String username, String password, RestAssuredExecutor restAssuredExecutor, ProjectUtil projectUtil,
            WorkbenchFactory workbenchFactory, DatabaseFactory databaseFactory) {
        this.browser = browser;
        this.restAssuredExecutor = restAssuredExecutor;
        this.username = username;
        this.password = password;
        this.projectUtil = projectUtil;
        this.workbenchFactory = workbenchFactory;
        this.databaseFactory = databaseFactory;

    }

    public Browser getBrowser() {
        return browser;
    }

    // Note: this method is used in Kronos
    public void assertJSHttpResponse(String projectName, String fileRelativePath, int expectedStatusCode, String expectedBody) {
        String path = "/services/js/" + projectName + "/" + fileRelativePath;
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(path)
                             .then()
                             .statusCode(expectedStatusCode)
                             .body(containsString(expectedBody)),
                username, password);
    }

    public void assertPublishedAllProjectsMessage() {
        assertStatusBarMessage("Published all projects in 'workspace'");
    }

    public void assertStatusBarMessage(String expectedMessage) {
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, expectedMessage);
    }

    public void openPath(String path) {
        openPath(path, false);
    }

    public void openPath(String path, boolean forceLogin) {
        browser.openPath(path);
        login(forceLogin);
    }

    public void login(boolean forceLogin) {
        if (!forceLogin && !isLoginPageOpened()) {
            LOGGER.info("Already logged in");
            return;
        }
        LOGGER.info("Logging...");
        browser.enterTextInElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.ID, USERNAME_FIELD_ID, username);
        browser.enterTextInElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.ID, PASSWORD_FIELD_ID, password);
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.TYPE, SUBMIT_TYPE, SIGN_IN_BUTTON_TEXT);
    }

    private boolean isLoginPageOpened() {
        String pageTitle = browser.getPageTitle();
        return LOGIN_PAGE_TITLE.equals(pageTitle);
    }

    public void createAndPublishProjectFromResources(String resourcesFolderPath) {
        createAndPublishProjectFromResources(resourcesFolderPath, true);
    }

    public void createAndPublishProjectFromResources(String resourcesFolderPath, boolean waitForSynchronizationExecution) {
        projectUtil.copyResourceProjectToUserWorkspace(username, resourcesFolderPath, Collections.emptyMap());

        Workbench workbench = openWorkbench();
        workbench.publishAll(waitForSynchronizationExecution);
    }

    public Workbench openWorkbench() {
        openHomePage();

        browser.clickOnElementById("perspective-workbench");

        return workbenchFactory.create(browser);
    }

    public Database openDatabase() {
        openHomePage();

        browser.clickOnElementById("perspective-database");

        return databaseFactory.create(browser);
    }

    public void openHomePage() {
        browser.openPath(ROOT_PATH);
        login(false);
    }

    public void createNewBlankProject(String projectName) {
        Workbench workbench = openWorkbench();

        workbench.createNewProject(projectName);

        assertPublishedProjectMessage(projectName);
    }

    public void assertPublishedProjectMessage(String projectName) {
        String publishedMessage = "Published '/workspace/" + projectName + "'";
        assertStatusBarMessage(publishedMessage);
    }

    public void openSpringBootAdmin() {
        browser.openPath("/spring-admin");
        login();
    }

    public void login() {
        login(true);
    }

    public void reload() {
        browser.reload();
    }

    public void close() {
        browser.clearCookies();
        browser.close();
    }
}
