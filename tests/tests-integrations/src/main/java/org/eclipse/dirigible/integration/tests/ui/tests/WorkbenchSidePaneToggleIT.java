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
 * Browser test for opening / closing the Workbench's Projects (left) pane from the Workbench
 * activity-bar button (issue #7492). Re-clicking the already active Workbench button collapses the
 * Projects pane to zero width (reclaiming the editor width) and re-clicking it again restores the
 * pane; the Assistant (right) pane is unaffected. The behaviour is opt-in per perspective, so a
 * perspective that did not opt in (the Database perspective) does not react to a re-click. This can
 * only be exercised in a browser: the panes live inside the perspective iframe and the toggle is a
 * rendered width change driven from the shell frame over the message hub.
 */
public class WorkbenchSidePaneToggleIT extends UserInterfaceIntegrationTest {

    private static final String WORKBENCH_BUTTON_ID = "perspective-workbench";
    private static final String DATABASE_BUTTON_ID = "perspective-database";
    private static final int COLLAPSED_MAX_WIDTH = 5;
    private static final int EXPANDED_MIN_WIDTH = 100;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Test
    void workbenchButtonOpensAndClosesTheProjectsPane() {
        ide.openHomePage();
        ide.openWorkbench();

        browser.findElementInAllFrames(By.cssSelector(".pane-left .pf-accordion"), Condition.visible);
        Selenide.switchTo()
                .defaultContent();

        Assertions.assertTrue(paneWidth("left") > EXPANDED_MIN_WIDTH, "the Projects pane should start open");
        int rightWidth = paneWidth("right");

        clickPerspectiveButton(WORKBENCH_BUTTON_ID);
        awaitPaneWidth("left", true);
        assertUnchanged("right", rightWidth, "toggling the Projects pane moved the Assistant pane");

        clickPerspectiveButton(WORKBENCH_BUTTON_ID);
        awaitPaneWidth("left", false);
        assertUnchanged("right", rightWidth, "toggling the Projects pane moved the Assistant pane");
    }

    @Test
    void nonOptedInPerspectiveDoesNotToggleItsLeftPane() {
        ide.openHomePage();
        ide.openDatabasePerspective();

        browser.findElementInAllFrames(By.cssSelector(".pane-left"), Condition.visible);
        Selenide.switchTo()
                .defaultContent();

        int leftWidth = paneWidth("left");
        Assertions.assertTrue(leftWidth > EXPANDED_MIN_WIDTH, "the Database left pane should start open");

        // Re-clicking the active Database button must not collapse its left pane - only the Workbench
        // opted in. Give the (no-op) hub round-trip a moment before asserting the width is unchanged.
        clickPerspectiveButton(DATABASE_BUTTON_ID);
        Selenide.sleep(1000);
        assertUnchanged("left", leftWidth, "re-clicking a non-opted-in perspective toggled its left pane");
    }

    private void clickPerspectiveButton(String buttonId) {
        Selenide.switchTo()
                .defaultContent();
        browser.clickOnElementById(buttonId);
    }

    private void assertUnchanged(String side, int expected, String message) {
        Assertions.assertTrue(Math.abs(paneWidth(side) - expected) <= 3, message);
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
