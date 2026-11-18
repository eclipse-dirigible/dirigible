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
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

/**
 * The Settings list of the platform shells scrolls inside its own panel (#8636): with more entries
 * than the window holds, picking one near the bottom leaves the page where it was, so the opened
 * setting fills the right side from its top. The list used to overflow its panel and scroll the
 * page both columns share, which left the opened setting above the visible area.
 */
class SettingsListScrollIT extends UserInterfaceIntegrationTest {

    private static final String[] SHELLS = {"/services/web/application/index.html", "/services/web/personal/index.html",
            "/services/web/partner/index.html", "/services/web/admin/index.html"};

    private static final Dimension[] WINDOWS = {new Dimension(1280, 600), new Dimension(1920, 1080)};

    /** More entries than either window holds. */
    private static final int ENTRIES = 60;

    private static final Duration RENDER = Duration.ofSeconds(30);

    /**
     * The pane showing a local setting is looked for among the siblings of the settings iframe: they
     * share the detail content area, which is the panel itself in some shells and a wrapper inside it
     * (below the compact back toolbar) in the application shell.
     */
    private static final String LAYOUT = "const app = document.getElementById('app');"
            + "const panels = app.querySelectorAll('[x-h-split-panel]');" + "const list = panels[0].querySelector('ul[x-h-list]');"
            + "const detail = panels[1].getBoundingClientRect();" + "const frame = panels[1].querySelector(\"iframe[title='Settings']\");"
            + "const page = Array.from(frame.parentElement.children).find(c => c.offsetParent !== null && c.tagName !== 'IFRAME');"
            + "return { page: app.scrollTop, list: list.scrollTop, overflows: list.scrollHeight > list.clientHeight,"
            + "  appTop: Math.round(app.getBoundingClientRect().top), frameTop: Math.round(frame.getBoundingClientRect().top),"
            + "  detailBottom: Math.round(detail.bottom), windowBottom: window.innerHeight,"
            + "  localBottom: page ? Math.round(page.getBoundingClientRect().bottom) : -1 };";

    @AfterEach
    void restoreTheWindow() {
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1920, 1080));
    }

    @Test
    void aSettingPickedLowInALongListOpensInView() {
        ide.openHomePage();
        for (Dimension window : WINDOWS) {
            WebDriverRunner.getWebDriver()
                           .manage()
                           .window()
                           .setSize(window);
            for (String shell : SHELLS) {
                assertTheListScrollsOnItsOwn(shell, shell + " at " + window);
            }
        }
    }

    private void assertTheListScrollsOnItsOwn(String shell, String where) {
        ide.openPath(shell + "#/settings");
        $("#app [x-h-split-panel] ul[x-h-list]").shouldBe(visible, RENDER);
        Selenide.executeJavaScript("Alpine.$data(document.body).settingsItems = Array.from({ length: arguments[0] },"
                + " (v, i) => ({ id: 'it-setting-' + i, label: 'IT Setting ' + i, path: 'about:blank' }));", ENTRIES);

        // an entry low in the list: the list scrolls, the page does not, the setting opens from its top
        $(By.xpath("//div[@id='app']//button[normalize-space(.)='IT Setting " + (ENTRIES - 1) + "']")).shouldBe(visible, RENDER)
                                                                                                      .click();
        $("#app iframe[title='Settings']").shouldBe(visible, RENDER);
        Map<String, Object> layout = Selenide.executeJavaScript(LAYOUT);
        assertTrue((Boolean) layout.get("overflows"), where + ": the list holds more entries than its panel shows " + layout);
        assertTrue(number(layout, "list") > 0, where + ": the list scrolled on its own " + layout);
        assertEquals(0, number(layout, "page"), where + ": the page did not scroll " + layout);
        assertTrue(number(layout, "frameTop") >= number(layout, "appTop"), where + ": the opened setting starts in view " + layout);
        assertTrue(number(layout, "detailBottom") <= number(layout, "windowBottom"), where + ": the detail ends in the window " + layout);

        // a locally rendered setting keeps its own scroll inside the bounded detail panel
        $(By.xpath("//div[@id='app']//button[.//*[@data-lucide='globe']]")).click();
        $("#app iframe[title='Settings']").shouldNotBe(visible, RENDER);
        layout = Selenide.executeJavaScript(LAYOUT);
        assertEquals(0, number(layout, "page"), where + ": Region & Language leaves the page where it was " + layout);
        assertTrue(number(layout, "localBottom") > 0 && number(layout, "localBottom") <= number(layout, "detailBottom"),
                where + ": Region & Language stays inside the detail panel " + layout);
    }

    private static int number(Map<String, Object> layout, String key) {
        return ((Number) layout.get(key)).intValue();
    }
}
