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

import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.chromium.HasCdp;

import com.codeborne.selenide.WebDriverRunner;

/**
 * Records the URL of every request a page makes with {@code fetch} or {@code XMLHttpRequest}, in
 * {@code window.__fetched} of each document (frames included) the tab loads after
 * {@link #record()}.
 *
 * <p>
 * An assertion that a page does NOT request something must read this list, not
 * {@code performance.getEntriesByType('resource')}: Chrome adds a {@code fetch} to the resource
 * timeline only once its response body has been read. A request whose caller looks at the status
 * alone - the shape of a permission probe, {@code (await fetch(url)).status !== 403} - never shows
 * up there, so an assertion that it was not made cannot fail (#8744).
 */
final class RecordedRequests {

    /**
     * The JavaScript expression for the recorded URLs of the current document. It throws where
     * {@link #record()} was not called, so an assertion on it cannot pass for want of a recorder.
     */
    static final String RECORDED = "window.__fetched";

    private static final String RECORDER = """
            if (!window.__fetched) {
              window.__fetched = [];
              const fetched = window.fetch;
              window.fetch = function (input) {
                window.__fetched.push(String(input && input.url ? input.url : input));
                return fetched.apply(this, arguments);
              };
              const open = XMLHttpRequest.prototype.open;
              XMLHttpRequest.prototype.open = function (method, url) {
                window.__fetched.push(String(url));
                return open.apply(this, arguments);
              };
            }""";

    private RecordedRequests() {}

    /**
     * Installs the recorder in the current browser tab: it runs before any page script in every
     * document loaded from now on. Calling it again is harmless.
     */
    static void record() {
        WebDriver driver = WebDriverRunner.getWebDriver();
        while (driver instanceof WrapsDriver wrapper) {
            driver = wrapper.getWrappedDriver();
        }
        ((HasCdp) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", RECORDER));
    }
}
