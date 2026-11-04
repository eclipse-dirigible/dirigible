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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.codeborne.selenide.Selenide;

/**
 * "Report an issue" in a real browser (#7948): a failed request's id reaches the report dialog of
 * the application shell, the report is filed from it, the reporter follows it on the shell's Issues
 * page, and the instance's operator finds it on the monitoring shell's Issues page.
 * <p>
 * Only a browser catches the failure mode of the shared fragments: a binding that throws inside the
 * dialog mounted with x-html aborts Alpine's walk with no server-side symptom.
 * <p>
 * A distribution may ship without the issues engine (#7991). The branding script states whether it
 * is deployed, and a shell told it is not leaves out the Issues entry and "Report an issue", and
 * its Issues page says so without asking anything of {@code /services/issues}.
 */
class IssueReportShellIT extends UserInterfaceIntegrationTest {

    private static final String APPLICATION_SHELL = "/services/web/application/index.html";

    private static final String MONITORING_SHELL = "/services/web/monitoring/index.html";

    private static final String SUBJECT = "Saving fails with a reference";

    private static final Duration RENDER = Duration.ofSeconds(30);

    private static final String NOT_FOUND = "This issue does not exist, or is not yours.";

    private static final String UNAVAILABLE = "Issue reporting is not available on this instance.";

    /**
     * The framed shell told there is no engine: its store, the two entries, its Issues page and its
     * requests.
     */
    private static final String WITHOUT_ENGINE =
            """
                    const w = document.getElementById('without-issues-engine').contentWindow;
                    const d = w.document;
                    const page = d.getElementById('issues-page-unavailable');
                    if (!w.Alpine || !page || !d.getElementById('shell-nav-issues')) return 'loading';
                    return [w.Alpine.store('issues').available,
                        d.getElementById('shell-nav-issues').closest('li').style.display,
                        d.getElementById('user-menu-report-issue') ? 'menu entry' : 'no menu entry',
                        page.checkVisibility() ? page.querySelector('[x-h-info-page-title]').textContent.trim() : 'page hidden',
                        w.Alpine.store('issues').dialogOpen ? 'dialog open' : 'dialog closed',
                        w.performance.getEntriesByType('resource').filter(e => e.name.indexOf('/services/issues') >= 0).length + ' requests'].join('|');
                    """;

    @Test
    void aFailedRequestIsReportedFromTheUserMenuAndFollowedByReporterAndOperator() {
        ide.openPath(APPLICATION_SHELL);
        $(By.xpath("//span[normalize-space(text())='Dashboard']")).shouldBe(visible, RENDER);

        // a request the shared fetch client sees fail - it records the failure with its request id
        Selenide.executeJavaScript("App.services.api.get('/services/issues/987654321', { baseUrl: '' }).catch(() => {});");
        String requestId = awaitRecordedRequestId();

        browser.clickOnElementById("user-menu-trigger");
        browser.clickOnElementById("user-menu-report-issue");

        $(By.id("issues-report-dialog")).shouldBe(visible, RENDER);
        $(By.id("issues-report-reference")).shouldHave(text(requestId), RENDER);
        $(By.id("issues-report-subject")).setValue(SUBJECT);
        $(By.id("issues-report-bundle-toggle")).click();
        $(By.id("issues-report-bundle")).shouldHave(text(requestId), RENDER);
        $(By.id("issues-report-submit")).click();
        $(By.id("issues-report-dialog")).shouldNotBe(visible, RENDER);

        // the reporter's Issues page lists it, and opens its conversation
        Selenide.executeJavaScript("window.location.hash = '#/issues';");
        $(By.xpath("//*[@id='issues-page']//span[normalize-space(text())='" + SUBJECT + "']")).shouldBe(visible, RENDER)
                                                                                              .click();
        $(By.id("issues-page-detail")).shouldHave(text(SUBJECT), RENDER);

        // the operator of the instance finds it on the support desk
        ide.openPath(MONITORING_SHELL);
        browser.clickOnElementById("monitoring-nav-issues");
        $(By.xpath("//*[@id='monitoring-issues-page']//span[normalize-space(text())='" + SUBJECT + "']")).shouldBe(visible, RENDER)
                                                                                                         .click();
        $(By.id("monitoring-issues-detail")).shouldHave(text(requestId), RENDER);
    }

    @Test
    void aShellWithoutTheIssuesEngineOffersNoIssuesAndSaysSo() {
        ide.openPath(APPLICATION_SHELL);
        $(By.xpath("//span[normalize-space(text())='Dashboard']")).shouldBe(visible, RENDER);

        // This instance runs the engine and its branding script says so, and a missing thread is still a
        // missing thread.
        assertEquals("true|true", Selenide.executeJavaScript("return top.PlatformIssues + '|' + Alpine.store('issues').available;"));
        Selenide.executeJavaScript("Alpine.store('issues').select(987654321);");
        assertEquals(NOT_FOUND, poll("return Alpine.store('issues').error;", NOT_FOUND));

        // The same shell where the branding script states no engine: a frame of it, under a top whose flag
        // reads false whatever the frame's own branding script writes. Its Issues link, the Workbench's
        // ?report=1, opens no dialog, and nothing asks /services/issues.
        Selenide.executeJavaScript(
                "Object.defineProperty(window, 'PlatformIssues', { get: () => false, set: () => {}, configurable: true });"
                        + " const frame = document.createElement('iframe'); frame.id = 'without-issues-engine';"
                        + " frame.style.cssText = 'position: fixed; inset: 0; width: 1280px; height: 800px; z-index: 9999;';"
                        + " frame.src = '" + APPLICATION_SHELL + "#/issues?report=1'; document.body.appendChild(frame);");
        String expected = "false|none|no menu entry|" + UNAVAILABLE + "|dialog closed|0 requests";
        assertEquals(expected, poll(WITHOUT_ENGINE, expected));

        // A 404 on the collection itself - an engine missing although nothing said so - is no missing
        // thread either: the report is answered with the reason, and the entries go.
        Selenide.executeJavaScript("const real = window.fetch;"
                + " window.fetch = (url, options) => String(url) === '/services/issues' ? Promise.resolve(new Response('', { status: 404 })) : real(url, options);"
                + " document.getElementById('without-issues-engine').remove();"
                + " const issues = Alpine.store('issues'); issues.report().then(() => { issues.draft.subject = 'x'; return issues.submit(); });");
        String refused = UNAVAILABLE + "|false|none";
        assertEquals(refused, poll("const issues = Alpine.store('issues'); return issues.error + '|' + issues.available + '|'"
                + " + document.getElementById('shell-nav-issues').closest('li').style.display;", refused));
    }

    private static String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + RENDER.toMillis();
        String value = null;
        while (System.currentTimeMillis() < deadline) {
            value = Selenide.executeJavaScript(script);
            if (expected.equals(value)) {
                return value;
            }
            Selenide.sleep(250);
        }
        return value;
    }

    private static String awaitRecordedRequestId() {
        long deadline = System.currentTimeMillis() + RENDER.toMillis();
        while (System.currentTimeMillis() < deadline) {
            String requestId = Selenide.executeJavaScript(
                    "const items = App.services.recentFailures.items; return items.length ? items[0].requestId : null;");
            if (requestId != null) {
                return requestId;
            }
            Selenide.sleep(250);
        }
        return fail("the failed request was never recorded with its request id");
    }
}
