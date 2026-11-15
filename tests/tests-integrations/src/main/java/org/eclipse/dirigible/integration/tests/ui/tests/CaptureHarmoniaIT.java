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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sun.net.httpserver.HttpServer;

/**
 * The browser half of {@code captures:} (#8544): the shared capture page
 * ({@code application-core/shell/views/capture.html}) driven the way a person uses it. The list's
 * "Bill Scan" action opens it in the app-wide dialog; a photographed bill is uploaded to the
 * generated capture endpoint (whose vision-model provider is a local server, as in
 * {@code IntentCaptureIT}); the page switches in place to the review step - the kept file beside
 * what the capture flagged - and Done refreshes the list and announces the outcome. The record's
 * own "Bill Scan review" action then opens the same page on the existing record.
 *
 * <p>
 * The endpoint's behaviour (what is sent to the provider, the record, the ledger) is asserted over
 * HTTP by {@code IntentCaptureIT}; this asserts what a person sees and presses.
 */
class CaptureHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "billcapture";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String VENDORS = API + "/vendor/VendorController";
    private static final String SCANS = API + "/bill/BillScanController";
    private static final String APP = "/services/web/" + PROJECT + "/gen/" + PROJECT + "/index.html#";
    private static final String PAGE = "/services/web/application-core/shell/views/capture.html";
    private static final String QR_TEXT = "DT123456*0000042*2026-03-15*10:20:30*120.00";
    private static final long TIMEOUT_SECONDS = 90;
    private static final Duration WAIT = Duration.ofSeconds(60);

    /**
     * The intent of {@code IntentCaptureIT}: a bill captured from a scan, with its lines and its file.
     */
    private static final String INTENT_YAML = """
            name: billcapture
            description: supplier bills captured from a scan

            entities:
              - name: Vendor
                fields:
                  - { name: id,                 type: integer, primaryKey: true, generated: true }
                  - { name: name,               type: string,  length: 100 }
                  - { name: registrationNumber, type: string,  length: 20 }
                  - { name: vatNumber,          type: string,  length: 20 }
              - name: Bill
                fields:
                  - { name: id,           type: integer, primaryKey: true, generated: true }
                  - { name: vendorNumber, type: string,  length: 50, required: true }
                  - { name: billedOn,     type: date }
                  - { name: gross,        type: decimal, precision: 18, scale: 2 }
                  - { name: net,          type: decimal, precision: 18, scale: 2 }
                  - { name: vat,          type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "round(Net * 0.2, 2)", calculatedOnUpdate: "round(Net * 0.2, 2)" }
                relations:
                  - { name: Vendor, kind: manyToOne, to: Vendor }
              - name: BillLine
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string,  length: 200, required: true }
                  - { name: quantity,    type: decimal, precision: 18, scale: 3 }
                  - { name: price,       type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }
              - name: BillScan
                function: Attachment
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }

            captures:
              - name: billScan
                into: Bill
                attachment: BillScan
                instructions: "The vendor is the party that issued the document."
                qr: { separator: "*", parts: [device, receipt, date, time, total] }
                fields:
                  - { field: vendorNumber, describe: "the vendor's own number of the document" }
                  - { field: billedOn, qr: date }
                  - { field: gross, qr: total }
                  - { field: net }
                  - { field: vat, compare: true }
                  - { field: Vendor, match: [registrationNumber, vatNumber], describe: "the vendor" }
                items:
                  into: BillLine
                  fields:
                    - { field: description }
                    - { field: quantity }
                    - { field: price }
            """;

    /** What the model "read": imperfect on purpose, so six things are left to review. */
    private static final String ANSWER = """
            {"fields":{
              "vendorNumber":{"value":"INV-77","confidence":"high"},
              "billedOn":{"value":"2026-03-14","confidence":"high"},
              "gross":{"value":120.0,"confidence":"medium"},
              "net":{"value":100,"confidence":"high"},
              "vat":{"value":21.00,"confidence":"high"},
              "Vendor_registrationNumber":{"value":"999999999","confidence":"high"},
              "Vendor_vatNumber":{"value":"BG123456789","confidence":"high"}},
             "items":[
              {"description":"Paper A4","quantity":2,"price":30.00,"confidence":"high"},
              {"description":"Toner","quantity":1,"price":null,"confidence":"low"},
              {"description":null,"quantity":3,"price":4.50,"confidence":"high"}]}
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    private HttpServer provider;
    private final List<JsonObject> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startProvider() throws IOException {
        provider = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        provider.createContext("/v1/messages", exchange -> {
            requests.add(new JsonObject());
            exchange.getRequestBody()
                    .readAllBytes();
            JsonObject reply = new JsonObject();
            reply.addProperty("model", "claude-test-model");
            JsonObject usage = new JsonObject();
            usage.addProperty("input_tokens", 1800);
            usage.addProperty("output_tokens", 240);
            reply.add("usage", usage);
            reply.addProperty("stop_reason", "end_turn");
            JsonArray content = new JsonArray();
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "thinking");
            thinking.addProperty("thinking", "");
            content.add(thinking);
            JsonObject text = new JsonObject();
            text.addProperty("type", "text");
            text.addProperty("text", ANSWER);
            content.add(text);
            reply.add("content", content);
            byte[] bytes = reply.toString()
                                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders()
                    .add("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        provider.start();
        Configuration.set("DIRIGIBLE_CAPTURE_API_KEY", "test-key");
        Configuration.set("DIRIGIBLE_CAPTURE_BASE_URL", "http://localhost:" + provider.getAddress()
                                                                                      .getPort());
    }

    @Test
    void a_scan_is_captured_in_the_dialog_reviewed_beside_its_file_and_reopened_from_the_record() throws IOException {
        // The page is a static file of the shared shell runtime: it must be served at the URL the
        // generator contributes.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PAGE)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("capturePage")),
                TIMEOUT_SECONDS);

        writeProjectFile("app.intent", INTENT_YAML);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        create(VENDORS, "{\"Name\":\"ACME OOD\",\"RegistrationNumber\":\"111111111\",\"VatNumber\":\"BG123456789\"}");
        File scan = scanFile();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();
        browser.openPath(APP + "/Bill");

        // The list's toolbar offers the capture, labelled with the capture's humanized name.
        awaitAction("Bill Scan");
        SelenideElement more = Selenide.$$("button[aria-label='More actions']")
                                       .filterBy(Condition.visible)
                                       .first()
                                       .shouldBe(Condition.visible);
        more.click();
        browser.clickOnElementContainingText(HtmlElementType.LI, "Bill Scan");

        // The upload step, in the dialog's iframe.
        enterDialog();
        Selenide.$("#capture-upload")
                .shouldBe(Condition.visible);
        SelenideElement capture = Selenide.$("#capture-submit")
                                          .shouldBe(Condition.visible);
        capture.shouldBe(Condition.disabled);
        Selenide.$("#capture-file")
                .uploadFile(scan);
        Selenide.$("#capture-chosen")
                .shouldHave(Condition.text("bill.png"));
        capture.shouldBe(Condition.enabled)
               .click();

        // The review step: the file on the left, the six things to look at on the right.
        Selenide.$("#capture-review")
                .shouldBe(Condition.visible, WAIT);
        Selenide.$("#capture-flag-count")
                .shouldHave(Condition.exactText("6 things to review"));
        List<String> flags = flags();
        assertEquals(List.of("billedOn QR_MISMATCH", "items[2] LINE", "items[2].price MISSING", "items[3] LINE_SKIPPED",
                "items[3].description MISSING", "vat MISMATCH"), flags);
        Selenide.$("[data-flag-field='billedOn'] [data-flag-label]")
                .shouldHave(Condition.exactText("Billed on"));
        Selenide.$("[data-flag-field='items[2].price'] [data-flag-label]")
                .shouldHave(Condition.exactText("Line 2 - price"));
        Selenide.$("[data-flag-field='items[3]'] [data-flag-message]")
                .shouldHave(Condition.text("the line was not created"));
        Selenide.$("[data-flag-field='vat'][data-flag-reason='MISMATCH']")
                .shouldHave(Condition.text("Differs from the calculated value"));
        Selenide.$("#capture-file-name")
                .shouldHave(Condition.exactText("bill.png"));

        String billId = billIdOf("INV-77");
        int scanId = scanIdOf(billId);
        SelenideElement image = Selenide.$("#capture-image")
                                        .shouldBe(Condition.visible, WAIT);
        image.shouldHave(Condition.attributeMatching("src", ".*" + SCANS + "/" + scanId + "/download\\?disposition=inline"));
        image.shouldHave(Condition.attribute("alt", "the captured document"));
        assertTrue(Boolean.TRUE.equals(Selenide.executeJavaScript("return document.getElementById('capture-image').naturalWidth > 0;")),
                "the kept file is actually shown");
        System.out.println("CAPTURE-REVIEW-SCREENSHOT: " + Selenide.screenshot("capture-review"));

        // Done reports the outcome: the shell toasts it and the list shows the new bill.
        Selenide.$("#capture-done")
                .click();
        leaveDialog();
        awaitAnnouncement("Captured bill.png: 6 to review");
        Selenide.$$("tbody tr")
                .findBy(Condition.text("INV-77"))
                .shouldBe(Condition.visible, WAIT);

        // Opened again from the record: the same page on the existing record, the same flags.
        browser.clickOnElementContainingText("td", "INV-77");
        SelenideElement sheetMore = Selenide.$("[x-h-sheet] button[aria-label='More actions']")
                                            .shouldBe(Condition.visible);
        sheetMore.click();
        browser.clickOnElementContainingText(HtmlElementType.LI, "Bill Scan review");
        enterDialog();
        Selenide.$("#capture-review")
                .shouldBe(Condition.visible, WAIT);
        Selenide.$("#capture-upload")
                .shouldNotBe(Condition.visible);
        Selenide.$("#capture-flag-count")
                .shouldHave(Condition.exactText("6 things to review"));
        assertEquals(flags, flags());
        Selenide.$("#capture-image")
                .shouldBe(Condition.visible, WAIT)
                .shouldHave(Condition.attributeMatching("src", ".*" + SCANS + "/" + scanId + "/download\\?disposition=inline"));
        Selenide.$("#capture-file-name")
                .shouldHave(Condition.exactText("bill.png"));
        // Nothing changed, so Done dismisses silently: no second announcement.
        leaveDialog();
        int announced = announcements();
        enterDialog();
        Selenide.$("#capture-done")
                .click();
        leaveDialog();
        Selenide.sleep(500);
        assertEquals(announced, announcements(), "reviewing an existing record announces nothing");
        assertEquals(1, requests.size(), "one upload is one provider call");
    }

    @Test
    void a_link_that_names_something_other_than_a_generated_controller_is_refused_before_anything_is_sent() {
        ide.openHomePage();
        for (String capture : List.of("//evil.example/services/java/x", "https://evil.example/services/java/x", "/services/java/../ide/x",
                "/services/ide/workspace", "/services/java/a%2e%2e/b")) {
            browser.openPath(PAGE + "?capture=" + java.net.URLEncoder.encode(capture, StandardCharsets.UTF_8) + "&entity=Bill");
            Selenide.$("#capture-invalid")
                    .shouldBe(Condition.visible);
            Selenide.$("#capture-file")
                    .shouldNotBe(Condition.visible);
        }
        browser.openPath(PAGE + "?capture="
                + java.net.URLEncoder.encode("/services/java/billcapture/gen/events/billcapture/BillScanCapture", StandardCharsets.UTF_8)
                + "&files=" + java.net.URLEncoder.encode("//evil.example/files", StandardCharsets.UTF_8));
        Selenide.$("#capture-invalid")
                .shouldBe(Condition.visible);
        browser.openPath(PAGE + "?capture="
                + java.net.URLEncoder.encode("/services/java/billcapture/gen/events/billcapture/BillScanCapture", StandardCharsets.UTF_8)
                + "&entity=Bill");
        Selenide.$("#capture-upload")
                .shouldBe(Condition.visible);
    }

    /** Moves into the app-wide dialog's iframe, once it has loaded the page. */
    private static void enterDialog() {
        Selenide.switchTo()
                .defaultContent();
        Selenide.switchTo()
                .frame(Selenide.$("iframe[title='Action']")
                               .shouldBe(Condition.visible, WAIT));
    }

    private static void leaveDialog() {
        Selenide.switchTo()
                .defaultContent();
    }

    /** The flags listed, as "field REASON", sorted. */
    private static List<String> flags() {
        List<String> flags = new ArrayList<>();
        for (SelenideElement flag : Selenide.$$("[data-capture-flag]")) {
            flags.add(flag.getAttribute("data-flag-field") + " " + flag.getAttribute("data-flag-reason"));
        }
        flags.sort(null);
        return flags;
    }

    /** Waits for the shell's customActions store to have loaded the action with the label. */
    private static void awaitAction(String label) {
        String loaded = "return Alpine.store('customActions').actions.some(a => a.label === '" + label + "');";
        for (int i = 0; i < 60 && !Boolean.TRUE.equals(Selenide.executeJavaScript(loaded)); i++) {
            Selenide.sleep(250);
        }
        assertEquals(Boolean.TRUE, Selenide.executeJavaScript(loaded), "the action [" + label + "] is contributed");
    }

    private static int announcements() {
        return ((Number) Selenide.executeJavaScript("return Alpine.store('notifications').items.length;")).intValue();
    }

    /** Waits for the shell to announce an outcome whose description contains the text. */
    private static void awaitAnnouncement(String text) {
        String descriptions = "";
        for (int i = 0; i < 60; i++) {
            descriptions = Selenide.executeJavaScript("return Alpine.store('notifications').items.map(n => n.description).join(' | ');");
            if (descriptions != null && descriptions.contains(text)) {
                return;
            }
            Selenide.sleep(250);
        }
        fail("expected an announcement containing [" + text + "], got [" + descriptions + "]");
    }

    /** The key of the bill with the vendor number. */
    private String billIdOf(String vendorNumber) {
        AtomicReference<String> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> bills = given().when()
                                                     .get(API + "/bill/BillController")
                                                     .then()
                                                     .statusCode(200)
                                                     .extract()
                                                     .jsonPath()
                                                     .getList("$");
            Map<String, Object> bill = bills.stream()
                                            .filter(row -> vendorNumber.equals(row.get("VendorNumber")))
                                            .findFirst()
                                            .orElseThrow(() -> new AssertionError("no bill with vendor number " + vendorNumber));
            id.set(String.valueOf(bill.get("Id")));
        }, TIMEOUT_SECONDS);
        return id.get();
    }

    /** The key of the file kept on the bill. */
    private int scanIdOf(String billId) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().when()
                                                        .get(SCANS + "?Bill=" + billId)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .jsonPath()
                                                        .getInt("[0].Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    /**
     * A PNG carrying one QR code, saved as bill.png - the part of a fiscal receipt read without a
     * model.
     */
    private static File scanFile() {
        try {
            BufferedImage image = MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(QR_TEXT, BarcodeFormat.QR_CODE, 400, 400));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            File file = new File(Files.createTempDirectory("capture-it")
                                      .toFile(),
                    "bill.png");
            Files.write(file.toPath(), out.toByteArray());
            return file;
        } catch (WriterException e) {
            throw new IllegalStateException("cannot render the QR fixture", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void create(String controller, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(controller)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
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

    private void writeProjectFile(String fileName, String content) {
        String path = PROJECT_PATH + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        if (provider != null) {
            provider.stop(0);
        }
        Configuration.remove("DIRIGIBLE_CAPTURE_API_KEY");
        Configuration.remove("DIRIGIBLE_CAPTURE_BASE_URL");
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
