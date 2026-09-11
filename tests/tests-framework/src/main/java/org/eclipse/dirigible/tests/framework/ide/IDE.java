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

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.eclipse.dirigible.tests.framework.util.SleepUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;

@Lazy
@Component
public class IDE {
    private static final Logger LOGGER = LoggerFactory.getLogger(IDE.class);

    private static final String LOGIN_PAGE_TITLE = "Please sign in";
    private static final String ROOT_PATH = "/";
    private static final String IDE_PATH = "/services/web/shell-ide/";
    private static final String USERNAME_FIELD_ID = "username";
    private static final String PASSWORD_FIELD_ID = "password";
    private static final String SUBMIT_TYPE = "submit";
    private static final String SIGN_IN_BUTTON_TEXT = "Sign in";
    private static final long LOGIN_COMPLETION_TIMEOUT_MILLIS = 30_000;
    private static final long LOGIN_COMPLETION_POLL_MILLIS = 500;

    private final Browser browser;
    private final String username;
    private final String password;
    private final RestAssuredExecutor restAssuredExecutor;
    private final WorkbenchFactory workbenchFactory;
    private final DatabasePerspectiveFactory databasePerspectiveFactory;
    private final GitPerspectiveFactory gitPerspectiveFactory;

    @Autowired
    IDE(Browser browser, RestAssuredExecutor restAssuredExecutor, WorkbenchFactory workbenchFactory,
            DatabasePerspectiveFactory databasePerspectiveFactory, GitPerspectiveFactory gitPerspectiveFactory) {
        this(browser, DirigibleTestTenant.createDefaultTenant()
                                         .getUsername(),
                DirigibleTestTenant.createDefaultTenant()
                                   .getPassword(),
                restAssuredExecutor, workbenchFactory, databasePerspectiveFactory, gitPerspectiveFactory);
    }

    IDE(Browser browser, String username, String password, RestAssuredExecutor restAssuredExecutor, WorkbenchFactory workbenchFactory,
            DatabasePerspectiveFactory databasePerspectiveFactory, GitPerspectiveFactory gitPerspectiveFactory) {
        this.browser = browser;
        this.restAssuredExecutor = restAssuredExecutor;
        this.username = username;
        this.password = password;
        this.workbenchFactory = workbenchFactory;
        this.databasePerspectiveFactory = databasePerspectiveFactory;
        this.gitPerspectiveFactory = gitPerspectiveFactory;
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

    public DatabasePerspective openDatabasePerspective() {
        openIde();

        browser.clickOnElementById("perspective-database");

        return databasePerspectiveFactory.create(browser);
    }

    public void openHomePage() {
        browser.openPath(ROOT_PATH);
        login(false);
    }

    /**
     * Opens the Workbench IDE shell directly. The root path lands on the Home landing page (the
     * DIRIGIBLE_HOME_URL default), which carries no IDE perspectives - IDE flows start here.
     */
    public void openIde() {
        browser.openPath(IDE_PATH);
        login(false);
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
        awaitLoginCompleted();
    }

    /**
     * Waits for the sign-in form to be replaced by the page the login redirects to. Clicking Sign in
     * only SUBMITS the form, so a caller that navigates straight afterwards raced the login: the app
     * URL was requested before the session existed, Spring saved it and answered with the login
     * redirect, and the request it saves never carries the fragment the browser did not send - so a
     * deep link into a generated SPA silently lost its route and the shell rendered its default page
     * instead (issue #7282).
     */
    private void awaitLoginCompleted() {
        long deadline = System.currentTimeMillis() + LOGIN_COMPLETION_TIMEOUT_MILLIS;
        do {
            if (!isLoginPageOpened()) {
                LOGGER.info("Logged in");
                return;
            }
            SleepUtil.sleepMillis(LOGIN_COMPLETION_POLL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        fail("The sign-in page is still open after " + LOGIN_COMPLETION_TIMEOUT_MILLIS / 1000 + "s - the login did not complete");
    }

    private boolean isLoginPageOpened() {
        String pageTitle = browser.getPageTitle();
        return LOGIN_PAGE_TITLE.equals(pageTitle);
    }

    public GitPerspective openGitPerspective() {
        openIde();

        browser.clickOnElementById("perspective-git");

        return gitPerspectiveFactory.create(browser);
    }

    public void createNewBlankProject(String projectName) {
        Workbench workbench = openWorkbench();

        workbench.createNewProject(projectName);

        assertPublishedProjectMessage(projectName);
    }

    public Workbench openWorkbench() {
        openIde();

        browser.clickOnElementById("perspective-workbench");

        return workbenchFactory.create(browser);
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

    public void openInbox() {
        openPath("/services/web/inbox/");
    }

    public void openPath(String path) {
        openPath(path, false);
    }

    public void openPath(String path, boolean forceLogin) {
        browser.openPath(path);
        login(forceLogin);
    }
}
