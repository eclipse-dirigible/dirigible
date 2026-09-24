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
 * Browser test for collapsing the Workbench's Projects (left) and Assistant (right) side panes from
 * the first panel's header chevron (issue #7492). The chevron collapses the pane to a slim rail
 * that reclaims the editor width and stays clickable to expand it again; the collapsed state
 * persists in the layout's localStorage key. This can only be exercised in a browser: the panes
 * live inside the perspective iframe and the collapse is a rendered width change.
 */
public class WorkbenchSidePaneToggleIT extends UserInterfaceIntegrationTest {

    private static final int COLLAPSED_MAX_WIDTH = 60;
    private static final int EXPANDED_MIN_WIDTH = 100;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Test
    void chevronCollapsesAndRestoresTheSidePanes() {
        ide.openHomePage();
        ide.openWorkbench();

        browser.findElementInAllFrames(By.cssSelector(".pane-left .pf-accordion"), Condition.visible);
        Selenide.switchTo()
                .defaultContent();

        Assertions.assertTrue(paneWidth("left") > EXPANDED_MIN_WIDTH, "the left pane should start expanded");
        Assertions.assertTrue(paneWidth("right") > EXPANDED_MIN_WIDTH, "the right pane should start expanded");

        // Collapsing / expanding one side must not move the other side.
        int rightWidth = paneWidth("right");
        clickPaneChevron("left");
        awaitPaneWidth("left", true);
        assertUnchanged("right", rightWidth, "collapsing the left pane moved the right pane");
        clickPaneChevron("left");
        awaitPaneWidth("left", false);
        assertUnchanged("right", rightWidth, "expanding the left pane moved the right pane");

        int leftWidth = paneWidth("left");
        clickPaneChevron("right");
        awaitPaneWidth("right", true);
        assertUnchanged("left", leftWidth, "collapsing the right pane moved the left pane");
        clickPaneChevron("right");
        awaitPaneWidth("right", false);
        assertUnchanged("left", leftWidth, "expanding the right pane moved the left pane");
    }

    private void assertUnchanged(String side, int expected, String message) {
        Assertions.assertTrue(Math.abs(paneWidth(side) - expected) <= 3, message);
    }

    @Test
    void collapsedStatePersistsAcrossReload() {
        ide.openHomePage();
        ide.openWorkbench();

        browser.findElementInAllFrames(By.cssSelector(".pane-left .pf-accordion"), Condition.visible);
        Selenide.switchTo()
                .defaultContent();
        Assertions.assertTrue(paneWidth("left") > EXPANDED_MIN_WIDTH, "the left pane should start expanded");

        clickPaneChevron("left");
        awaitPaneWidth("left", true);

        ide.reload();

        // The perspective reopens from the persisted selection; the left pane must still be collapsed.
        browser.findElementInAllFrames(By.cssSelector(".pane-left .pf-accordion"));
        Selenide.switchTo()
                .defaultContent();
        awaitPaneWidth("left", true);
    }

    private void clickPaneChevron(String side) {
        browser.findElementInAllFrames(By.cssSelector(".pane-" + side + " .pf-pane-collapse"), Condition.visible)
               .click();
        Selenide.switchTo()
                .defaultContent();
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
