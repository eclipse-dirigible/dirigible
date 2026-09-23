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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

import org.openqa.selenium.By;

/**
 * Browser test for the Workbench header's side-panel toggles (issue #7492). The shell header lives
 * in the top frame while the Projects (left) and Assistant (right) panes live inside the
 * perspective iframe, and the split library only collapses a pane down to its {@code minSize} - two
 * facts a pure HTTP test cannot exercise. It asserts the observable outcome: clicking a toggle
 * drives the pane's width to zero (reclaiming the editor space) and clicking it again restores it,
 * and that a collapsed pane stays collapsed across a reload (the state persists in the layout's
 * localStorage key).
 */
public class WorkbenchSidePaneToggleIT extends UserInterfaceIntegrationTest {

    private static final String LEFT_TOGGLE_ID = "toggle-left-panel";
    private static final String RIGHT_TOGGLE_ID = "toggle-right-panel";
    private static final int COLLAPSED_MAX_WIDTH = 20;
    private static final int EXPANDED_MIN_WIDTH = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Test
    void togglesCollapseAndRestoreTheSidePanes() {
        ide.openHomePage();
        ide.openWorkbench();

        // Wait for the layout to render both side panes inside the perspective iframe.
        browser.findElementInAllFrames(By.cssSelector(".pane-left"), Condition.visible);
        browser.findElementInAllFrames(By.cssSelector(".pane-right"), Condition.visible);
        Selenide.switchTo()
                .defaultContent();

        Assertions.assertTrue(paneWidth("left") > EXPANDED_MIN_WIDTH, "the left pane should start expanded");
        Assertions.assertTrue(paneWidth("right") > EXPANDED_MIN_WIDTH, "the right pane should start expanded");

        browser.clickOnElementById(LEFT_TOGGLE_ID);
        awaitPaneWidth("left", true);

        browser.clickOnElementById(RIGHT_TOGGLE_ID);
        awaitPaneWidth("right", true);

        browser.clickOnElementById(LEFT_TOGGLE_ID);
        awaitPaneWidth("left", false);

        browser.clickOnElementById(RIGHT_TOGGLE_ID);
        awaitPaneWidth("right", false);
    }

    @Test
    void collapsedStatePersistsAcrossReload() {
        ide.openHomePage();
        ide.openWorkbench();

        browser.findElementInAllFrames(By.cssSelector(".pane-left"), Condition.visible);
        Selenide.switchTo()
                .defaultContent();
        Assertions.assertTrue(paneWidth("left") > EXPANDED_MIN_WIDTH, "the left pane should start expanded");

        browser.clickOnElementById(LEFT_TOGGLE_ID);
        awaitPaneWidth("left", true);

        ide.reload();

        // The perspective reopens from the persisted selection; the left pane must still be collapsed.
        browser.findElementInAllFrames(By.cssSelector(".pane-left"));
        Selenide.switchTo()
                .defaultContent();
        awaitPaneWidth("left", true);
    }

    private void awaitPaneWidth(String side, boolean collapsed) {
        Awaitility.await()
                  .atMost(TIMEOUT)
                  .pollInSameThread() // Selenide binds the WebDriver to the test thread
                  .pollInterval(200, TimeUnit.MILLISECONDS)
                  .until(() -> collapsed ? paneWidth(side) <= COLLAPSED_MAX_WIDTH : paneWidth(side) > EXPANDED_MIN_WIDTH);
    }

    /**
     * Reads the rendered width of the {@code .pane-left} / {@code .pane-right} element from the visible
     * perspective iframe. Runs in the top frame and reaches into the same-origin iframe, so it needs no
     * frame switching and does not disturb the driver's frame context. Returns -1 when the pane is
     * absent.
     */
    private int paneWidth(String side) {
        Object width = Selenide.executeJavaScript("var side = arguments[0];" //
                + "var frames = document.querySelectorAll('iframe');" //
                + "for (var i = 0; i < frames.length; i++) {" //
                + "  var frame = frames[i];" //
                + "  if (frame.getBoundingClientRect().width === 0) continue;" //
                + "  var doc;" //
                + "  try { doc = frame.contentDocument; } catch (e) { continue; }" //
                + "  if (!doc) continue;" //
                + "  var pane = doc.querySelector('.pane-' + side);" //
                + "  if (pane) return Math.round(pane.getBoundingClientRect().width);" //
                + "}" //
                + "return -1;", side);
        return width == null ? -1 : ((Number) width).intValue();
    }
}
