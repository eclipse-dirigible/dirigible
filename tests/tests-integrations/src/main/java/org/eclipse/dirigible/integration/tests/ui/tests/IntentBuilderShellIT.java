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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Browser test for the Builder shell - the conversational AI intent builder.
 *
 * <p>
 * The assistant's upstream is a local HTTP stub, so the whole journey runs with no Anthropic key
 * and no network: {@code DIRIGIBLE_INTENT_AI_BASE_URL} exists precisely for this, and the stub
 * answers the Messages API with a single {@code propose_intent} tool call, exactly as
 * {@code IntentAgentServiceTest} scripts the same upstream in-process.
 *
 * <p>
 * Two levels of cover. The smoke test asserts the shell BOOTSTRAPS - the class of regression that
 * services-level tests cannot catch, where every endpoint stays green while the page is dead (a
 * mis-ordered script, a store that never registers). The full journey then drives the actual
 * promise of the shell end to end: one sentence in the chat becomes a validated intent, the canvas
 * draws it, and the single Publish button turns it into a published application - covering the
 * client-side re-validation, the auto-apply, the hidden persistence, and every step of the publish
 * pipeline (generate models, generate code, publish, verify against the Problems feed).
 */
// One Dirigible boot for the whole class: the methods are read-only or clean up after themselves,
// so the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IntentBuilderShellIT extends UserInterfaceIntegrationTest {

    private static final String BUILDER_PATH = "/services/web/builder/index.html";
    private static final String API_KEY_ENV = "DIRIGIBLE_INTENT_AI_API_KEY";
    private static final String BASE_URL_ENV = "DIRIGIBLE_INTENT_AI_BASE_URL";

    /**
     * The intent the stubbed assistant proposes - deliberately minimal, so the journey is about the
     * shell.
     */
    private static final String PROPOSED_INTENT = """
            name: expenses
            description: Track employee expenses
            entities:
              - name: Expense
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string,  length: 200, required: true }
                  - { name: amount,      type: decimal, precision: 12, scale: 2 }
            """;

    /** How wide each streamed fragment of the proposal is - narrow, so there are many of them. */
    private static final int FRAGMENT_WIDTH = 23;

    /** The project the shell derives from the intent's name - the user never types or sees it. */
    private static final String PROJECT = "expenses";
    private static final String WORKSPACE_PROJECT = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + PROJECT;
    private static final String INTENT_PATH = WORKSPACE_PROJECT + "/app.intent";

    @Autowired
    private IRepository repository;

    private HttpServer upstream;

    @BeforeEach
    void startStubbedAssistant() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress(0), 0);
        upstream.createContext("/v1/messages", this::respondWithProposal);
        upstream.start();
        Configuration.set(API_KEY_ENV, "test-key");
        Configuration.set(BASE_URL_ENV, "http://localhost:" + upstream.getAddress()
                                                                      .getPort());
    }

    @AfterEach
    void stopStubbedAssistant() {
        Configuration.remove(API_KEY_ENV);
        Configuration.remove(BASE_URL_ENV);
        if (upstream != null) {
            upstream.stop(0);
        }
    }

    /**
     * The Messages API shape the agent service parses: one {@code propose_intent} tool-use block,
     * delivered as the server-sent event stream the client asks for ({@code "stream": true}).
     *
     * <p>
     * The tool input is split across several {@code input_json_delta} fragments on purpose - a stub
     * that sent it in one piece would pass even if the client only ever read the first fragment, which
     * is exactly the assembly this journey is meant to exercise end to end.
     */
    private void respondWithProposal(HttpExchange exchange) throws IOException {
        String toolInput = new Gson().toJson(
                Map.of("explanation", "Added an Expense entity with a description and an amount.", "yaml", PROPOSED_INTENT));
        StringBuilder stream = new StringBuilder();
        stream.append(event("message_start", "{\"type\":\"message_start\",\"message\":{\"id\":\"msg_stub\",\"content\":[]}}"));
        stream.append(event("content_block_start", "{\"type\":\"content_block_start\",\"index\":0,\"content_block\":"
                + "{\"type\":\"tool_use\",\"id\":\"toolu_stub\",\"name\":\"propose_intent\",\"input\":{}}}"));
        for (int start = 0; start < toolInput.length(); start += FRAGMENT_WIDTH) {
            JsonObject delta = new JsonObject();
            delta.addProperty("type", "input_json_delta");
            delta.addProperty("partial_json", toolInput.substring(start, Math.min(start + FRAGMENT_WIDTH, toolInput.length())));
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "content_block_delta");
            payload.addProperty("index", 0);
            payload.add("delta", delta);
            stream.append(event("content_block_delta", payload.toString()));
        }
        stream.append(event("content_block_stop", "{\"type\":\"content_block_stop\",\"index\":0}"));
        stream.append(event("message_delta", "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\"}}"));
        stream.append(event("message_stop", "{\"type\":\"message_stop\"}"));

        byte[] payload = stream.toString()
                               .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    /** One server-sent event: the {@code event:} name line, then its single-line JSON data line. */
    private static String event(String type, String data) {
        return "event: " + type + "\ndata: " + data + "\n\n";
    }

    @Test
    @Tag("smoke")
    void builderShell_loads_and_bootstraps() {
        openBuilder();

        // Alpine started and the Builder's own stores registered - a mis-ordered or missing script
        // leaves the page rendering nothing while every endpoint behind it stays green. Polled rather
        // than sampled once: openBuilder() returns as soon as navigation does, and the shell's scripts
        // are deferred, so a single probe races the boot and reports a healthy page as broken on a slow
        // runner. A genuinely mis-ordered script never registers the stores, so it still fails - just at
        // the timeout instead of instantly.
        Awaitility.await()
                  // In the calling thread: Selenide binds its WebDriver per-thread, so a probe on
                  // Awaitility's own poll thread finds no driver at all.
                  .pollInSameThread()
                  .atMost(Duration.ofSeconds(60))
                  .pollInterval(Duration.ofMillis(250))
                  .untilAsserted(() -> {
                      Object bootstrapped = Selenide.executeJavaScript("return !!(window.Alpine && Alpine.store('intent')"
                              + " && Alpine.store('conversation') && Alpine.store('publish') && window.IntentDiagrams);");
                      Assertions.assertTrue(Boolean.TRUE.equals(bootstrapped),
                              "The Builder shell failed to bootstrap its stores and renderer.");
                  });

        // The first-run surface is there: the invitation and the composer. Explicit deadlines, like every
        // other wait in this class - Selenide's default is 4s, and Alpine renders these AFTER the stores
        // the poll above waits for, so the default races the boot rather than testing it.
        Selenide.$(By.xpath("//*[contains(text(), 'Describe the application you need')]"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.$(By.id("builder-input"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));

        // The assistant IS configured here (the stub), so the shell must not claim otherwise.
        Selenide.$(By.xpath("//*[contains(text(), 'is not configured on this instance')]"))
                .shouldNotBe(Condition.visible);
    }

    @Test
    void an_unconfigured_assistant_is_announced_before_the_user_types() {
        // Without a key the shell can do nothing, so it says so up front rather than letting the
        // first message fail. The status probe is configuration-only server-side - this banner costs
        // no upstream model call, which is why it can run on every page load.
        Configuration.remove(API_KEY_ENV);

        openBuilder();

        Selenide.$(By.xpath("//*[contains(text(), 'is not configured on this instance')]"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
    }

    @Test
    void a_conversation_becomes_a_published_application() {
        openBuilder();

        // The composer appears once the shell has booted, which openBuilder() does not wait for.
        Selenide.$(By.id("builder-input"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60))
                .setValue("I need an expense tracker.");
        Selenide.$(By.cssSelector("button[aria-label='Send']"))
                .shouldBe(Condition.enabled)
                .click();

        // The assistant's explanation lands in the transcript.
        Selenide.$(By.xpath("//*[contains(text(), 'Added an Expense entity')]"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));

        // The proposal was re-validated client-side and auto-applied: the canvas draws the model.
        Selenide.$(By.cssSelector("#builder-diagrams .intent-diagram svg"))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));
        Selenide.$(By.xpath("//div[@id='builder-diagrams']//*[contains(text(), 'Expense')]"))
                .shouldBe(Condition.visible);

        // Hidden persistence: the project was created and the intent saved, with nothing asked of the user.
        Assertions.assertTrue(repository.getResource(INTENT_PATH)
                                        .exists(),
                "The accepted proposal was not saved as " + INTENT_PATH);

        // The one button: generate the models, generate the code, publish, verify.
        Selenide.$(By.xpath("//button[contains(normalize-space(.), 'Publish')]"))
                .shouldBe(Condition.enabled)
                .click();

        // The success state is the visible sign the conversation became a running application. A
        // failed pipeline renders its own panel instead, so this assertion cannot pass on a bad run.
        Selenide.$(By.xpath("//*[contains(text(), 'is live.')]"))
                .shouldBe(Condition.visible, Duration.ofMinutes(5));

        // ...and the verdict is backed by what the pipeline actually produced: the intent generated
        // its model files, and publishing copied the project into the registry. Asserting the effect
        // as well as the panel keeps the test honest if the success state is ever reworked.
        Assertions.assertTrue(repository.getResource(WORKSPACE_PROJECT + "/expenses.model")
                                        .exists(),
                "Generate did not write the model file into the workspace project");
        Assertions.assertTrue(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/app.intent")
                                        .exists(),
                "Publish did not copy the project into the registry");

        // Continuity: the conversation is the record of WHY the app looks the way it does, so it has to
        // outlive the browser session. Nothing in the shell reads the transcript from the browser any
        // more, so a reload that shows the dialogue again can only have restored it from the server.
        openBuilder();
        Selenide.$(By.xpath("//div[contains(@class, 'builder-message') and contains(text(), 'I need an expense tracker.')]"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        Selenide.$(By.xpath("//div[contains(@class, 'builder-message') and contains(text(), 'Added an Expense entity')]"))
                .shouldBe(Condition.visible);
    }

    private void openBuilder() {
        ide.openHomePage();
        browser.openPath(BUILDER_PATH);
    }
}
