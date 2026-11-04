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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.api.messaging.TimeoutException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * An external provisioner suspends a tenant through {@code PUT tenants/{id}/status} and resumes it
 * (#8477): while it is suspended its people and integration clients are refused
 * {@code 409 SUSPENDED}, its job is paused, its client-Java listener consumes nothing and its
 * process timer does not fire - and the tenant beside it runs on. A schema change published
 * meanwhile still reaches it. Resumed, it runs exactly as before: the message that waited on its
 * queue is consumed, the timer fires and its rows are unchanged. A suspended tenant can be
 * deprovisioned.
 *
 * <p>
 * The test plays the external provisioner, as {@code TenantDeprovisioningIT} does - each tenant's
 * database user and schema, its registration, its data source and its activation - with the
 * provisioner's machine token from a mock identity provider, which also signs the tenants' users'
 * and integration client's tokens ({@code TOKEN_GROUPS}, as {@code IntegrationClientsIT}).
 */
@ActiveProfiles("keycloak")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class TenantSuspensionIT extends IntegrationTest {

    private static final String TENANTS_PATH = "/services/tenant-provisioning/tenants/";
    private static final String CURRENT_TENANT_PATH = "/services/security/tenant-selection/current";

    private static final String APP_ID = "library";
    private static final String PROJECT = "tenant-suspension-it";
    private static final String TABLE = "TENANT_SUSPENSION_ORDERS";
    private static final String PROCESS = "tenant-suspension-it-process";
    /**
     * The tenant a request runs in, on a path the tenant selection filters: the current-tenant endpoint
     * is not filtered for a user, who reads it before choosing a tenant.
     */
    private static final String WHOAMI = "/services/js/" + PROJECT + "/whoami.mjs";
    private static final String IN_QUEUE = PROJECT + "-in";
    private static final String OUT_QUEUE = PROJECT + "-out";
    private static final String CLIENT_ID = "tenant-suspension-it-client";
    private static final String CLIENT_ROLE = PROJECT + ".Orders.OrderReadOnly";

    /** The tenant that is suspended. */
    private static final ExternalTenant ALPHA = new ExternalTenant("susp-alpha", "SUSPALPHA", "U_SUSPALPHA");

    /** The tenant beside it, which must run on. */
    private static final ExternalTenant BETA = new ExternalTenant("susp-beta", "SUSPBETA", "U_SUSPBETA");

    private static final String DB_PASSWORD = "tenant-suspension-it-password";
    private static final String AT = "2026-11-04T10:00:00Z";

    private static final long INITIALIZATION_TIMEOUT_SECONDS = 300;
    private static final long RUNTIME_TIMEOUT_SECONDS = 120;

    /** How long a suspended tenant is watched for work it must not do. */
    private static final long QUIET_MILLIS = 5000;

    private static final AtomicLong REVISION = new AtomicLong();

    private static ExternalFrontendIT.MockIdentityProvider identityProvider;

    @LocalServerPort
    private int port;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ProcessEngine processEngine;

    @BeforeAll
    static void startIdentityProviderAndEnableTheApi() throws Exception {
        identityProvider = ExternalFrontendIT.MockIdentityProvider.start();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue("groups");
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(ExternalFrontendIT.CLIENT_ID);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    static void stopIdentityProvider() {
        identityProvider.stop();
    }

    @DynamicPropertySource
    static void keycloakProfile(DynamicPropertyRegistry registry) {
        registry.add("DIRIGIBLE_KEYCLOAK_AUTH_SERVER_URL", () -> identityProvider.issuer());
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_ID", () -> ExternalFrontendIT.CLIENT_ID);
        registry.add("DIRIGIBLE_KEYCLOAK_CLIENT_SECRET", () -> ExternalFrontendIT.CLIENT_SECRET);
        registry.add("DIRIGIBLE_HOST", () -> "{baseUrl}");
    }

    @Test
    @Order(1)
    void aSuspendedTenantIsRefusedAndRunsNothingAndComesBackExactlyAsItWas() throws Exception {
        publishProject(false);
        activateAndAwait(ALPHA);
        activateAndAwait(BETA);
        registerClient(ALPHA);
        insertOrder(ALPHA, 1);
        for (ExternalTenant tenant : List.of(ALPHA, BETA)) {
            assertEquals("echo:warm-up", roundTrip(tenant, "warm-up"), "the listener of " + tenant.id() + " runs before the suspension");
        }
        ProcessInstance alphaTimer = startTimer(ALPHA);
        ProcessInstance betaTimer = startTimer(BETA);
        user(ALPHA).get(WHOAMI)
                   .then()
                   .statusCode(200)
                   .body("tenant", equalTo(ALPHA.id()));

        changeStatus(ALPHA, TenantStatus.SUSPENDED).statusCode(200)
                                                   .body("id", equalTo(ALPHA.id()))
                                                   .body("status", equalTo("SUSPENDED"));

        // its people and its integration client are refused, with the reason
        user(ALPHA).get(WHOAMI)
                   .then()
                   .statusCode(409)
                   .body("reason", equalTo("SUSPENDED"));
        client(ALPHA).get(CURRENT_TENANT_PATH)
                     .then()
                     .statusCode(409)
                     .body("reason", equalTo("SUSPENDED"));
        user(BETA).get(WHOAMI)
                  .then()
                  .statusCode(200)
                  .body("tenant", equalTo(BETA.id()));

        // nothing runs for it, everything runs for the tenant beside it
        assertTrue(allJobTriggersIn(ALPHA, Trigger.TriggerState.PAUSED), "the suspended tenant's job is paused");
        assertTrue(allJobTriggersIn(BETA, Trigger.TriggerState.NORMAL), "the other tenant's job is not");
        send(ALPHA, "while-suspended");
        assertEquals(null, receive(ALPHA, QUIET_MILLIS), "the suspended tenant's listener consumes nothing");
        assertEquals("echo:beside", roundTrip(BETA, "beside"), "the other tenant's listener runs on");
        Awaitility.await()
                  .atMost(RUNTIME_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> isEnded(betaTimer));
        assertTrue(isSuspended(alphaTimer), "the suspended tenant's timer has not fired, though the other tenant's has");

        // the provisioner still maintains its replica and its registry
        putUsers(ALPHA).statusCode(200);
        registerClient(ALPHA);

        // an application version published meanwhile reaches its schema
        publishProject(true);
        assertTrue(hasColumn(ALPHA, "NOTE"), "the suspended tenant's table got the new column");
        assertTrue(hasColumn(BETA, "NOTE"), "and so did the other tenant's");
        assertTrue(allJobTriggersIn(ALPHA, Trigger.TriggerState.PAUSED), "a synchronizer pass does not unpause the job");

        changeStatus(ALPHA, TenantStatus.PROVISIONED).statusCode(200)
                                                     .body("status", equalTo("PROVISIONED"));

        user(ALPHA).get(WHOAMI)
                   .then()
                   .statusCode(200)
                   .body("tenant", equalTo(ALPHA.id()));
        client(ALPHA).get(CURRENT_TENANT_PATH)
                     .then()
                     .statusCode(200)
                     .body("tenant.id", equalTo(ALPHA.id()));
        assertTrue(allJobTriggersIn(ALPHA, Trigger.TriggerState.NORMAL), "the job runs again");
        assertEquals("echo:while-suspended", receive(ALPHA, RUNTIME_TIMEOUT_SECONDS * 1000),
                "the message that arrived while suspended waited on the queue and is consumed now");
        Awaitility.await()
                  .atMost(RUNTIME_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> isEnded(alphaTimer));
        assertEquals(List.of(1), orders(ALPHA), "the rows written before the suspension are unchanged");

        // suspended once more, and then removed: a suspended tenant can be deprovisioned
        changeStatus(ALPHA, TenantStatus.SUSPENDED).statusCode(200);
        startTimerAndSuspendIt(ALPHA);
        provisioner().delete(TENANTS_PATH + ALPHA.id())
                     .then()
                     .statusCode(204);
        assertTrue(tenantService.findById(ALPHA.id())
                                .isEmpty(),
                "the tenant row");
        assertTrue(dataSourceService.findOptionalByName(ALPHA.id() + "_DefaultDB")
                                    .isEmpty(),
                "the tenant's data source");
        assertEquals(0, jobCount(ALPHA), "no paused job of the tenant is left");
        assertEquals(0, runningProcesses(ALPHA), "no suspended process instance of the tenant is left");
        assertEquals(0, rows("DIRIGIBLE_TENANT_CLIENTS", "TENCLIENT_TENANT_ID", ALPHA), "the client registry");
        assertTrue(allJobTriggersIn(BETA, Trigger.TriggerState.NORMAL), "the other tenant is untouched");
    }

    @Test
    @Order(2)
    void theRouteAnswersWhatItCannotDo() {
        provisioner().contentType(ContentType.JSON)
                     .body(Map.of("name", "susp-pending"))
                     .put(TENANTS_PATH + "susp-pending")
                     .then()
                     .statusCode(anyOf(is(200), is(201)));
        changeStatus("susp-pending", "SUSPENDED").statusCode(409);
        changeStatus("default-tenant", "SUSPENDED").statusCode(400)
                                                   .body("status", equalTo(400));
        changeStatus("susp-ghost", "SUSPENDED").statusCode(404);
        changeStatus(BETA.id(), "DEPROVISIONING").statusCode(400);
        changeStatus(BETA.id(), null).statusCode(400);

        api(identityProvider.idToken("plain", List.of(BETA.id() + "." + APP_ID + ".Owner"))).contentType(ContentType.JSON)
                                                                                            .body(Map.of("status", "SUSPENDED"))
                                                                                            .put(TENANTS_PATH + BETA.id() + "/status")
                                                                                            .then()
                                                                                            .statusCode(403);
        given().port(port)
               .header("Sec-Fetch-Mode", "cors")
               .contentType(ContentType.JSON)
               .body(Map.of("status", "SUSPENDED"))
               .put(TENANTS_PATH + BETA.id() + "/status")
               .then()
               .statusCode(401);
        assertEquals(TenantStatus.PROVISIONED, tenantService.findById(BETA.id())
                                                            .orElseThrow()
                                                            .getStatus(),
                "no refused call changed the tenant");
    }

    // --- the provisioner's calls ------------------------------------------------------------------

    private ValidatableResponse changeStatus(ExternalTenant tenant, TenantStatus status) {
        return changeStatus(tenant.id(), status.name());
    }

    private ValidatableResponse changeStatus(String tenantId, String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        return provisioner().contentType(ContentType.JSON)
                            .body(body)
                            .put(TENANTS_PATH + tenantId + "/status")
                            .then();
    }

    private void activateAndAwait(ExternalTenant tenant) throws SQLException {
        createDatabaseUserAndSchema(tenant);
        provisioner().contentType(ContentType.JSON)
                     .body(Map.of("name", tenant.id()))
                     .put(TENANTS_PATH + tenant.id())
                     .then()
                     .statusCode(anyOf(is(200), is(201)));
        provisioner().contentType(ContentType.JSON)
                     .body(Map.of("username", tenant.user(), "password", DB_PASSWORD, "schema", tenant.schema()))
                     .put(TENANTS_PATH + tenant.id() + "/datasources/default")
                     .then()
                     .statusCode(anyOf(is(200), is(201)));
        provisioner().post(TENANTS_PATH + tenant.id() + "/activation")
                     .then()
                     .statusCode(202);
        Awaitility.await()
                  .atMost(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> !"IN_PROGRESS".equals(initializationStatus(tenant)));
        assertEquals("COMPLETED", initializationStatus(tenant), "the initialization status of " + tenant.id());
    }

    private String initializationStatus(ExternalTenant tenant) {
        return provisioner().get(TENANTS_PATH + tenant.id() + "/activation")
                            .then()
                            .statusCode(200)
                            .extract()
                            .path("status");
    }

    private ValidatableResponse putUsers(ExternalTenant tenant) {
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("role", "Owner");
        role.put("state", "GRANTED");
        role.put("grantedBy", "owner@example.test");
        role.put("grantedAt", AT);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("email", "owner@example.test");
        user.put("revision", REVISION.incrementAndGet());
        user.put("status", "ASSIGNED");
        user.put("roles", new ArrayList<>(List.of(role)));
        user.put("invitedBy", "owner@example.test");
        user.put("invitedAt", AT);
        user.put("lastChangedBy", "owner@example.test");
        user.put("lastChangedAt", AT);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("complete", false);
        body.put("revision", REVISION.get());
        body.put("users", List.of(user));
        return provisioner().contentType(ContentType.JSON)
                            .body(body)
                            .put(TENANTS_PATH + tenant.id() + "/users")
                            .then();
    }

    private void registerClient(ExternalTenant tenant) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientId", CLIENT_ID);
        client.put("name", "The suspension IT client");
        client.put("roles", List.of(CLIENT_ROLE));
        client.put("createdBy", "staff@example.test");
        client.put("createdAt", AT);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("revision", REVISION.incrementAndGet());
        body.put("clients", List.of(client));
        provisioner().contentType(ContentType.JSON)
                     .body(body)
                     .put(TENANTS_PATH + tenant.id() + "/clients")
                     .then()
                     .statusCode(200);
    }

    // --- who calls --------------------------------------------------------------------------------

    /** The access token of the provisioning system, a client of the deployment's own audience. */
    private RequestSpecification provisioner() {
        return api(identityProvider.accessToken("service-account-provisioning", "openid dirigible/TENANT_PROVISIONER",
                claims -> claims.audience(ExternalFrontendIT.CLIENT_ID)));
    }

    /** An owner of the tenant, naming it on a bearer request. */
    private RequestSpecification user(ExternalTenant tenant) {
        return api(identityProvider.idToken("owner-of-" + tenant.id(), List.of(tenant.id() + "." + APP_ID + ".Owner"))).header(
                TenantSelectionConstants.TENANT_HEADER, tenant.id());
    }

    /** The tenant's integration client, naming its tenant as it must. */
    private RequestSpecification client(ExternalTenant tenant) {
        String token = identityProvider.accessToken("service-account-" + CLIENT_ID, "openid", claims -> claims.claim("azp", CLIENT_ID)
                                                                                                              .claim("client_id",
                                                                                                                      CLIENT_ID));
        return api(token).header(TenantSelectionConstants.TENANT_HEADER, tenant.id());
    }

    private RequestSpecification api(String token) {
        return given().port(port)
                      .auth()
                      .oauth2(token);
    }

    // --- the project ------------------------------------------------------------------------------

    /**
     * A per-tenant table (with a column more in its second version), a scheduled job that never fires
     * during the test, a client-Java listener that echoes every message of a queue onto another one,
     * and a process that waits on a short timer.
     */
    private void publishProject(boolean withNote) {
        String note = withNote ? """
                ,
                        { "name": "NOTE", "type": "VARCHAR", "length": "100", "nullable": "true" }""" : "";
        write(TABLE + ".table", """
                {
                    "name": "%s",
                    "type": "TABLE",
                    "columns": [
                        { "name": "ORDER_ID", "type": "INTEGER", "length": "0", "nullable": "false", "primaryKey": "true" }%s
                    ]
                }
                """.formatted(TABLE, note));
        if (!withNote) {
            write("heartbeat.job", """
                    {
                        "expression": "0 0 0 1 1 ? 2099",
                        "group": "defined",
                        "handler": "%s/heartbeat-handler.ts",
                        "description": "Never fires during the test",
                        "parameters": []
                    }
                    """.formatted(PROJECT));
            write("heartbeat-handler.ts", "export {};\n");
            write("whoami.mjs", """
                    import { response } from "@aerokit/sdk/http";

                    const BeanProvider = Java.type("org.eclipse.dirigible.components.base.spring.BeanProvider");
                    response.setContentType("application/json");
                    response.println(JSON.stringify({ tenant: BeanProvider.getTenantContext().getCurrentTenant().getId() }));
                    """);
            write("suspension/Echo.java", """
                    package suspension;

                    import org.eclipse.dirigible.sdk.component.Component;
                    import org.eclipse.dirigible.sdk.messaging.ListenerKind;
                    import org.eclipse.dirigible.sdk.messaging.MessageHandler;
                    import org.eclipse.dirigible.sdk.messaging.Producer;

                    @Component
                    public class Echo implements MessageHandler {

                        @Override
                        public String destination() {
                            return "%s";
                        }

                        @Override
                        public ListenerKind kind() {
                            return ListenerKind.QUEUE;
                        }

                        @Override
                        public void onMessage(String message) {
                            Producer.sendToQueue("%s", "echo:" + message);
                        }
                    }
                    """.formatted(IN_QUEUE, OUT_QUEUE));
            write("timer.bpmn", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%s" name="Tenant suspension IT" isExecutable="true">
                        <startEvent id="start"/>
                        <sequenceFlow id="toWait" sourceRef="start" targetRef="wait"/>
                        <intermediateCatchEvent id="wait">
                          <timerEventDefinition><timeDuration>PT10S</timeDuration></timerEventDefinition>
                        </intermediateCatchEvent>
                        <sequenceFlow id="toEnd" sourceRef="wait" targetRef="end"/>
                        <endEvent id="end"/>
                      </process>
                    </definitions>
                    """.formatted(PROCESS));
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void write(String name, String content) {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/" + name,
                content.getBytes(StandardCharsets.UTF_8), false, "application/octet-stream", true);
    }

    // --- what runs for a tenant -------------------------------------------------------------------

    private String roundTrip(ExternalTenant tenant, String message) {
        send(tenant, message);
        return receive(tenant, RUNTIME_TIMEOUT_SECONDS * 1000);
    }

    private void send(ExternalTenant tenant, String message) {
        tenantContext.execute(tenant.id(), () -> {
            MessagingFacade.sendToQueue(IN_QUEUE, message);
            return null;
        });
    }

    /** The next echo on the tenant's out-queue, or {@code null} when none arrives in time. */
    private String receive(ExternalTenant tenant, long timeoutMillis) {
        return tenantContext.execute(tenant.id(), () -> {
            try {
                return MessagingFacade.receiveFromQueue(OUT_QUEUE, timeoutMillis);
            } catch (TimeoutException none) {
                return null;
            }
        });
    }

    private ProcessInstance startTimer(ExternalTenant tenant) {
        return processEngine.getRuntimeService()
                            .startProcessInstanceByKeyAndTenantId(PROCESS, tenant.id());
    }

    /** A process instance started while the tenant is suspended is suspended by a repeated suspend. */
    private void startTimerAndSuspendIt(ExternalTenant tenant) {
        startTimer(tenant);
        changeStatus(tenant, TenantStatus.SUSPENDED).statusCode(200);
        assertEquals(0, processEngine.getRuntimeService()
                                     .createProcessInstanceQuery()
                                     .processInstanceTenantId(tenant.id())
                                     .active()
                                     .count(),
                "every process instance of the suspended tenant is suspended");
    }

    private boolean isEnded(ProcessInstance instance) {
        return processEngine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .processInstanceId(instance.getId())
                            .count() == 0;
    }

    private boolean isSuspended(ProcessInstance instance) {
        ProcessInstance current = processEngine.getRuntimeService()
                                               .createProcessInstanceQuery()
                                               .processInstanceId(instance.getId())
                                               .singleResult();
        return current != null && current.isSuspended();
    }

    private long runningProcesses(ExternalTenant tenant) {
        return processEngine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .processInstanceTenantId(tenant.id())
                            .count();
    }

    private List<JobKey> jobsOf(ExternalTenant tenant) throws SchedulerException {
        String prefix = tenant.id() + "###";
        return scheduler.getJobKeys(GroupMatcher.jobGroupEquals("defined"))
                        .stream()
                        .filter(key -> key.getName()
                                          .startsWith(prefix))
                        .toList();
    }

    private long jobCount(ExternalTenant tenant) throws SchedulerException {
        return jobsOf(tenant).size();
    }

    private boolean allJobTriggersIn(ExternalTenant tenant, Trigger.TriggerState state) throws SchedulerException {
        List<JobKey> jobs = jobsOf(tenant);
        if (jobs.isEmpty()) {
            return false;
        }
        for (JobKey job : jobs) {
            for (Trigger trigger : scheduler.getTriggersOfJob(job)) {
                if (scheduler.getTriggerState(trigger.getKey()) != state) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- the tenant's data ------------------------------------------------------------------------

    private void insertOrder(ExternalTenant tenant, int id) throws SQLException {
        try (Connection connection = tenantConnection(tenant);
                PreparedStatement statement = connection.prepareStatement("INSERT INTO \"" + TABLE + "\" (\"ORDER_ID\") VALUES (?)")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private List<Integer> orders(ExternalTenant tenant) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = tenantConnection(tenant);
                PreparedStatement statement =
                        connection.prepareStatement("SELECT \"ORDER_ID\" FROM \"" + TABLE + "\" ORDER BY \"ORDER_ID\"");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getInt(1));
            }
        }
        return ids;
    }

    private boolean hasColumn(ExternalTenant tenant, String column) throws SQLException {
        try (Connection connection = tenantConnection(tenant);
                ResultSet columns = connection.getMetaData()
                                              .getColumns(null, null, TABLE, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME")) && tenant.schema()
                                                                                       .equalsIgnoreCase(
                                                                                               columns.getString("TABLE_SCHEM"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private Connection tenantConnection(ExternalTenant tenant) throws SQLException {
        return dataSourcesManager.getDataSource(tenant.id() + "_DefaultDB")
                                 .getConnection();
    }

    private int rows(String table, String tenantColumn, ExternalTenant tenant) throws SQLException {
        try (Connection connection = dataSourcesManager.getSystemDataSource()
                                                       .getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE " + tenantColumn + " = ?")) {
            statement.setString(1, tenant.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private boolean schemaExists(ExternalTenant tenant) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet schemas = connection.getMetaData()
                                              .getSchemas(null, tenant.schema())) {
            return schemas.next();
        }
    }

    /** What the external provisioner does before it calls the API. */
    private void createDatabaseUserAndSchema(ExternalTenant tenant) throws SQLException {
        if (schemaExists(tenant)) {
            return;
        }
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            execute(connection, SqlFactory.getNative(connection)
                                          .create()
                                          .user(tenant.user(), DB_PASSWORD)
                                          .build());
            execute(connection, SqlFactory.getNative(connection)
                                          .create()
                                          .schema(tenant.schema())
                                          .authorization(tenant.user())
                                          .build());
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    /**
     * A tenant whose database user and schema exist outside the platform.
     *
     * @param id the tenant id
     * @param schema the schema the provisioner created
     * @param user the database user the provisioner created
     */
    private record ExternalTenant(String id, String schema, String user) {
    }
}
