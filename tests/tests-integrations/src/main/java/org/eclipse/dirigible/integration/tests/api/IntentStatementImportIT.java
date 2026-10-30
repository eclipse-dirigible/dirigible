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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@code imports:} construct end to end (#7856): an intent declaring a CSV import and an MT940
 * import of bank statement lines is generated, compiled and published, and the real sample files
 * are uploaded to the generated endpoints.
 *
 * <p>
 * The CSV is the issue's acceptance file: windows-1251, a {@code ;} separator, {@code 1.234,56}
 * amounts, a bank preamble line and a quoted field holding the separator. Uploading either file
 * again is refused as a duplicate on the declared key and saves nothing; every attempt lands in the
 * entity's import ledger; and a {@code Hook} bean in the project's {@code custom/} code sees the
 * rows of the CSV import after they are committed.
 */
class IntentStatementImportIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "bankimport";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String CSV_UPLOAD = EVENTS + "/BankCsvImport/upload";
    private static final String MT940_UPLOAD = EVENTS + "/SwiftImport/upload";
    private static final String LINES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/statementline/StatementLineController";
    private static final String LEDGER = "BANKIMPORT_STATEMENT_LINE_IMPORTS";

    private static final String INTENT_YAML = """
            name: bankimport
            description: bank statement lines imported from CSV and MT940 files

            entities:
              - name: StatementLine
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: statementId, type: string,  length: 100, required: true }
                  - { name: lineNumber,  type: integer, required: true }
                  - { name: bookedOn,    type: date }
                  - { name: amount,      type: decimal, precision: 18, scale: 2 }
                  - { name: details,     type: string,  length: 500 }
                  - { name: matched,     type: boolean, defaultValue: "false" }

            imports:
              - name: bankCsv
                into: StatementLine
                format: csv
                encoding: windows-1251
                csv: { separator: ";", decimal: ",", grouping: ".", dateFormat: dd.MM.yyyy, skipLines: 1 }
                map:
                  statementId: _file
                  lineNumber:  _line
                  bookedOn:    Дата
                  amount:      Сума
                  details:     Основание
                duplicateKey: [statementId, lineNumber]
              - name: swift
                into: StatementLine
                format: mt940
                map: { statementId: statementId, lineNumber: lineNumber, bookedOn: valueDate, amount: amount, details: details }
                duplicateKey: [statementId, lineNumber]
            """;

    /** A matching rule: the post-import hook marks every row of a CSV import as matched. */
    private static final String MATCHING_HOOK_JAVA = """
            package custom;

            import java.util.List;

            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.imports.ImportResult;

            import gen.bankimport.data.statementline.StatementLineEntity;
            import gen.bankimport.data.statementline.StatementLineRepository;
            import gen.events.bankimport.BankCsvImport;

            @Component
            public class MatchingHook implements BankCsvImport.Hook {

                @Override
                public void afterImport(ImportResult result, List<StatementLineEntity> rows) {
                    StatementLineRepository repository = new StatementLineRepository();
                    for (StatementLineEntity row : rows) {
                        row.Matched = Boolean.TRUE;
                        repository.update(row);
                    }
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_statement_file_imports_once_into_rows_and_a_second_upload_is_refused_as_a_duplicate() throws SQLException {
        deployBankimport();

        byte[] csv = fixture("bank-statement-bg.csv");
        restAssuredExecutor.execute(() -> given().multiPart("file", "bank-statement-bg.csv", csv, "text/csv")
                                                 .when()
                                                 .post(CSV_UPLOAD)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("status", equalTo("IMPORTED"))
                                                 .body("records", equalTo(3)),
                90);

        List<Map<String, Object>> csvRows = lines("bank-statement-bg.csv");
        assertEquals(3, csvRows.size(), "every record of the CSV is a row: " + csvRows);
        assertLine(csvRows.get(0), 1, "2026-03-15", "1234.56", "Плащане по фактура 1001");
        assertLine(csvRows.get(1), 2, "2026-03-16", "-12.50", "Такса; обслужване");
        assertLine(csvRows.get(2), 3, "2026-03-31", "1000000.01", "Лихва за март");
        for (Map<String, Object> row : csvRows) {
            assertEquals(Boolean.TRUE, row.get("Matched"), "the post-import hook saw every committed row: " + row);
        }

        restAssuredExecutor.execute(() -> given().multiPart("file", "bank-statement-bg.csv", csv, "text/csv")
                                                 .when()
                                                 .post(CSV_UPLOAD)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("status", equalTo("DUPLICATE"))
                                                 .body("message", containsString("was already imported")));
        assertEquals(3, lines("bank-statement-bg.csv").size(), "a refused upload saves nothing");

        byte[] mt940 = fixture("statement.mt940");
        restAssuredExecutor.execute(() -> given().body(mt940)
                                                 .contentType("application/octet-stream")
                                                 .when()
                                                 .post(MT940_UPLOAD + "?fileName=statement.mt940")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("status", equalTo("IMPORTED"))
                                                 .body("records", equalTo(3)));
        List<Map<String, Object>> march = lines("STMT-2026-03");
        assertEquals(2, march.size(), "the :61: lines of the March statement: " + march);
        assertLine(march.get(0), 1, "2026-03-15", "1234.56", "Плащане по фактура 1001\nACME OOD");
        assertLine(march.get(1), 2, "2026-03-16", "-12.50", "Такса обслужване");
        assertNotEquals(Boolean.TRUE, march.get(0)
                                           .get("Matched"),
                "a hook of the CSV import is not one of the MT940 import");
        List<Map<String, Object>> april = lines("STMT-2026-04");
        assertEquals(1, april.size());
        assertEquals(0, new BigDecimal("-100.00").compareTo(new BigDecimal(String.valueOf(april.get(0)
                                                                                               .get("Amount")))),
                "a reversed credit is a debit: " + april.get(0));

        restAssuredExecutor.execute(() -> given().body(mt940)
                                                 .contentType("application/octet-stream")
                                                 .when()
                                                 .post(MT940_UPLOAD + "?fileName=statement-again.mt940")
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message", containsString("statementId [STMT-2026-03], lineNumber [1]")));

        restAssuredExecutor.execute(() -> given().body("not a statement".getBytes(StandardCharsets.UTF_8))
                                                 .contentType("application/octet-stream")
                                                 .when()
                                                 .post(MT940_UPLOAD + "?fileName=junk.txt")
                                                 .then()
                                                 .statusCode(400)
                                                 .body("status", equalTo("REJECTED")));

        assertEquals(
                List.of("bankCsv IMPORTED 3 bank-statement-bg.csv", "bankCsv DUPLICATE 0 bank-statement-bg.csv",
                        "swift IMPORTED 3 statement.mt940", "swift DUPLICATE 0 statement-again.mt940", "swift REJECTED 0 junk.txt"),
                ledger());
    }

    /**
     * Four simultaneous uploads of the same file - a double-click on Upload, a retrying client - must
     * import it exactly once: the target carries a unique constraint on the declared duplicate key, so
     * only one upload commits its rows and the rest are refused as duplicates, whatever the timing
     * (#8190). Without the constraint each upload's check-then-insert passed and every line was stored
     * as many times as there were overlapping requests.
     */
    @Test
    void concurrent_uploads_of_one_file_import_it_exactly_once() {
        deployBankimport();

        byte[] csv = fixture("bank-statement-bg.csv");
        int attempts = 4;
        List<Integer> statuses = Collections.synchronizedList(new ArrayList<>(attempts));

        restAssuredExecutor.execute(() -> uploadConcurrently(csv, attempts, statuses));

        long imported = statuses.stream()
                                .filter(code -> code == 200)
                                .count();
        long duplicate = statuses.stream()
                                 .filter(code -> code == 409)
                                 .count();
        assertEquals(attempts, statuses.size(), "every concurrent upload returned: " + statuses);
        assertEquals(1, imported, "exactly one concurrent upload imports the file: " + statuses);
        assertEquals(attempts - 1, duplicate, "every other concurrent upload is a duplicate: " + statuses);
        assertEquals(3, lines("bank-statement-bg.csv").size(), "the file's lines are stored exactly once");
    }

    /** Releases {@code attempts} uploads of the same file together and collects their status codes. */
    private static void uploadConcurrently(byte[] csv, int attempts, List<Integer> statuses) {
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(attempts);
        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    statuses.add(given().multiPart("file", "bank-statement-bg.csv", csv, "text/csv")
                                        .when()
                                        .post(CSV_UPLOAD)
                                        .getStatusCode());
                    return null;
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "the uploads parked at the barrier");
            go.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("interrupted while uploading concurrently", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("a concurrent upload did not complete", e);
        } finally {
            pool.shutdownNow();
        }
    }

    private void deployBankimport() {
        writeProjectFile("app.intent", INTENT_YAML);
        writeProjectFile("custom/MatchingHook.java", MATCHING_HOOK_JAVA);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void assertLine(Map<String, Object> row, int lineNumber, String bookedOn, String amount, String details) {
        assertEquals(lineNumber, ((Number) row.get("LineNumber")).intValue(), "line number of " + row);
        assertEquals(bookedOn, row.get("BookedOn"), "booking date of " + row);
        assertEquals(0, new BigDecimal(amount).compareTo(new BigDecimal(String.valueOf(row.get("Amount")))), "amount of " + row);
        assertEquals(details, row.get("Details"), "details of " + row);
    }

    /** The rows of one statement, by line number. */
    private List<Map<String, Object>> lines(String statementId) {
        AtomicReference<List<Map<String, Object>>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(given().when()
                                                          .get(LINES)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .using(new JsonPathConfig(NumberReturnType.BIG_DECIMAL))
                                                          .getList("$")));
        List<Map<String, Object>> statement = new ArrayList<>();
        for (Map<String, Object> row : rows.get()) {
            if (statementId.equals(row.get("StatementId"))) {
                statement.add(row);
            }
        }
        statement.sort(Comparator.comparingInt(row -> ((Number) row.get("LineNumber")).intValue()));
        return statement;
    }

    private List<String> ledger() throws SQLException {
        List<String> entries = new ArrayList<>();
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery("SELECT \"ImportName\", \"Status\", \"Records\", \"FileName\", \"Checksum\" FROM \"" + LEDGER
                                + "\" ORDER BY \"Id\"")) {
            while (result.next()) {
                assertEquals(64, result.getString("Checksum")
                                       .length(),
                        "every attempt records the file's checksum");
                entries.add(result.getString("ImportName") + " " + result.getString("Status") + " " + result.getInt("Records") + " "
                        + result.getString("FileName"));
            }
        }
        return entries;
    }

    private static byte[] fixture(String name) {
        try (InputStream content = IntentStatementImportIT.class.getResourceAsStream("/IntentStatementImportIT/" + name)) {
            assertTrue(content != null, "missing fixture " + name);
            return content.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        assertTrue(repository.hasResource(PROJECT_PATH + "/gen/events/" + PROJECT + "/BankCsvImport.java"),
                "the CSV import class is generated");
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
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
