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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

/**
 * An intent action that calls the application's OWN endpoint (issue #8090): a hand-written
 * {@code TicketApi} owns the escalation rule, and the generated UI offers the button, collects the
 * prompt, sends the typed request and reports the endpoint's answer - with no page and no generated
 * controller written for it.
 *
 * <p>
 * The roles are checked over HTTP, as the users who hold them: the views aggregation is what drops
 * the button for a caller without the token, so a SupportLead receives it and an Agent does not.
 * The rest runs in the browser, as the administrator, who is offered every button.
 */
class IntentCallActionSampleIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "helpdesk";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String TICKET_API = "/services/java/" + PROJECT + "/custom/api/TicketApi";
    private static final String ACTIONS = "/services/platform/extensions/views?extensionPoints=" + PROJECT + "-custom-action";
    private static final String PASSWORD = "secret";

    private static final String INTENT_YAML = """
            name: helpdesk
            entities:
              - name: TicketStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: EscalationStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Ticket
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }
                related:
                  - { entity: Escalation, show: [id, reason, Status] }
              - name: Escalation
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: reason, type: string }
                relations:
                  - { name: Ticket, kind: manyToOne, to: Ticket }
                  - { name: Status, kind: manyToOne, to: EscalationStatus, function: EntityStatus, init: 1 }
            actions:
              - name: EscalateTicket
                forEntity: Ticket
                label: Escalate
                icon: arrow-up
                fromStatus: [OPEN, ON_HOLD]
                can: Ticket:escalate
                prompt:
                  - { name: level, type: integer, label: Level, required: true }
                  - { name: note,  type: text,    label: Note }
                call:
                  method: POST
                  path: /services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations
                  body: { kind: MANUAL, level: "{level}", note: "{note}" }
                success: Escalation requested
              - name: RetryEscalation
                forEntity: Escalation
                label: Retry
                fromStatus: [FAILED]
                can: Escalation:retry
                confirm: Run this escalation again?
                primary: true
                call:
                  method: POST
                  path: /services/java/helpdesk/custom/api/TicketApi/escalations/{id}/retry
            permissions:
              - role: SupportLead
                can: [Ticket:read, Ticket:escalate, Escalation:read, Escalation:retry]
              - role: Agent
                can: [Ticket:read, Escalation:read]
            seeds:
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: ON_HOLD }
                  - { id: 3, name: CLOSED }
              - name: escalation-statuses
                entity: EscalationStatus
                rows:
                  - { id: 1, name: PENDING }
                  - { id: 2, name: DONE }
                  - { id: 3, name: FAILED }
              - name: tickets
                entity: Ticket
                rows:
                  - { id: 1, subject: Printer jammed, Status: 1 }
                  - { id: 2, subject: Password reset, Status: 3 }
              - name: escalations
                entity: Escalation
                rows:
                  - { id: 1, reason: Pager down, Ticket: 1, Status: 3 }
            """;

    /**
     * The application's own endpoint: it owns the rule (a CLOSED ticket cannot be escalated, answered
     * with a 409 written for people), answers 202, and records what it received for the test to read.
     */
    private static final String TICKET_API_JAVA = """
            package custom.api;

            import java.util.List;
            import java.util.Map;
            import java.util.concurrent.CopyOnWriteArrayList;

            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.http.Body;
            import org.eclipse.dirigible.sdk.http.Controller;
            import org.eclipse.dirigible.sdk.http.Get;
            import org.eclipse.dirigible.sdk.http.PathParam;
            import org.eclipse.dirigible.sdk.http.Post;
            import org.eclipse.dirigible.sdk.http.Request;
            import org.eclipse.dirigible.sdk.http.Response;
            import org.springframework.http.HttpStatus;
            import org.springframework.web.server.ResponseStatusException;

            import gen.helpdesk.data.ticket.TicketEntity;
            import gen.helpdesk.data.ticket.TicketRepository;

            @Controller
            @Component
            public class TicketApi {

                private static final int CLOSED = 3;

                private final TicketRepository tickets;

                private final List<Map<String, Object>> received = new CopyOnWriteArrayList<>();

                public TicketApi(TicketRepository tickets) {
                    this.tickets = tickets;
                }

                @Post("/tickets/{id}/escalations")
                public void escalate(@PathParam("id") Integer id, @Body Map<String, Object> body) {
                    TicketEntity ticket = tickets.findById(id);
                    if (ticket.Status != null && ticket.Status == CLOSED) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket [" + id + "] is CLOSED");
                    }
                    received.add(Map.of("call", "escalate " + id, "body", body));
                    Response.getNative().setStatus(202);
                }

                @Post("/escalations/{id}/retry")
                public void retry(@PathParam("id") Integer id) {
                    received.add(Map.of("call", "retry " + id, "contentLength", Request.getNative().getContentLengthLong()));
                    Response.getNative().setStatus(202);
                }

                @Get("/received")
                public List<Map<String, Object>> received() {
                    return received;
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void a_call_action_sends_the_typed_request_to_the_applications_own_endpoint_and_reports_its_answer() {
        writeProjectFile("app.intent", INTENT_YAML);
        generateProject();
        writeProjectFile("custom/api/TicketApi.java", TICKET_API_JAVA);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Who is offered the buttons is decided by the server: the SupportLead receives both, an Agent -
        // whose role lists neither token - receives neither.
        securityUtil.createUserInDefaultTenant("lead", PASSWORD, "SupportLead");
        securityUtil.createUserInDefaultTenant("agent", PASSWORD, "Agent");
        assertEquals(List.of("helpdesk-Escalation-RetryEscalation", "helpdesk-Ticket-EscalateTicket"), actionIds("lead"));
        assertEquals(List.of(), actionIds("agent"));

        // The administrator escalates the OPEN ticket through the dialog.
        ide.openHomePage();
        browser.openPath("/services/web/" + PROJECT + "/gen/helpdesk/index.html#/Ticket");
        browser.clickOnElementContainingText("td", "Printer jammed");
        Selenide.$("[x-h-sheet] button[aria-label='More actions']")
                .shouldBe(Condition.visible)
                .click();
        browser.clickOnElementContainingText(HtmlElementType.LI, "Escalate");
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER2, "Escalate");
        Selenide.switchTo()
                .defaultContent();
        // The values are entered through the store the dialog's controls bind (see
        // GeneratesPromptHarmoniaIT for why a synthesized pointer cannot drive the Harmonia popups);
        // promptRun() is what the dialog's Run button invokes - the required check, then the call.
        assertEquals("level:NUMBER,note:TEXT",
                Selenide.executeJavaScript("return Alpine.store('customActions').promptCols.map(c => c.name + ':' + c.widget).join(',');"));
        Selenide.executeJavaScript("var s = Alpine.store('customActions'); s.promptValues.level = 2;"
                + " s.promptValues.note = 'VIP customer'; s.promptRun();");
        awaitAnnouncement("Escalation requested");
        List<Map<String, Object>> escalations = received();
        assertEquals(1, escalations.size(), "one escalation reached the controller: " + escalations);
        assertEquals("escalate 1", escalations.get(0)
                                              .get("call"));
        assertEquals(Map.of("kind", "MANUAL", "level", 2, "note", "VIP customer"), escalations.get(0)
                                                                                              .get("body"),
                "the controller receives the templated body, the level as a number");

        // The same action is offered in the two other menus a user opens on a record (#8116): the row's
        // own menu in the list, and the record page's - for the OPEN ticket, not for the CLOSED one.
        // The escalation above left the ticket's sheet open over the list: close it, and load the list
        // afresh.
        Selenide.actions()
                .sendKeys(Keys.ESCAPE)
                .perform();
        browser.openPath("/services/web/" + PROJECT + "/gen/helpdesk/index.html#/Ticket");
        Selenide.refresh();
        List<String> open = rowMenu("Printer jammed");
        assertTrue(open.contains("Escalate"), "the OPEN ticket's row menu offers Escalate: " + open);
        List<String> closed = rowMenu("Password reset");
        assertFalse(closed.contains("Escalate"), "the CLOSED ticket's row menu does not: " + closed);
        List<String> openPage = pageMenu(1);
        assertTrue(openPage.contains("Escalate"), "the OPEN ticket's page offers Escalate in its More actions: " + openPage);
        List<String> closedPage = pageMenu(2);
        assertFalse(closedPage.contains("Escalate"), "the CLOSED ticket's page does not: " + closedPage);

        // The ticket's Escalations register offers the FAILED escalation's Retry beside Open (#8118).
        List<String> register = registerRowMenu(1, "Pager down");
        assertTrue(register.contains("Retry"), "the register row of a FAILED escalation offers Retry: " + register);

        // Retry is declared primary (#8119): on the FAILED escalation it is the filled button - in the
        // list's sheet and on its page - and Edit steps down.
        browser.openPath("/services/web/" + PROJECT + "/gen/helpdesk/index.html#/Escalation");
        Selenide.refresh();
        browser.clickOnElementContainingText("td", "Pager down");
        Selenide.$$("[x-h-sheet] button[data-variant='primary']")
                .findBy(Condition.exactText("Retry"))
                .shouldBe(Condition.visible);
        Selenide.actions()
                .sendKeys(Keys.ESCAPE)
                .perform();
        browser.openPath("/services/web/" + PROJECT + "/gen/helpdesk/index.html#/Escalation/1/preview");
        Selenide.refresh();
        Selenide.$$("button[data-variant='primary']")
                .filterBy(Condition.visible)
                .findBy(Condition.exactText("Retry"))
                .shouldBe(Condition.visible);
        Selenide.$$("button[data-variant='outline']")
                .filterBy(Condition.visible)
                .findBy(Condition.exactText("Edit"))
                .shouldBe(Condition.visible);

        // A CLOSED ticket is offered no Escalate button; forced anyway, the endpoint's own refusal is
        // what the user reads.
        assertEquals(Boolean.FALSE,
                Selenide.executeJavaScript("return Alpine.store('customActions').getActions('Ticket', 'entity', { Id: 2, Status: 3 })"
                        + ".some(a => a.label === 'Escalate');"));
        Selenide.executeJavaScript(
                "var s = Alpine.store('customActions');" + " s.runCall(s.actions.find(a => a.label === 'Escalate'), 2, { level: 1 });");
        awaitAnnouncement("Ticket [2] is CLOSED");

        // Retry on a FAILED escalation: a confirm, then a POST with no body.
        assertEquals("Run this escalation again?", Selenide.executeJavaScript("var s = Alpine.store('customActions');"
                + " s.trigger(s.actions.find(a => a.label === 'Retry'), 1); return s.confirmAction.confirm;"));
        Selenide.executeJavaScript("Alpine.store('customActions').confirmRun();");
        awaitAnnouncement("Retry completed");
        List<Map<String, Object>> received = received();
        assertEquals("retry 1", received.get(received.size() - 1)
                                        .get("call"));
        assertTrue(((Number) received.get(received.size() - 1)
                                     .get("contentLength")).longValue() <= 0,
                "a call without a body sends none: " + received);
    }

    /**
     * The items of a list row's own menu (its ⋯ cell), opened on the row whose text contains the
     * subject.
     */
    private static List<String> rowMenu(String subject) {
        SelenideElement row = Selenide.$$("tbody tr")
                                      .findBy(Condition.text(subject))
                                      .shouldBe(Condition.visible);
        SelenideElement trigger = row.$("button[aria-label='Row actions']");
        trigger.click();
        // The menu that trigger opened - the menu component renders it outside the row and names its
        // trigger, and a menu opened earlier may still be on screen.
        ElementsCollection items = Selenide.$$("ul[aria-labelledby='" + trigger.getAttribute("id") + "'] li[x-h-menu-item]")
                                           .filterBy(Condition.visible);
        items.first()
             .shouldBe(Condition.visible);
        List<String> texts = items.texts();
        Selenide.actions()
                .sendKeys(Keys.ESCAPE)
                .perform();
        return texts;
    }

    /**
     * The items of a record page's More actions menu, in preview, once the page has loaded its record.
     */
    private List<String> pageMenu(int id) {
        browser.openPath("/services/web/" + PROJECT + "/gen/helpdesk/index.html#/Ticket/" + id + "/preview");
        Selenide.refresh();
        // The page's own record, once loaded: the actions' status guards read it.
        String loaded = "var el = document.querySelector('[x-data=\"TicketFormPage\"]');"
                + " var page = el && window.Alpine ? Alpine.$data(el) : null;"
                + " return !!(page && page.loadedRecord && page.loadedRecord.Id == " + id + ");";
        for (int i = 0; i < 40 && !Boolean.TRUE.equals(Selenide.executeJavaScript(loaded)); i++) {
            Selenide.sleep(250);
        }
        assertEquals(Boolean.TRUE, Selenide.executeJavaScript(loaded), "ticket " + id + "'s page loaded its record");
        SelenideElement trigger = Selenide.$$("button[aria-label='More actions']")
                                          .filterBy(Condition.visible)
                                          .first()
                                          .shouldBe(Condition.visible);
        trigger.click();
        ElementsCollection items = Selenide.$$("ul[aria-labelledby='" + trigger.getAttribute("id") + "'] li[x-h-menu-item]")
                                           .filterBy(Condition.visible);
        items.first()
             .shouldBe(Condition.visible);
        List<String> texts = items.texts();
        Selenide.actions()
                .sendKeys(Keys.ESCAPE)
                .perform();
        return texts;
    }

    /**
     * The items of a related register row's menu, on the given ticket's page, opened on the row with
     * the text.
     */
    private List<String> registerRowMenu(int ticket, String text) {
        browser.openPath("/services/web/" + PROJECT + "/gen/helpdesk/index.html#/Ticket/" + ticket + "/preview");
        Selenide.refresh();
        SelenideElement row = Selenide.$$("div[x-data^='relatedPanel'] tbody tr")
                                      .findBy(Condition.text(text))
                                      .shouldBe(Condition.visible);
        SelenideElement trigger = row.$("button[aria-label='Row actions']")
                                     .shouldBe(Condition.visible);
        trigger.click();
        ElementsCollection items = Selenide.$$("ul[aria-labelledby='" + trigger.getAttribute("id") + "'] li[x-h-menu-item]")
                                           .filterBy(Condition.visible);
        items.first()
             .shouldBe(Condition.visible);
        List<String> texts = items.texts();
        Selenide.actions()
                .sendKeys(Keys.ESCAPE)
                .perform();
        return texts;
    }

    /** The custom-action descriptors the given user is offered, by id. */
    private List<String> actionIds(String user) {
        AtomicReference<List<String>> ids = new AtomicReference<>();
        restAssuredExecutor.execute(() -> ids.set(given().when()
                                                         .get(ACTIONS)
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .getList("id", String.class)
                                                         .stream()
                                                         .sorted()
                                                         .toList()),
                user, PASSWORD, 60);
        return ids.get();
    }

    private List<Map<String, Object>> received() {
        AtomicReference<List<Map<String, Object>>> received = new AtomicReference<>();
        restAssuredExecutor.execute(() -> received.set(given().when()
                                                              .get(TICKET_API + "/received")
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .jsonPath()
                                                              .getList("$")),
                30);
        return received.get();
    }

    /** Waits for the shell to announce an outcome whose description contains the text. */
    private static void awaitAnnouncement(String text) {
        String descriptions = "";
        for (int i = 0; i < 40; i++) {
            descriptions = Selenide.executeJavaScript("return Alpine.store('notifications').items.map(n => n.description).join(' | ');");
            if (descriptions != null && descriptions.contains(text)) {
                return;
            }
            Selenide.sleep(250);
        }
        fail("expected an announcement containing [" + text + "], got [" + descriptions + "]");
    }

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generateProject() {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
