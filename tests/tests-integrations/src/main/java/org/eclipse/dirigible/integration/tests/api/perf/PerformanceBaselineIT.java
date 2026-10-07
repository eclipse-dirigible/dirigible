/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.perf;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * A performance baseline for the generated surface (#7646): until it, no test measured time, and an
 * N+1 in a template was found by reading code (#7526) or by a user.
 *
 * <p>
 * The {@code PerformanceBaselineIT/app.intent} application is generated and published on a
 * PostgreSQL 16 of the test's own (Testcontainers), filled with 10 000 orders, 100 000 order lines
 * and 1 000 open tasks, and then every step below is run 50 times by 10 concurrent users, after a
 * warm-up of its own:
 * <ul>
 * <li>{@code listPage} - page 1 of the order list ({@code $limit}/{@code $offset}, the generated
 * list's paging; it has no sort parameter);</li>
 * <li>{@code search} - the order list filtered by customer ({@code POST .../search}, the generated
 * filter, which pages nothing);</li>
 * <li>{@code create} - an order create, three checks reading the same customer hop and a roll-up
 * fed;</li>
 * <li>{@code transition} - an order moved to Confirmed through its generated transition endpoint,
 * which fires a posting (a plain {@code PUT} of the status is refused with a 409: the status is the
 * transition's column);</li>
 * <li>{@code print} - the order's standard {@code .print} template rendered to PDF;</li>
 * <li>{@code inbox} - {@code GET /services/inbox/tasks?type=groups} over 1 000 open tasks.</li>
 * </ul>
 * Each step reports its p50 / p95 latency, the statements a typical request of it executes - the
 * five most repeated named, so an N+1 is named rather than guessed - and the peak use of each
 * connection pool. The statements counted are the ones the request's own thread executes
 * ({@link StatementCountingDriver}): the posting and the roll-up recompute run after the commit, on
 * a listener's thread, and are not part of the request they follow.
 *
 * <p>
 * The numbers are gated against {@code tests/PERF_BASELINE.json} ({@link PerformanceBaseline});
 * with {@code -Dperf.baseline.record=true} they are written there instead, which is what the
 * release workflow does. The file also records the platform defaults the run measured with: each
 * data source's Hikari pool size, the Tomcat thread pool and the JVM heap.
 *
 * <p>
 * Tagged {@code perf}: it needs Docker and a quiet machine, so it runs in a job of its own in the
 * nightly and the release workflows, never in the shards or the PR gate.
 */
@Tag("perf")
@Import(PerformanceBaselineITConfig.class)
class PerformanceBaselineIT extends IntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceBaselineIT.class);

    private static final String DATABASE = "testdb";
    private static final String SYSTEM_DATABASE = "systemdb";
    private static final String DATABASE_USER = "testuser";
    private static final String DATABASE_PASSWORD = "testpass";

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "perf-baseline";
    private static final String INTENT = "app.intent";

    private static final String API = "/services/java/" + PROJECT + "/gen/perf/api";
    private static final String ORDERS = API + "/salesorder/SalesOrderController";
    private static final String JOURNAL = API + "/journalentry/JournalEntryController";
    private static final String APPROVALS = API + "/approval/ApprovalController";
    private static final String CONFIRM = "/services/java/" + PROJECT + "/gen/events/perf/ConfirmOrderTransition/run";
    private static final String PRINT = "/services/print/SalesOrder";
    private static final String INBOX = "/services/inbox/tasks?type=groups";

    private static final int CUSTOMERS = 1_000;
    private static final int ORDERS_SEEDED = 10_000;
    private static final int LINES_PER_ORDER = 10;
    private static final int OPEN_TASKS = 1_000;

    /** The concurrent users. */
    private static final int USERS = 10;
    private static final int ITERATIONS = 50;
    private static final int WARM_UP_ITERATIONS = 10;

    private static final LocalDate FIRST_ORDER_DATE = LocalDate.of(2026, 1, 1);
    private static final Duration PUBLISH_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration TASKS_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration POSTING_TIMEOUT = Duration.ofMinutes(2);

    /** Writes the measurements as the new baseline instead of gating them. */
    private static final boolean RECORD = Boolean.getBoolean("perf.baseline.record");
    /** Relative to the module, where failsafe runs: {@code tests/PERF_BASELINE.json}. */
    private static final Path BASELINE = Path.of(System.getProperty("perf.baseline.file", "../PERF_BASELINE.json"));

    private static final Gson GSON = new Gson();

    private static GenericContainer<?> postgres;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    @Qualifier("SystemDB")
    private DataSource systemDataSource;

    @Autowired
    private Environment environment;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** Runs before the application context starts, which then boots on this database. */
    @BeforeAll
    @SuppressWarnings("resource")
    static void startPostgreSql() throws IOException, InterruptedException {
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:16")).withExposedPorts(5432)
                                                                               .withEnv(Map.of("POSTGRES_DB", DATABASE, "POSTGRES_USER",
                                                                                       DATABASE_USER, "POSTGRES_PASSWORD",
                                                                                       DATABASE_PASSWORD))
                                                                               .waitingFor(Wait.forLogMessage(
                                                                                       ".*database system is ready to accept connections.*\\s",
                                                                                       2));
        postgres.start();
        ExecResult created =
                postgres.execInContainer("psql", "-U", DATABASE_USER, "-d", DATABASE, "-c", "CREATE DATABASE " + SYSTEM_DATABASE);
        assertEquals(0, created.getExitCode(), "creating the SystemDB failed: " + created.getStderr());

        String host = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/";
        // Runtime values win over the environment, so this holds on a machine that exports its own.
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER", StatementCountingDriver.class.getName());
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_URL", host + DATABASE);
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_USERNAME", DATABASE_USER);
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_PASSWORD", DATABASE_PASSWORD);
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_DRIVER", StatementCountingDriver.class.getName());
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_URL", host + SYSTEM_DATABASE);
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_USERNAME", DATABASE_USER);
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", DATABASE_PASSWORD);
    }

    @AfterAll
    static void stopPostgreSql() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    void theGeneratedSurfaceStaysWithinItsBaseline() throws Exception {
        deployApplication();
        SeededIds seeded = seed();
        parkOpenTasks();
        awaitPrintTemplate();

        AtomicInteger created = new AtomicInteger();
        AtomicInteger transitioned = new AtomicInteger();
        AtomicInteger searched = new AtomicInteger();
        List<StepMeasurement> measurements = new ArrayList<>();
        measurements.add(measure("listPage", i -> get(ORDERS + "?$limit=20&$offset=0")));
        measurements.add(measure("search", i -> post(ORDERS + "/search",
                Map.of("equals", Map.of("Customer", seeded.firstCustomer() + searched.getAndIncrement() % CUSTOMERS)))));
        measurements.add(measure("create", i -> post(ORDERS, newOrder(seeded, created.getAndIncrement()))));
        measurements.add(measure("transition", i -> confirm(seeded, transitioned.getAndIncrement())));
        measurements.add(measure("print", i -> post(PRINT + "?lang=en", printPayload(seeded))));
        measurements.add(measure("inbox", i -> get(INBOX)));

        awaitPostings(transitioned.get());
        JsonObject platformDefaults = platformDefaults();
        report(measurements, platformDefaults);

        if (RECORD) {
            PerformanceBaseline.write(BASELINE, measurements, platformDefaults);
            LOGGER.info("Recorded the performance baseline into [{}]", BASELINE.toAbsolutePath());
            return;
        }
        PerformanceBaseline baseline = PerformanceBaseline.read(BASELINE);
        List<String> regressions = measurements.stream()
                                               .flatMap(measurement -> baseline.regressions(measurement)
                                                                               .stream())
                                               .toList();
        assertThat(regressions).as("steps beyond the baseline in [%s]", BASELINE.toAbsolutePath())
                               .isEmpty();
    }

    private void deployApplication() {
        String projectPath = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
        repository.createResource(projectPath + "/" + INTENT, resource(PerformanceBaselineIT.class.getSimpleName() + "/" + INTENT));
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=" + INTENT)
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
                                                 .get(ORDERS + "?$limit=1")
                                                 .then()
                                                 .statusCode(200),
                PUBLISH_TIMEOUT.toSeconds());
    }

    /** The first identifiers of what {@link #seed()} inserted - each table's are consecutive. */
    private record SeededIds(int firstCustomer, int firstOrder) {
    }

    /**
     * Fills the tables in a few set-based statements rather than through the API, which would take
     * hours for the lines alone: the measurement is of the reads and writes that follow.
     */
    private SeededIds seed() throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO "PERF_CUSTOMER" ("CUSTOMER_NAME", "CUSTOMER_EMAIL", "CUSTOMER_PHONE", "CUSTOMER_ORDER_COUNT")
                    SELECT 'Customer ' || g, 'customer' || g || '@example.com', '+359 2 ' || g, 0
                    FROM generate_series(1, %d) g""".formatted(CUSTOMERS));
            int firstCustomer = firstId(statement, "SELECT MIN(\"CUSTOMER_ID\") FROM \"PERF_CUSTOMER\"");
            statement.executeUpdate("""
                    INSERT INTO "PERF_SALES_ORDER" ("SALES_ORDER_REFERENCE", "SALES_ORDER_SENT_METHOD", "SALES_ORDER_ORDER_DATE",
                        "SALES_ORDER_AMOUNT", "SALES_ORDER_CUSTOMER", "SALES_ORDER_STATUS")
                    SELECT 'SO-' || g, 1, DATE '%s' + (g %% 365), 100 + g, %d + (g - 1) %% %d, 1
                    FROM generate_series(1, %d) g""".formatted(FIRST_ORDER_DATE, firstCustomer, CUSTOMERS, ORDERS_SEEDED));
            int firstOrder = firstId(statement, "SELECT MIN(\"SALES_ORDER_ID\") FROM \"PERF_SALES_ORDER\"");
            statement.executeUpdate("""
                    INSERT INTO "PERF_SALES_ORDER_ITEM" ("SALES_ORDER_ITEM_PRODUCT", "SALES_ORDER_ITEM_QUANTITY", "SALES_ORDER_ITEM_PRICE",
                        "SALES_ORDER_ITEM_SALES_ORDER")
                    SELECT 'Product ' || l, l, 10, o."SALES_ORDER_ID"
                    FROM "PERF_SALES_ORDER" o CROSS JOIN generate_series(1, %d) l""".formatted(LINES_PER_ORDER));
            statement.executeUpdate("""
                    UPDATE "PERF_CUSTOMER" c SET "CUSTOMER_ORDER_COUNT" =
                        (SELECT COUNT(*) FROM "PERF_SALES_ORDER" o WHERE o."SALES_ORDER_CUSTOMER" = c."CUSTOMER_ID")""");
            statement.execute("ANALYZE");
            return new SeededIds(firstCustomer, firstOrder);
        }
    }

    private static int firstId(Statement statement, String query) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(query)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    /** Every approval starts a review parked on a user task addressed to a role the admin holds. */
    private void parkOpenTasks() throws InterruptedException, ExecutionException {
        run(OPEN_TASKS, i -> post(APPROVALS, Map.of("Subject", "Approval " + i)), null);
        restAssuredExecutor.execute(() -> assertThat(given().when()
                                                            .get(INBOX)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getList("$")).as("open tasks in the inbox")
                                                                          .hasSize(OPEN_TASKS),
                TASKS_TIMEOUT.toSeconds());
    }

    /** The template reaches the CMS on a synchronizer pass of its own. */
    private void awaitPrintTemplate() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PRINT + "/languages")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("code", hasItem("en")),
                PUBLISH_TIMEOUT.toSeconds());
    }

    /** Every confirmed order is booked into the journal by the posting its transition fired. */
    private void awaitPostings(int confirmed) {
        restAssuredExecutor.execute(() -> assertThat(given().when()
                                                            .get(JOURNAL + "/count")
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getInt("count")).as("journal entries posted by the transitions")
                                                                             .isEqualTo(confirmed),
                POSTING_TIMEOUT.toSeconds());
    }

    private static Map<String, Object> newOrder(SeededIds seeded, int index) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("Reference", "NEW-" + index);
        order.put("SentMethod", 1);
        order.put("OrderDate", FIRST_ORDER_DATE.toString());
        order.put("Amount", 250);
        order.put("Customer", seeded.firstCustomer() + index % CUSTOMERS);
        return order;
    }

    /** The Confirm button on the seeded order {@code index}: the generated transition endpoint. */
    private HttpRequest confirm(SeededIds seeded, int index) {
        return post(CONFIRM, Map.of("id", seeded.firstOrder() + index));
    }

    /** What the document view hands the print endpoint: the header and its lines, labels resolved. */
    private static Map<String, Object> printPayload(SeededIds seeded) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("Id", seeded.firstOrder());
        document.put("Reference", "SO-1");
        document.put("SentMethod", 1);
        document.put("OrderDate", FIRST_ORDER_DATE.plusDays(1)
                                                  .toString());
        document.put("Amount", 101);
        document.put("Customer", "Customer 1");
        document.put("Status", "Draft");
        List<Map<String, Object>> items = IntStream.rangeClosed(1, LINES_PER_ORDER)
                                                   .mapToObj(line -> Map.<String, Object>of("Product", "Product " + line, "Quantity", line,
                                                           "Price", 10))
                                                   .toList();
        return Map.of("document", document, "items", items);
    }

    /**
     * Runs a step: a warm-up of its own, then {@link #ITERATIONS} requests by {@link #USERS} concurrent
     * users, each request carrying the step's name so its statements are counted.
     */
    private StepMeasurement measure(String step, IntFunction<HttpRequest> request)
            throws InterruptedException, ExecutionException, SQLException {
        run(WARM_UP_ITERATIONS, request, null);
        Map<String, HikariDataSource> pools = pools();
        List<Double> latencies;
        Map<String, StepMeasurement.PoolPeak> peaks;
        try (PoolSampler sampler = new PoolSampler(pools)) {
            latencies = run(ITERATIONS, request, step);
            peaks = sampler.peaks();
        }
        Awaitility.await()
                  .atMost(30, TimeUnit.SECONDS)
                  .until(() -> StatementCountingFilter.recorded(step)
                                                      .size() == ITERATIONS);
        return new StepMeasurement(step, latencies, StatementCountingFilter.recorded(step), peaks);
    }

    /**
     * Sends the requests from {@link #USERS} threads and returns their latencies.
     *
     * @param step the step to count the requests' statements under, null to count nothing
     */
    private List<Double> run(int count, IntFunction<HttpRequest> request, String step) throws InterruptedException, ExecutionException {
        ExecutorService users = Executors.newFixedThreadPool(USERS);
        try {
            List<Future<Double>> requests = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int index = i;
                requests.add(users.submit(() -> send(step == null ? request.apply(index) : withStep(request.apply(index), step))));
            }
            List<Double> latencies = new ArrayList<>(count);
            for (Future<Double> sent : requests) {
                latencies.add(sent.get());
            }
            return latencies;
        } finally {
            users.shutdownNow();
        }
    }

    private double send(HttpRequest request) throws IOException, InterruptedException {
        long start = System.nanoTime();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        if (response.statusCode() / 100 != 2) {
            throw new AssertionError(request.method() + " " + request.uri() + " answered " + response.statusCode() + ": "
                    + new String(response.body(), StandardCharsets.UTF_8));
        }
        return millis;
    }

    private static HttpRequest withStep(HttpRequest request, String step) {
        return HttpRequest.newBuilder(request, (name, value) -> true)
                          .header(StatementCountingFilter.STEP_HEADER, step)
                          .build();
    }

    private HttpRequest get(String path) {
        return request(path).GET()
                            .build();
    }

    private HttpRequest post(String path, Object body) {
        return request(path).POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                            .header("Content-Type", "application/json")
                            .build();
    }

    private HttpRequest.Builder request(String path) {
        DirigibleTestTenant tenant = DirigibleTestTenant.createDefaultTenant();
        String credentials = Base64.getEncoder()
                                   .encodeToString((tenant.getUsername() + ":" + tenant.getPassword()).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                          .header("Authorization", "Basic " + credentials)
                          .timeout(Duration.ofMinutes(2));
    }

    /**
     * The pools a request can wait on: the application's DefaultDB, and the two SystemDB pools - the
     * Spring bean Flowable and the platform's JPA repositories use, and the one the data-source
     * artefacts open beside it.
     */
    private Map<String, HikariDataSource> pools() throws SQLException {
        Map<String, HikariDataSource> pools = new LinkedHashMap<>();
        pools.put("DefaultDB", dataSourcesManager.getDefaultDataSource()
                                                 .unwrap(HikariDataSource.class));
        pools.put("SystemDB", systemDataSource.unwrap(HikariDataSource.class));
        pools.put("SystemDB (artefacts)", dataSourcesManager.getSystemDataSource()
                                                            .unwrap(HikariDataSource.class));
        return pools;
    }

    /** The defaults the measurement ran with - what a deployment that sets nothing gets. */
    private JsonObject platformDefaults() throws SQLException {
        JsonObject defaults = new JsonObject();
        JsonObject poolSizes = new JsonObject();
        pools().forEach((name, pool) -> poolSizes.addProperty(name, pool.getMaximumPoolSize()));
        defaults.add("hikariMaximumPoolSize", poolSizes);
        defaults.addProperty("tomcatMaxThreads", environment.getProperty("server.tomcat.threads.max", Integer.class, 200));
        defaults.addProperty("jvmMaxHeapMb", Runtime.getRuntime()
                                                    .maxMemory()
                / (1024 * 1024));
        defaults.addProperty("availableProcessors", Runtime.getRuntime()
                                                           .availableProcessors());
        defaults.addProperty("javaVersion", Runtime.version()
                                                   .toString());
        defaults.addProperty("concurrentUsers", USERS);
        defaults.addProperty("iterations", ITERATIONS);
        return defaults;
    }

    private static void report(List<StepMeasurement> measurements, JsonObject platformDefaults) {
        StringBuilder report = new StringBuilder("Performance baseline (").append(USERS)
                                                                          .append(" users, ")
                                                                          .append(ITERATIONS)
                                                                          .append(" requests per step) - ")
                                                                          .append(platformDefaults)
                                                                          .append('\n');
        for (StepMeasurement measurement : measurements) {
            report.append(String.format("  %-10s p50 %8.1f ms  p95 %8.1f ms  statements/request %5d (max %5d)  pools %s%n",
                    measurement.step(), measurement.p50Millis(), measurement.p95Millis(), measurement.statementsPerRequest(),
                    measurement.maxStatementsPerRequest(), measurement.pools()));
            for (String statement : measurement.mostRepeatedStatements()) {
                report.append("             ")
                      .append(statement)
                      .append('\n');
            }
        }
        LOGGER.info("{}", report);
    }

    private static byte[] resource(String name) {
        try (InputStream in = PerformanceBaselineIT.class.getClassLoader()
                                                         .getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("No resource [" + name + "] on the classpath");
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read [" + name + "]", ex);
        }
    }

    /** Samples the pools while a step runs, keeping each one's peak. */
    private static final class PoolSampler implements AutoCloseable {

        private static final long INTERVAL_MILLIS = 20;

        private final Map<String, HikariDataSource> pools;
        private final Map<String, int[]> peaks = new LinkedHashMap<>();
        private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();

        PoolSampler(Map<String, HikariDataSource> pools) {
            this.pools = pools;
            pools.keySet()
                 .forEach(name -> peaks.put(name, new int[2]));
            sampler.scheduleAtFixedRate(this::sample, 0, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        }

        private synchronized void sample() {
            pools.forEach((name, pool) -> {
                HikariPoolMXBean bean = pool.getHikariPoolMXBean();
                int[] peak = peaks.get(name);
                peak[0] = Math.max(peak[0], bean.getActiveConnections());
                peak[1] = Math.max(peak[1], bean.getThreadsAwaitingConnection());
            });
        }

        synchronized Map<String, StepMeasurement.PoolPeak> peaks() {
            Map<String, StepMeasurement.PoolPeak> result = new LinkedHashMap<>();
            peaks.forEach((name, peak) -> result.put(name, new StepMeasurement.PoolPeak(pools.get(name)
                                                                                             .getMaximumPoolSize(),
                    peak[0], peak[1])));
            return result;
        }

        @Override
        public void close() {
            sampler.shutdownNow();
        }
    }
}
