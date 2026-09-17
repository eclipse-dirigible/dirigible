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

import static com.codeborne.selenide.Condition.visible;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

/**
 * A generated shell page of an EARLIER vintage keeps working against the CURRENT shared runtime.
 * <p>
 * The shell runtime ({@code application-core/shell/js/components/layout/appShell.js}) is served
 * once, by absolute URL, to every generated {@code gen/<model>/index.html} - and that page is a
 * file in the project, rewritten only by an explicit Generate. So a platform upgrade swaps the
 * runtime under every page generated before it, and the names the page binds (its Alpine
 * expressions, its {@code x-ref}s) are a contract across vintages. #7400 renamed them together with
 * the template and every deployed application's embedded iframe showed its own sidebar (#7427); no
 * test caught it because every other Harmonia IT REGENERATES the page and so only ever sees the
 * current page with the current runtime.
 * <p>
 * The fixture is the pre-#7400 template rendered by the real pipeline for the
 * {@code DependsOnHarmoniaIT} model - not hand-written, so it binds exactly what a deployed page of
 * that vintage binds. The project is generated with the current template, then that page is put in
 * the registry over the generated one, the way a deployed project keeps its page through an
 * upgrade. One method on purpose: the base class discards the application context after each.
 */
class ShellRuntimeVintageIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "DependsOnHarmoniaIT";
    private static final String PAGE = "/services/web/" + PROJECT + "/gen/edm/index.html";
    private static final String OLD_PAGE_FIXTURE = "ShellRuntimeVintageIT/index.html";
    private static final By SIDEBAR = By.cssSelector("[data-slot=sidebar]");

    @Autowired
    private ProjectDeployer deployer;

    @Autowired
    private IRepository repository;

    @Test
    void anOlderPageRendersAgainstTheCurrentRuntime() throws IOException {
        deployer.deployGeneratedFromModel(PROJECT, "edm.model");
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/gen/edm/index.html", oldPage());

        // Log in on the page itself, then navigate with the plain opener: the login redirect keeps the
        // query but drops the fragment, and the route is in the fragment.
        ide.openPath(PAGE);

        // Hosted in the application shell: the page draws no navigation of its own, the host owns it.
        browser.openPath(PAGE + "?embedded=true#/Orders");
        browser.assertElementExistsByTypeAndText(HtmlElementType.BUTTON, "New");
        Selenide.$(SIDEBAR)
                .shouldNotBe(visible);

        // Standalone on a wide screen: the sidebar is the navigation.
        browser.openPath(PAGE + "#/Orders");
        browser.assertElementExistsByTypeAndText(HtmlElementType.BUTTON, "New");
        Selenide.$(SIDEBAR)
                .shouldBe(visible);

        // Standalone below the breakpoint: the sidebar lives in the drawer the hamburger opens.
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(800, 900));
        Selenide.$(SIDEBAR)
                .shouldNotBe(visible);
        Selenide.$(By.cssSelector("button[aria-label=Navigation]"))
                .shouldBe(visible)
                .click();
        Selenide.$(SIDEBAR)
                .shouldBe(visible);
    }

    private static byte[] oldPage() throws IOException {
        try (InputStream page = ShellRuntimeVintageIT.class.getClassLoader()
                                                           .getResourceAsStream(OLD_PAGE_FIXTURE)) {
            if (page == null) {
                throw new IllegalStateException("Missing fixture [" + OLD_PAGE_FIXTURE + "]");
            }
            return page.readAllBytes();
        }
    }
}
