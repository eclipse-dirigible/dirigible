/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.intent.conversation.ConversationRole;
import org.eclipse.dirigible.components.intent.conversation.ConversationSurface;
import org.eclipse.dirigible.components.intent.conversation.IntentConversation;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationMessageRepository;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationRepository;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService.ConversationKey;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService.MessageDraft;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the persisted AI conversations behind the Builder and the Intent Editor -
 * {@code GET /services/ide/intent/conversations} and
 * {@code POST /services/ide/intent/conversations/messages}.
 *
 * <p>
 * The conversation is why an application looks the way it does, so what is asserted here is the
 * promise the clients depend on: a conversation restores in full for whoever opens the app next,
 * each surface keeps its own, every message carries its author and its time, and one tenant's
 * history is unreachable from another. HTTP-only, no Chrome, no synchronization cycles.
 */
class IntentConversationIT extends IntegrationTest {

    private static final String BASE_URL = "/services/ide/intent/conversations";
    private static final String PROJECT = "intent-conversation-it";
    private static final String INTENT_PATH = "app.intent";
    private static final String BUILDER = "builder";
    private static final String INTENT_EDITOR = "intent-editor";

    /**
     * The user the default-tenant requests authenticate as - every message must be attributed to it.
     */
    private static final String ADMIN = DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value();

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private IntentConversationService conversationService;

    @Autowired
    private IntentConversationRepository conversationRepository;

    @Autowired
    private IntentConversationMessageRepository messageRepository;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    @Test
    void an_app_conversation_is_restored_in_full_with_author_and_time() {
        restAssuredExecutor.execute(() -> {
            // Nothing has been said about this app yet: an empty conversation, never a 404 - the clients
            // must not have to treat "no history" as an error.
            given().when()
                   .get(url(BUILDER))
                   .then()
                   .statusCode(200)
                   .body("id", nullValue())
                   .body("project", equalTo(PROJECT))
                   .body("surface", equalTo(BUILDER))
                   .body("messages", hasSize(0))
                   .body("turns", hasSize(0));

            append(BUILDER, """
                    {"messages":[
                      {"role":"user","content":"Create an order management app"},
                      {"role":"assistant","content":"Added Customer and Order plus an approval process."}
                    ]}""");

            // This is the acceptance criterion: opening the same app - any browser, any machine, a
            // teammate - restores the same dialogue, with who said what and when.
            given().when()
                   .get(url(BUILDER))
                   .then()
                   .statusCode(200)
                   .body("id", notNullValue())
                   .body("createdAt", notNullValue())
                   .body("messages", hasSize(2))
                   .body("messages.sequence", contains(0, 1))
                   .body("messages.role", contains("user", "assistant"))
                   .body("messages[0].content", equalTo("Create an order management app"))
                   .body("messages[0].createdBy", equalTo(ADMIN))
                   .body("messages[0].createdAt", notNullValue())
                   .body("messages[1].content", equalTo("Added Customer and Order plus an approval process."));
        });
    }

    @Test
    void every_turn_is_appended_and_nothing_earlier_is_rewritten() {
        restAssuredExecutor.execute(() -> {
            append(BUILDER, """
                    {"messages":[
                      {"role":"user","content":"first ask"},
                      {"role":"assistant","content":"first answer"}
                    ]}""");
            // A failed turn contributes its error bubble - it is part of the record of what happened, and
            // a display-only `note`/`error` is never replayed to the model.
            append(BUILDER, """
                    {"messages":[
                      {"role":"user","content":"second ask"},
                      {"role":"error","content":"That change does not validate yet"},
                      {"role":"note","content":"Applied to the editor"}
                    ]}""");

            given().when()
                   .get(url(BUILDER))
                   .then()
                   .statusCode(200)
                   .body("messages", hasSize(5))
                   .body("messages.sequence", contains(0, 1, 2, 3, 4))
                   .body("messages.role", contains("user", "assistant", "user", "error", "note"))
                   .body("messages.content",
                           contains("first ask", "first answer", "second ask", "That change does not validate yet",
                                   "Applied to the editor"))
                   // The record keeps everything; the transcript keeps only what may be replayed. The
                   // second ask was never answered, so replaying it would send two consecutive user turns
                   // and the model API would reject the client's very next request.
                   .body("turns.role", contains("user", "assistant"))
                   .body("turns.content", contains("first ask", "first answer"));
        });
    }

    @Test
    void the_two_surfaces_keep_separate_conversations_for_the_same_app() {
        restAssuredExecutor.execute(() -> {
            append(BUILDER, """
                    {"messages":[{"role":"user","content":"asked in the Builder"}]}""");
            append(INTENT_EDITOR, """
                    {"messages":[{"role":"user","content":"asked in the Intent Editor"}]}""");

            given().when()
                   .get(url(BUILDER))
                   .then()
                   .statusCode(200)
                   .body("messages", hasSize(1))
                   .body("messages[0].content", equalTo("asked in the Builder"));
            given().when()
                   .get(url(INTENT_EDITOR))
                   .then()
                   .statusCode(200)
                   .body("messages", hasSize(1))
                   .body("messages[0].content", equalTo("asked in the Intent Editor"));
        });
    }

    @Test
    void a_request_that_names_no_real_surface_or_app_is_rejected() {
        restAssuredExecutor.execute(() -> {
            // An unrecognised surface would otherwise start a parallel conversation that is written to
            // and never restored from, with nothing reporting it.
            given().when()
                   .get(BASE_URL + "?project=" + PROJECT + "&surface=whiteboard&path=" + INTENT_PATH)
                   .then()
                   .statusCode(400);
            given().when()
                   .get(BASE_URL + "?project=&surface=" + BUILDER + "&path=" + INTENT_PATH)
                   .then()
                   .statusCode(400);
            given().contentType("application/json")
                   .body("{\"messages\":[]}")
                   .when()
                   .post(BASE_URL + "/messages?project=" + PROJECT + "&surface=" + BUILDER + "&path=" + INTENT_PATH)
                   .then()
                   .statusCode(400);
        });
    }

    @Test
    void a_tenant_can_neither_read_nor_extend_another_tenants_conversation() throws Exception {
        // The conversations live in one system table with a tenant column, and the whole isolation
        // mechanism is the tenant-scoped lookup in the service - so this drives the service directly in
        // two tenant scopes rather than provisioning a second tenant's schema, which the mechanism does
        // not involve.
        ConversationKey key = new ConversationKey(PROJECT, ConversationSurface.BUILDER, INTENT_PATH);
        Tenant rival = rivalTenant();

        tenantContext.execute(defaultTenant, () -> conversationService.append(key,
                List.of(new MessageDraft(ConversationRole.USER, "the default tenant's business data"))));

        tenantContext.execute(rival, () -> {
            assertTrue(conversationService.find(key)
                                          .isEmpty(),
                    "another tenant's conversation must be invisible");
            // Saying something under the same key must start the rival's OWN conversation, never extend
            // the one it cannot see.
            conversationService.append(key, List.of(new MessageDraft(ConversationRole.USER, "the rival tenant's own ask")));
            assertEquals(1, conversationService.messagesOf(conversationService.find(key)
                                                                              .orElseThrow())
                                               .size());
            return null;
        });

        tenantContext.execute(defaultTenant, () -> {
            IntentConversation conversation = conversationService.find(key)
                                                                 .orElseThrow();
            assertEquals(1, conversationService.messagesOf(conversation)
                                               .size(),
                    "the default tenant's conversation must be untouched by the rival's");
            assertEquals("the default tenant's business data", conversationService.messagesOf(conversation)
                                                                                  .get(0)
                                                                                  .getContent());
            return null;
        });
    }

    @AfterEach
    void removeConversations() {
        conversationRepository.findAll()
                              .stream()
                              .filter(conversation -> PROJECT.equals(conversation.getProject()))
                              .forEach(conversation -> {
                                  messageRepository.deleteAll(
                                          messageRepository.findByConversationIdOrderBySequenceAsc(conversation.getId()));
                                  conversationRepository.delete(conversation);
                              });
    }

    /** The read URL of one surface's conversation about the test app. */
    private static String url(String surface) {
        return BASE_URL + "?project=" + PROJECT + "&surface=" + surface + "&path=" + INTENT_PATH;
    }

    /** Appends one turn's messages and asserts the append succeeded. */
    private static void append(String surface, String body) {
        given().contentType("application/json")
               .body(body)
               .when()
               .post(BASE_URL + "/messages?project=" + PROJECT + "&surface=" + surface + "&path=" + INTENT_PATH)
               .then()
               .statusCode(200)
               .body("id", notNullValue());
    }

    /** A second tenant - only its id matters to the scoping under test. */
    private static Tenant rivalTenant() {
        return new Tenant() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return "intent-conversation-it-rival";
            }

            @Override
            public boolean isDefault() {
                return false;
            }

            @Override
            public String getName() {
                return "Intent Conversation IT Rival";
            }

            @Override
            public String getSubdomain() {
                return "intent-conversation-it-rival";
            }
        };
    }
}
