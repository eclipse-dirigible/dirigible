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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.deque.html.axecore.results.Check;
import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.deque.html.axecore.selenium.AxeReporter;

/**
 * The pages the Harmonia template generates pass an axe-core scan against WCAG 2.1 A/AA (#7645).
 * The generated markup carries ARIA where the component library put it, but nothing checked it, so
 * any page was one template edit away from a regression no one sees.
 *
 * <p>
 * An intent application is generated and published with a record in every page kind; axe-core then
 * scans the manage list, the manage form, the document page, its read-only preview and the Inbox.
 * Every page's full result is written under {@code target/failsafe-reports/axe/} - the folder the
 * IT jobs upload whatever the outcome - with a summary of the counts per impact, and a
 * {@code serious} or {@code critical} violation fails the test naming the page, the rule and the
 * elements. {@code minor} and {@code moderate} findings are reported, not enforced.
 */
class AccessibilityHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "accessibility-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/accessibility/api";
    private static final String APP = "/services/web/" + PROJECT + "/gen/accessibility/index.html#";

    /** The WCAG 2.1 level AA conformance rules, and the level A ones it includes. */
    private static final List<String> WCAG_21_AA = List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");

    /** The impacts that fail the test; the others are reported only. */
    private static final Set<String> ENFORCED_IMPACTS = Set.of("serious", "critical");

    /**
     * The one finding not enforced: contrast against Harmonia's own primary colour. In Harmonia 3.1.2's
     * default palette white on {@code --primary} is 3.67:1 in light mode and 4.43:1 in dark mode, and
     * {@code --primary} as link text is 3.67:1 on the light and 4.26:1 on the dark background - and in
     * dark mode no single token can meet 4.5:1 for both uses. It is fixed in codbex/harmonia, not per
     * template. Only a node whose foreground or background IS that colour is exempt; every other
     * contrast failure - a muted text the templates put on a tinted surface - is enforced.
     */
    private static final String CONTRAST_RULE = "color-contrast";

    /** Resolves {@code var(--primary)} to the {@code #rrggbb} axe reports colours in. */
    private static final String PRIMARY_COLOR = """
            const probe = document.createElement('div');
            probe.style.color = 'var(--primary)';
            document.body.appendChild(probe);
            const color = getComputedStyle(probe).color;
            probe.remove();
            const context = document.createElement('canvas').getContext('2d');
            context.fillStyle = color;
            context.fillRect(0, 0, 1, 1);
            const [r, g, b] = context.getImageData(0, 0, 1, 1).data;
            return '#' + [r, g, b].map(v => v.toString(16).padStart(2, '0')).join('');
            """;

    /** Every page is scanned in both colour schemes the theme menu offers. */
    private static final List<String> COLOR_SCHEMES = List.of("light", "dark");

    private static final Path REPORTS = Path.of("target", "failsafe-reports", "axe");

    private static final Duration RENDER_TIMEOUT = Duration.ofSeconds(60);

    private static final long POLL_INTERVAL_MILLIS = 250;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void theGeneratedPagesHaveNoSeriousOrCriticalViolations() throws IOException {
        generateAndPublish();
        int customer = create("/customer/CustomerController", "{\"Name\":\"Acme\",\"Email\":\"office@acme.example\"}");
        int invoice = create("/invoice/InvoiceController", "{\"Number\":\"INV-1\",\"Note\":\"First\",\"Customer\":" + customer + "}");
        create("/invoice/InvoiceItemController", "{\"Product\":\"Widget\",\"Quantity\":2,\"Invoice\":" + invoice + "}");

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("list", "/Customer");
        pages.put("form", "/Customer/create");
        pages.put("document", "/Invoice/" + invoice + "/edit");
        pages.put("preview", "/Invoice/" + invoice + "/preview");
        pages.put("inbox", "/inbox");
        Map<String, String> readyWhen = Map.of( //
                "list", "table tbody tr", //
                "form", "#f_Name", //
                "document", "#f_Note", //
                "preview", "#f_Note", //
                "inbox", "[x-h-split-panel]");

        Files.createDirectories(REPORTS);
        List<String> enforced = new ArrayList<>();
        Map<String, Map<String, Integer>> countsByPage = new LinkedHashMap<>();
        for (String scheme : COLOR_SCHEMES) {
            for (Map.Entry<String, String> page : pages.entrySet()) {
                String name = page.getKey() + "-" + scheme;
                browser.openPath(APP + page.getValue());
                Selenide.$(By.cssSelector(readyWhen.get(page.getKey())))
                        .shouldBe(Condition.visible, RENDER_TIMEOUT);
                // Pinned: the default follows the OS, which differs between a developer's machine and CI.
                Selenide.executeJavaScript("Harmonia.setColorScheme(arguments[0])", scheme);
                awaitTrue("return document.documentElement.classList.contains('dark') === (arguments[0] === 'dark')", scheme);
                // The switch animates the colours; a scan mid-transition measures a colour neither scheme has.
                awaitTrue("return document.getAnimations().every(animation => animation.playState !== 'running');");
                Results results = new AxeBuilder().withTags(WCAG_21_AA)
                                                  .analyze(WebDriverRunner.getWebDriver());
                AxeReporter.writeResultsToJsonFile(REPORTS.resolve(name)
                                                          .toString(),
                        results);
                countsByPage.put(name, countByImpact(results.getViolations()));
                String primary = Selenide.executeJavaScript(PRIMARY_COLOR);
                for (Rule violation : results.getViolations()) {
                    if (!ENFORCED_IMPACTS.contains(violation.getImpact())) {
                        continue;
                    }
                    List<CheckedNode> nodes = violation.getNodes()
                                                       .stream()
                                                       .filter(node -> !(CONTRAST_RULE.equals(violation.getId())
                                                               && involvesColor(node, primary)))
                                                       .toList();
                    if (!nodes.isEmpty()) {
                        enforced.add(name + ": " + describe(violation, nodes));
                    }
                }
            }
        }
        Files.writeString(REPORTS.resolve("summary.txt"), summary(countsByPage), StandardCharsets.UTF_8);

        assertTrue(enforced.isEmpty(), "serious or critical WCAG 2.1 AA violations on the generated pages (full report in " + REPORTS
                + "):\n" + String.join("\n", enforced));

        assertStatusStepIsAnnounced(invoice);
        assertTheLineDialogReturnsFocus(invoice);
    }

    /** The status step indicator announces the active step, not only colours it. */
    private void assertStatusStepIsAnnounced(int invoice) {
        browser.openPath(APP + "/Invoice/" + invoice + "/edit");
        Selenide.$$("[x-h-step-indicator-trigger][aria-current='step']")
                .shouldHave(CollectionCondition.size(1), RENDER_TIMEOUT);
    }

    /**
     * A dialog takes the focus when it opens and gives it back to what opened it when it closes -
     * Harmonia's own contract, guarded here because a page that re-renders its opener breaks it. Closed
     * with its Close button: the page wires no Esc dismissal for this dialog.
     */
    private void assertTheLineDialogReturnsFocus(int invoice) {
        browser.openPath(APP + "/Invoice/" + invoice + "/edit");
        SelenideElement add = Selenide.$("button[\\@click=\"openRowDialog(null)\"]")
                                      .shouldBe(Condition.visible, RENDER_TIMEOUT);
        add.click();
        awaitTrue("const d = document.activeElement; return !!(d && d.closest('[x-h-dialog]'));");
        Selenide.$("[x-h-dialog-overlay][data-open] [x-h-dialog-close]")
                .shouldBe(Condition.visible, RENDER_TIMEOUT)
                .click();
        awaitTrue("return document.activeElement === document.querySelector('button[\\\\@click=\"openRowDialog(null)\"]');");
    }

    /** Polls a script until it answers true; fails naming the script when the render timeout passes. */
    private static void awaitTrue(String script, Object... arguments) {
        long deadline = System.currentTimeMillis() + RENDER_TIMEOUT.toMillis();
        while (!Boolean.TRUE.equals(Selenide.executeJavaScript(script, arguments))) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Still false after " + RENDER_TIMEOUT.toSeconds() + " s: " + script);
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        }
    }

    private static Map<String, Integer> countByImpact(List<Rule> violations) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Rule violation : violations) {
            counts.merge(String.valueOf(violation.getImpact()), violation.getNodes()
                                                                         .size(),
                    Integer::sum);
        }
        return counts;
    }

    private static String describe(Rule violation, List<CheckedNode> nodes) {
        String targets = nodes.stream()
                              .limit(5)
                              .map(node -> String.valueOf(node.getTarget()))
                              .collect(Collectors.joining(", "));
        return "[" + violation.getImpact() + "] " + violation.getId() + " - " + violation.getHelp() + " (" + nodes.size() + " elements: "
                + targets + ")";
    }

    /** Whether the contrast axe measured for the node has the given colour on either side. */
    private static boolean involvesColor(CheckedNode node, String color) {
        for (Check check : node.getAny()) {
            if (check.getData() instanceof Map<?, ?> data
                    && (sameColor(data.get("fgColor"), color) || sameColor(data.get("bgColor"), color))) {
                return true;
            }
        }
        return false;
    }

    /** Two {@code #rrggbb} colours within the rounding of the colour-space conversions apart. */
    private static boolean sameColor(Object measured, String expected) {
        if (!(measured instanceof String hex) || hex.length() != 7 || expected == null || expected.length() != 7) {
            return false;
        }
        for (int channel = 1; channel < 7; channel += 2) {
            int a = Integer.parseInt(hex.substring(channel, channel + 2), 16);
            int b = Integer.parseInt(expected.substring(channel, channel + 2), 16);
            if (Math.abs(a - b) > 2) {
                return false;
            }
        }
        return true;
    }

    private static String summary(Map<String, Map<String, Integer>> countsByPage) {
        StringBuilder summary = new StringBuilder("axe-core WCAG 2.1 AA violations per page, by impact (elements)\n");
        countsByPage.forEach((page, counts) -> summary.append(page)
                                                      .append(": ")
                                                      .append(counts.isEmpty() ? "none" : counts.toString())
                                                      .append('\n'));
        return summary.toString();
    }

    private int create(String controller, String body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private void generateAndPublish() {
        byte[] intent = resource(getClass().getSimpleName() + "/app.intent");
        String path = PROJECT_PATH + "/app.intent";
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(intent);
        } else {
            repository.createResource(path, intent);
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/customer/CustomerController")
                                                 .then()
                                                 .statusCode(200),
                RENDER_TIMEOUT.toSeconds());
    }

    private static byte[] resource(String name) {
        try (InputStream in = AccessibilityHarmoniaIT.class.getClassLoader()
                                                           .getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("No resource [" + name + "] on the classpath");
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read [" + name + "]", ex);
        }
    }
}
