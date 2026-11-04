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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.response.Response;

/**
 * The intent {@code exports:} construct (#7855), compiled and run: a positional file in a legacy
 * encoding over an entity, an XML document over a report, and a delimited file produced through the
 * generated bean from hand-written code - every run recorded on the ledger entity with its stored
 * file and checksum, a refused parameter recording nothing, and a run the data cannot be written
 * for recorded as FAILED with the reason.
 *
 * <p>
 * The files are compared byte for byte: an exacting format is exactly what a near miss (a shifted
 * column, a replaced character, a lost trailer total) gets wrong while every step reports success.
 */
class IntentExportIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "exportsdemo";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String EXPORTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String LEDGER = API + "/exportrun/ExportRunController";
    private static final String REPORT = "/services/java/" + PROJECT + "/gen/purchasesbysupplier/api/reports/PurchasesBySupplierController";
    private static final Charset WINDOWS_1251 = Charset.forName("windows-1251");

    private static final String INTENT_YAML = """
            name: exportsdemo
            description: declarative export formats over a query

            entities:
              - name: Supplier
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 100 }
                  - { name: vatNumber, type: string, length: 20 }
              - name: PurchaseInvoice
                fields:
                  - { name: id,           type: integer, primaryKey: true, generated: true }
                  - { name: number,       type: string, length: 20 }
                  - { name: documentDate, type: date }
                  - { name: taxBase,      type: decimal, precision: 18, scale: 2 }
                  - { name: note,         type: string, length: 200 }
                relations:
                  - { name: supplier, kind: manyToOne, to: Supplier }
              - name: ExportRun

            reports:
              - name: PurchasesBySupplier
                source: PurchaseInvoice
                dimensions: [supplier.name]
                measures: [sum(taxBase), count(*)]
                parameters:
                  - { name: fromDate, target: documentDate, op: ge }
                  - { name: fromId,   target: id, op: ge, initial: "0" }

            exports:
              - name: VatPurchaseLedger
                source: PurchaseInvoice
                format: fixedWidth
                encoding: windows-1251
                ledger: ExportRun
                fileName: "VAT-{fromDate}.txt"
                orderBy: [documentDate, number]
                parameters:
                  - { name: fromDate, field: documentDate, op: ge, required: true }
                  - { name: toDate,   field: documentDate, op: le }
                records:
                  - type: header
                    fields:
                      - { literal: 'H', width: 1 }
                      - { param: fromDate, width: 8, pattern: ddMMyyyy }
                  - type: line
                    fields:
                      - { from: supplier.vatNumber, width: 12 }
                      - { from: supplier.name, width: 10, truncate: true }
                      - { from: number, width: 6 }
                      - { from: documentDate, width: 8, pattern: ddMMyyyy }
                      - { from: taxBase, width: 10, scale: 2, align: right, pad: '0' }
                  - type: trailer
                    fields:
                      - { literal: 'T', width: 1 }
                      - { from: count(), width: 4, align: right, pad: '0' }
                      - { from: sum(taxBase), width: 12, scale: 2, align: right }
              - name: SupplierPurchases
                source: PurchasesBySupplier
                format: xml
                ledger: ExportRun
                root: Purchases
                parameters:
                  - { name: fromDate }
                  - { name: fromId }
                records:
                  - type: header
                    element: Period
                    fields:
                      - { param: fromDate, name: from, attribute: true }
                  - type: line
                    element: Supplier
                    fields:
                      - { from: supplier.name, name: Name, attribute: true }
                      - { from: sum(taxBase), name: TaxBase, scale: 2 }
                      - { from: count(*), name: Invoices }
                  - type: trailer
                    element: Total
                    fields:
                      - { from: sum(sum(taxBase)), name: TaxBase, scale: 2 }
              - name: InvoiceNotes
                source: PurchaseInvoice
                format: delimited
                delimiter: ';'
                lineEnding: lf
                ledger: ExportRun
                records:
                  - type: header
                    fields: [{ literal: Number }, { literal: Note }]
                  - type: line
                    fields: [{ from: number }, { from: note }]
              # A regulator's field that accepts uppercase Cyrillic, digits and hyphens only (#7925).
              - name: SupplierDeclaration
                source: Supplier
                format: delimited
                delimiter: ';'
                lineEnding: lf
                ledger: ExportRun
                parameters:
                  - { name: vat, field: vatNumber, op: eq }
                records:
                  - type: line
                    fields:
                      - { from: vatNumber }
                      - from: name
                        replace: { "№": "" }
                        case: upper
                        allowed: "А-ЯЁ0-9 -"
                        onDisallowed: strip
                        collapseSpaces: true
                        onLoss: fail
            """;

    /** Hand-written code calling the generated export bean - the job-callable half of the construct. */
    private static final String EXPORT_RUNNER = """
            package custom;

            import java.util.Map;

            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.http.Controller;
            import org.eclipse.dirigible.sdk.http.Get;

            import gen.events.exportsdemo.InvoiceNotesExport;

            @Controller
            @Component
            public class ExportRunner {

                private final InvoiceNotesExport export;

                public ExportRunner(InvoiceNotesExport export) {
                    this.export = export;
                }

                @Get("/notes")
                public String notes() {
                    return String.valueOf(export.run(Map.of()).Id);
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void every_declared_format_is_written_exactly_and_every_run_is_recorded() {
        generateProject();
        writeProjectFile("custom/ExportRunner.java", EXPORT_RUNNER);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int alpha = create(API + "/supplier/SupplierController", "{\"Name\":\"Фирма Алфа ООД\",\"VatNumber\":\"BG111111111\"}");
        int beta = create(API + "/supplier/SupplierController", "{\"Name\":\"Beta\",\"VatNumber\":\"BG222\"}");
        create(API + "/purchaseinvoice/PurchaseInvoiceController",
                "{\"Number\":\"INV1\",\"DocumentDate\":\"2026-03-05\",\"TaxBase\":1234.50,\"Note\":\"plain\",\"Supplier\":" + alpha + "}");
        create(API + "/purchaseinvoice/PurchaseInvoiceController",
                "{\"Number\":\"INV2\",\"DocumentDate\":\"2026-03-20\",\"TaxBase\":100.00,\"Note\":\"semi; colon\",\"Supplier\":" + beta
                        + "}");
        create(API + "/purchaseinvoice/PurchaseInvoiceController",
                "{\"Number\":\"INV0\",\"DocumentDate\":\"2026-02-15\",\"TaxBase\":999.99,\"Supplier\":" + beta + "}");

        // A positional file in windows-1251: the hop-loaded supplier columns, a Cyrillic name cut to its
        // width, zero-padded amounts, and the trailer's count and total over the selected rows only.
        Response vat = download(EXPORTS + "/VatPurchaseLedgerExport/download?fromDate=2026-03-01", 200);
        String expectedVat = "H01032026\r\n" + "BG111111111 Фирма АлфаINV1  050320260001234.50\r\n"
                + "BG222       Beta      INV2  200320260000100.00\r\n" + "T0002     1334.50\r\n";
        assertArrayEquals(expectedVat.getBytes(WINDOWS_1251), vat.asByteArray());
        assertTrue(vat.getHeader("Content-Disposition")
                      .contains("VAT-2026-03-01.txt"),
                vat.getHeader("Content-Disposition"));
        assertLedgerRun(vat, "VatPurchaseLedger", 2);

        // An XML document over the report, its parameter passed through to the report's own filter.
        Response xml = download(EXPORTS + "/SupplierPurchasesExport/download?fromDate=2026-03-15", 200);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Purchases>\n  <Period from=\"2026-03-15\"></Period>\n"
                + "  <Supplier Name=\"Beta\">\n    <TaxBase>100.00</TaxBase>\n    <Invoices>1</Invoices>\n  </Supplier>\n"
                + "  <Total>\n    <TaxBase>100.00</TaxBase>\n  </Total>\n</Purchases>\n";
        assertEquals(expectedXml, new String(xml.asByteArray(), StandardCharsets.UTF_8));
        assertLedgerRun(xml, "SupplierPurchases", 1);

        // The bean, called from hand-written code: the run is recorded and its stored file is the ledger
        // row's own download.
        AtomicReference<String> runId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> runId.set(given().when()
                                                           .get("/services/java/" + PROJECT + "/custom/ExportRunner/notes")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .asString()),
                90);
        byte[] notes = download(LEDGER + "/" + runId.get() + "/download", 200).asByteArray();
        assertEquals("Number;Note\nINV1;plain\nINV2;\"semi; colon\"\nINV0;\n", new String(notes, StandardCharsets.UTF_8));
        Map<String, Object> notesRun = ledgerRun(Integer.parseInt(runId.get()));
        assertEquals(sha256(notes), notesRun.get("Checksum"));
        assertEquals("SUCCEEDED", notesRun.get("Outcome"));

        // A name normalised for a narrow character set: replaced, uppercased, its punctuation stripped and
        // its spaces collapsed - and the run records the name as the data holds it beside the file's.
        create(API + "/supplier/SupplierController", "{\"Name\":\"Гама-Трейд, ЕООД / клон №2\",\"VatNumber\":\"BG333\"}");
        Response declaration = download(EXPORTS + "/SupplierDeclarationExport/download?vat=BG333", 200);
        assertEquals("BG333;ГАМА-ТРЕЙД ЕООД КЛОН 2\n", new String(declaration.asByteArray(), StandardCharsets.UTF_8));
        String normalised = String.valueOf(assertLedgerRun(declaration, "SupplierDeclaration", 1).get("Normalised"));
        assertTrue(normalised.startsWith("record 1 [line] field [")
                && normalised.endsWith("]: [Гама-Трейд, ЕООД / клон №2] -> [ГАМА-ТРЕЙД ЕООД КЛОН 2]"), normalised);

        // A Latin name has no letter the field allows: written, it would be a value nobody typed, so the
        // run is refused naming the record and recorded FAILED.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EXPORTS + "/SupplierDeclarationExport/download?vat=BG222")
                                                 .then()
                                                 .statusCode(422)
                                                 .body(containsString("record 1 [line]"))
                                                 .body(containsString("[Beta] would be written as []")),
                90);
        List<Map<String, Object>> declarationRuns = ledgerRuns();
        Map<String, Object> refusedDeclaration = declarationRuns.get(declarationRuns.size() - 1);
        assertEquals("SupplierDeclaration", refusedDeclaration.get("Export"));
        assertEquals("FAILED", refusedDeclaration.get("Outcome"));

        // A required parameter left out is refused before anything runs, and records nothing.
        int recorded = ledgerRuns().size();
        download(EXPORTS + "/VatPurchaseLedgerExport/download", 400);
        assertEquals(recorded, ledgerRuns().size(), "a refused parameter must not record a run");

        // A value wider than its positional field fails the run, which is recorded with the reason.
        create(API + "/purchaseinvoice/PurchaseInvoiceController",
                "{\"Number\":\"INVOICE-LONG\",\"DocumentDate\":\"2026-04-01\",\"TaxBase\":1,\"Supplier\":" + beta + "}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EXPORTS + "/VatPurchaseLedgerExport/download?fromDate=2026-04-01")
                                                 .then()
                                                 .statusCode(422)
                                                 .body(containsString("wider than its 6")),
                90);
        List<Map<String, Object>> runs = ledgerRuns();
        Map<String, Object> failed = runs.get(runs.size() - 1);
        assertEquals("FAILED", failed.get("Outcome"));
        assertTrue(String.valueOf(failed.get("ErrorMessage"))
                         .contains("[INVOICE-LONG] is 12 characters, wider than its 6"),
                String.valueOf(failed.get("ErrorMessage")));
        assertNull(failed.get("StoragePath"), "a failed run stores no file");

        // A report parameter is read as its type before the report runs: a value that is not one, or an
        // integer beyond its type, is a 400 naming the parameter and the type - on the report and on the
        // export over it, which then records nothing (#7828, #8098).
        assertAnswered(REPORT + "?fromDate=abc", null, 400, "Report parameter [fromDate] is not a valid DATE: abc");
        assertAnswered(REPORT + "?fromId=99999999999", null, 400, "Report parameter [fromId] is not a valid INTEGER: 99999999999");
        assertAnswered(REPORT + "/count?fromId=abc", null, 400, "Report parameter [fromId] is not a valid INTEGER: abc");
        int beforeRefusedRuns = ledgerRuns().size();
        assertAnswered(EXPORTS + "/SupplierPurchasesExport/download?fromId=abc", null, 400,
                "Report parameter [fromId] is not a valid INTEGER: abc");
        assertAnswered(EXPORTS + "/SupplierPurchasesExport/download?fromDate=2026-13-45", null, 400,
                "Report parameter [fromDate] is not a valid DATE: 2026-13-45");
        assertEquals(beforeRefusedRuns, ledgerRuns().size(), "a parameter refused before the run must not record one");
        // A negative page bound is ignored, as the entity controllers ignore it (#7828).
        assertEquals(2, reportRows(REPORT + "?$limit=-1&$offset=-1", null).size(), "a negative $limit/$offset is ignored");

        // A /search condition is read as its column's type: a word in a decimal, a pattern on a number
        // and an integer beyond its type are 400s naming the column and the value (#8177) ...
        assertAnswered(REPORT + "/search", conditions("Sum Tax Base", "GT", "abc"), 400,
                "Report filter value of column [Sum Tax Base] is not a valid DECIMAL: abc");
        assertAnswered(REPORT + "/search", conditions("Sum Tax Base", "LIKE", "1%"), 400,
                "Report filter operator LIKE does not apply to column [Sum Tax Base]");
        assertAnswered(REPORT + "/search", conditions("Count", "GT", "1e99"), 400, "Report filter value of column [Count]");
        // ... while a condition of the right type still filters: Beta's three invoices, Alfa's one.
        assertEquals(1, reportRows(REPORT + "/search", conditions("Count", "GTE", "2")).size(), "a typed condition filters");

        // The ledger is written by export runs only: a run nobody made is refused over REST, and a FAILED
        // run, which stored no file, has none to serve (#8098).
        int beforeCreateRuns = ledgerRuns().size();
        assertAnswered(LEDGER, "{\"Export\":\"SupplierPurchases\",\"Outcome\":\"SUCCEEDED\",\"RecordCount\":5}", 405,
                "cannot be created over REST");
        assertEquals(beforeCreateRuns, ledgerRuns().size(), "a ledger row made over REST must not be stored");
        assertAnswered(LEDGER + "/" + failed.get("Id") + "/download", null, 404, "has no stored file");
    }

    /** A GET (no body) or a JSON POST answered with the status and a reason containing the text. */
    private void assertAnswered(String url, String body, int status, String reason) {
        restAssuredExecutor.execute(() -> request(url, body).then()
                                                            .statusCode(status)
                                                            .body(containsString(reason)),
                90);
    }

    private List<Map<String, Object>> reportRows(String url, String body) {
        AtomicReference<List<Map<String, Object>>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(request(url, body).then()
                                                                     .statusCode(200)
                                                                     .extract()
                                                                     .jsonPath()
                                                                     .getList("$")),
                90);
        return rows.get();
    }

    private static Response request(String url, String body) {
        return body == null ? given().when()
                                     .get(url)
                : given().contentType("application/json")
                         .body(body)
                         .when()
                         .post(url);
    }

    private static String conditions(String column, String operator, String value) {
        return "{\"conditions\":[{\"column\":\"" + column + "\",\"operator\":\"" + operator + "\",\"value\":\"" + value + "\"}]}";
    }

    private Map<String, Object> assertLedgerRun(Response download, String export, int rows) {
        Map<String, Object> run = ledgerRun(Integer.parseInt(download.getHeader("X-Export-Run")));
        assertEquals(export, run.get("Export"));
        assertEquals("SUCCEEDED", run.get("Outcome"));
        assertEquals(rows, ((Number) run.get("RecordCount")).intValue());
        assertEquals(sha256(download.asByteArray()), run.get("Checksum"));
        assertEquals(download.getHeader("X-Export-Checksum"), run.get("Checksum"));
        // The ledger row serves the very bytes the run answered.
        assertArrayEquals(download.asByteArray(), download(LEDGER + "/" + run.get("Id") + "/download", 200).asByteArray());
        return run;
    }

    private Map<String, Object> ledgerRun(int id) {
        AtomicReference<Map<String, Object>> run = new AtomicReference<>();
        restAssuredExecutor.execute(() -> run.set(given().when()
                                                         .get(LEDGER + "/" + id)
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .getMap("$")),
                90);
        return run.get();
    }

    private List<Map<String, Object>> ledgerRuns() {
        AtomicReference<List<Map<String, Object>>> runs = new AtomicReference<>();
        restAssuredExecutor.execute(() -> runs.set(given().when()
                                                          .get(LEDGER)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("$")),
                90);
        return runs.get();
    }

    private Response download(String url, int status) {
        AtomicReference<Response> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(url)
                                                              .then()
                                                              .statusCode(status)
                                                              .extract()
                                                              .response()),
                90);
        return response.get();
    }

    private int create(String url, String body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json; charset=UTF-8")
                                                        .body(body.getBytes(StandardCharsets.UTF_8))
                                                        .when()
                                                        .post(url)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                90);
        return id.get();
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256")
                                                    .digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void generateProject() {
        writeProjectFile("app.intent", INTENT_YAML);
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

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
    }
}
