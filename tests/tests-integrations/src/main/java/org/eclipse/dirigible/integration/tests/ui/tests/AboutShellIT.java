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

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Selenide;

/**
 * The About entry of the user menu in a real browser (#8502): which application and which platform
 * the user runs. A deployment that declares its application ({@code DIRIGIBLE_APPLICATION_NAME},
 * {@code DIRIGIBLE_APPLICATION_VERSION}) shows it above the platform; one that does not shows the
 * platform alone.
 * <p>
 * Only a browser catches the failure mode of the shared fragments: a binding that throws inside the
 * dialog mounted with x-html aborts Alpine's walk with no server-side symptom.
 */
class AboutShellIT extends UserInterfaceIntegrationTest {

    private static final String APPLICATION_SHELL = "/services/web/application/index.html";

    private static final String PERSONAL_SHELL = "/services/web/personal/index.html";

    private static final String APPLICATION_NAME = "DIRIGIBLE_APPLICATION_NAME";

    private static final String APPLICATION_VERSION = "DIRIGIBLE_APPLICATION_VERSION";

    private static final Duration RENDER = Duration.ofSeconds(30);

    @AfterEach
    void forgetTheApplication() {
        Configuration.remove(APPLICATION_NAME);
        Configuration.remove(APPLICATION_VERSION);
    }

    @Test
    void anUndeclaredApplicationShowsThePlatformAlone() {
        String platform = platform();

        openAbout(APPLICATION_SHELL);

        $(By.id("about-platform")).shouldHave(exactText(platform), RENDER);
        assertFalse($(By.id("about-application")).exists(), "no application row without a declared application version");
        String copied = Selenide.executeJavaScript("return Alpine.store('about').text;");
        assertTrue(copied.startsWith("Platform: " + platform), copied);
        assertFalse(copied.contains("Application:"), copied);

        $(By.id("about-close")).click();
        $(By.id("about-dialog")).shouldNotBe(visible, RENDER);
    }

    @Test
    void aDeclaredApplicationShowsItsVersionAboveThePlatform() {
        Configuration.set(APPLICATION_NAME, "BusinessIntents");
        Configuration.set(APPLICATION_VERSION, "1.38.0");
        String platform = platform();

        for (String shell : new String[] {APPLICATION_SHELL, PERSONAL_SHELL}) {
            openAbout(shell);

            $(By.id("about-application")).shouldHave(exactText("BusinessIntents 1.38.0"), RENDER);
            $(By.id("about-platform")).shouldHave(exactText(platform), RENDER);
            String copied = Selenide.executeJavaScript("return Alpine.store('about').text;");
            assertTrue(copied.startsWith("Application: BusinessIntents 1.38.0\nPlatform: " + platform), shell + ": " + copied);
        }
    }

    /** The platform line as the dialog prints it: the product's name and version. */
    private static String platform() {
        String name = Configuration.get("DIRIGIBLE_PRODUCT_NAME");
        String version = Configuration.get("DIRIGIBLE_PRODUCT_VERSION");
        assertTrue(version != null && !version.isBlank() && !version.contains("${"),
                "the instance declares its platform version: " + version);
        return ((name == null || name.isBlank() || name.contains("${") ? "Eclipse Dirigible" : name.trim()) + " " + version.trim());
    }

    private void openAbout(String shell) {
        ide.openPath(shell);
        $(By.id("user-menu-trigger")).shouldBe(visible, RENDER);
        assertTrue(awaitMarkup(), shell + " mounts the About dialog");
        browser.clickOnElementById("user-menu-trigger");
        browser.clickOnElementById("user-menu-about");
        $(By.id("about-dialog")).shouldBe(visible, RENDER);
    }

    private static boolean awaitMarkup() {
        long deadline = System.currentTimeMillis() + RENDER.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(
                    Selenide.executeJavaScript("return !!(window.Alpine && Alpine.store('about') && Alpine.store('about').markup);"))) {
                return true;
            }
            Selenide.sleep(250);
        }
        return false;
    }
}
