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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * The IDE is not translated (#8793): a language an earlier version's IDE language picker saved in
 * the browser - one the instance still ships catalogs for, or one it never had - no longer keeps
 * the IDE from initializing its texts (#8736). The Workbench opens, i18next initializes in
 * {@code en-US} and the shell's {@code onInit} callbacks, which select the perspective, run.
 */
class IdeStaleLanguageIT extends UserInterfaceIntegrationTest {

    /** The key the removed picker saved the IDE's language under, for this branding and shell. */
    private static final String LANGUAGE_KEY = "return getBrandingInfo().prefix + '.' + top.getConfigData().id + '.locale.language';";

    private static final String INITIALIZED_LANGUAGE = "return top.i18next && top.i18next.isInitialized ? top.i18next.language : null;";

    @Test
    void theWorkbenchOpensWhateverLanguageWasSaved() {
        ide.openIde();
        waitForWorkbench();
        String key = Selenide.executeJavaScript(LANGUAGE_KEY);

        for (String savedLanguage : List.of("bg-BG", "xx-XX")) {
            Selenide.executeJavaScript("localStorage.setItem(arguments[0], arguments[1]);", key, savedLanguage);
            browser.reload();

            waitForWorkbench();
            await().pollInSameThread()
                   .atMost(Duration.ofSeconds(30))
                   .until(() -> Selenide.executeJavaScript(INITIALIZED_LANGUAGE) != null);
            assertEquals("en-US", Selenide.executeJavaScript(INITIALIZED_LANGUAGE), "with '" + savedLanguage + "' saved");
        }
    }

    private static void waitForWorkbench() {
        Selenide.$(By.id("perspective-workbench"))
                .shouldBe(Condition.visible);
    }
}
