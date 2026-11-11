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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The tenant data export, end to end on one instance: two tenants, each with rows in an application
 * table and a stored document; an Owner exports their tenant and downloads one archive that holds
 * their tenant's data and nothing of the other's.
 *
 * <p>
 * Both tenants are provisioned after the instance booted, so every export here also shows that a
 * tenant provisioned late has the export's processes. Driven with MockMvc under
 * {@code TOKEN_GROUPS}: a fabricated OIDC person selects the tenant, the way the picker does.
 */
@Tag("slow")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantExportIT extends IntegrationTest {

    private static final String EXPORTS = "/services/data/tenant-export";
    private static final String DOCUMENTS = "/services/documents";
    private static final String SELECTION = "/services/security/tenant-selection";
    private static final String APP_ID = "library";
    private static final String GROUPS_CLAIM = "groups";
    private static final String PROJECT = "tenant-export-it";
    private static final String TABLE = "TENANT_EXPORT_NOTES";
    private static final String RESTORE_DATA_SOURCE = "TENANTEXPORTRESTORE";
    private static final String RESTORE_FOLDER = "tenant-export-restore";
    private static final String ALPHA_OWNER = "owner.alpha@example.com";
    private static final String BETA_OWNER = "owner.beta@example.com";
    private static final String ALPHA_NOTE = "a note of alpha";
    private static final String BETA_NOTE = "a note of beta";
    private static final String ALPHA_FILE = "alpha-contract.txt";
    private static final String BETA_FILE = "beta-contract.txt";
    private static final String ALPHA_FILE_CONTENT = "the contract of alpha";
    private static final Duration WAIT = Duration.ofSeconds(120);

    private static DirigibleTestTenant alpha;
    private static DirigibleTestTenant beta;
    private static long alphaExport;
    private static byte[] alphaArchive;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private DataSourceInitializer dataSourceInitializer;

    @Autowired
    private TenantContext tenantContext;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    static void enableTheExport() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
        DirigibleConfig.TENANT_EXPORT_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_EXPORT_MAX_ARCHIVES.setStringValue("2");
    }

    @BeforeEach
    void provisionTheTenantsOnce() throws Exception {
        if (alpha != null) {
            return;
        }
        // ids without a hyphen: the tenant's schema is named by its id, and the schema import that the
        // restore runs builds its statements for a schema name that needs no quoting
        DirigibleTestTenant first = tenant("exportalpha");
        DirigibleTestTenant second = tenant("exportbeta");
        createTenants(first, second);
        waitForTenantsProvisioning(List.of(first, second));

        publishTheTable();
        insertNote(first, 1, ALPHA_NOTE);
        insertNote(second, 1, BETA_NOTE);
        alpha = first;
        beta = second;
        upload(enter(alpha, ALPHA_OWNER, "Owner"), person(alpha, ALPHA_OWNER, "Owner"), ALPHA_FILE, ALPHA_FILE_CONTENT);
        upload(enter(beta, BETA_OWNER, "Owner"), person(beta, BETA_OWNER, "Owner"), BETA_FILE, "the contract of beta");
    }

    @Test
    @Order(1)
    void anOwnersArchiveHoldsTheirTenantsDataAndNothingOfAnothers() throws Exception {
        MockHttpSession owner = enter(alpha, ALPHA_OWNER, "Owner");
        Authentication ann = person(alpha, ALPHA_OWNER, "Owner");
        mvc.perform(get(EXPORTS + "/context").session(owner)
                                             .with(authentication(ann)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.maxArchives").value(2));

        // a tenant, a data source or a path in the request never chooses what is exported
        String accepted = mvc.perform(post(EXPORTS).session(owner)
                                                   .with(authentication(ann))
                                                   .param("tenantId", beta.getId())
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content("{\"tenantId\":\"" + beta.getId() + "\",\"dataSource\":\"SystemDB\"}"))
                             .andExpect(status().isAccepted())
                             .andExpect(jsonPath("$.requestedBy").value(ALPHA_OWNER))
                             .andReturn()
                             .getResponse()
                             .getContentAsString();
        alphaExport = json.readTree(accepted)
                          .get("id")
                          .asLong();
        JsonNode completed = awaitCompleted(owner, ann, alphaExport);
        String archiveName = completed.get("archiveName")
                                      .asString();
        assertTrue(archiveName.startsWith("tenant-" + alpha.getId() + "-data-export-") && archiveName.endsWith(".zip"), archiveName);
        assertNotNull(completed.get("expiresAt"));

        MockHttpServletResponse download = mvc.perform(get(EXPORTS + "/" + alphaExport + "/archive").session(owner)
                                                                                                    .with(authentication(ann)))
                                              .andExpect(status().isOk())
                                              .andReturn()
                                              .getResponse();
        assertEquals("attachment; filename=\"" + archiveName + "\"", download.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        alphaArchive = download.getContentAsByteArray();
        assertEquals(completed.get("sizeBytes")
                              .asLong(),
                alphaArchive.length);

        Map<String, String> entries = unzip(alphaArchive);
        assertTrue(entries.containsKey("manifest.json"), entries.keySet()
                                                                .toString());
        assertTrue(entries.containsKey("database/export-topology.json"), entries.keySet()
                                                                                .toString());
        assertTrue(entries.containsKey("files/" + ALPHA_FILE), entries.keySet()
                                                                      .toString());
        assertEquals(ALPHA_FILE_CONTENT, entries.get("files/" + ALPHA_FILE));
        String csv = entry(entries, "database/" + TABLE + ".csv");
        assertTrue(csv.contains(ALPHA_NOTE), csv);
        assertNotNull(entry(entries, "database/" + TABLE + ".json"));

        // nothing of the other tenant, no platform table, and not the exports themselves
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            assertFalse(entry.getValue()
                             .contains(BETA_NOTE),
                    entry.getKey());
            assertFalse(entry.getKey()
                             .contains(BETA_FILE),
                    entry.getKey());
            assertFalse(entry.getKey()
                             .contains("__EXPORTS"),
                    entry.getKey());
            assertFalse(entry.getKey()
                             .toUpperCase()
                             .startsWith("DATABASE/DIRIGIBLE_"),
                    entry.getKey());
        }

        JsonNode manifest = json.readTree(entries.get("manifest.json"));
        assertEquals(alpha.getId(), manifest.get("tenantId")
                                            .asString());
        assertEquals(1, manifest.get("files")
                                .asInt());
        assertEquals(1, manifest.get("tables")
                                .size());
        assertEquals(1, manifest.get("tables")
                                .get(0)
                                .get("rows")
                                .asInt());
    }

    @Test
    @Order(2)
    void anotherTenantsOwnerAndAMemberDoNotReachIt() throws Exception {
        MockHttpSession owner = enter(beta, BETA_OWNER, "Owner");
        Authentication ben = person(beta, BETA_OWNER, "Owner");
        mvc.perform(get(EXPORTS).session(owner)
                                .with(authentication(ben)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(EXPORTS + "/" + alphaExport).session(owner)
                                                    .with(authentication(ben)))
           .andExpect(status().isNotFound());
        mvc.perform(get(EXPORTS + "/" + alphaExport + "/archive").session(owner)
                                                                 .with(authentication(ben)))
           .andExpect(status().isNotFound());
        mvc.perform(delete(EXPORTS + "/" + alphaExport).session(owner)
                                                       .with(authentication(ben)))
           .andExpect(status().isNotFound());

        MockHttpSession member = enter(alpha, "member.alpha@example.com", "User");
        Authentication mia = person(alpha, "member.alpha@example.com", "User");
        mvc.perform(get(EXPORTS + "/context").session(member)
                                             .with(authentication(mia)))
           .andExpect(status().isForbidden());
        mvc.perform(post(EXPORTS).session(member)
                                 .with(authentication(mia)))
           .andExpect(status().isForbidden());
        mvc.perform(get(EXPORTS + "/" + alphaExport + "/archive").session(member)
                                                                 .with(authentication(mia)))
           .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void theArchiveIsNotAmongTheTenantsDocuments() throws Exception {
        mvc.perform(get(DOCUMENTS).session(enter(alpha, ALPHA_OWNER, "Owner"))
                                  .with(authentication(person(alpha, ALPHA_OWNER, "Owner"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.children[*].name", hasItem(ALPHA_FILE)))
           .andExpect(jsonPath("$.children[*].name", not(hasItem("__EXPORTS"))));
    }

    @Test
    @Order(4)
    void oneExportAtATimeAndNoMoreArchivesThanTheLimit() throws Exception {
        MockHttpSession owner = enter(alpha, ALPHA_OWNER, "Owner");
        Authentication ann = person(alpha, ALPHA_OWNER, "Owner");

        // while one runs: no second one, no archive yet, no delete
        setStatus(alphaExport, "RUNNING");
        mvc.perform(post(EXPORTS).session(owner)
                                 .with(authentication(ann)))
           .andExpect(status().isConflict());
        mvc.perform(get(EXPORTS + "/" + alphaExport + "/archive").session(owner)
                                                                 .with(authentication(ann)))
           .andExpect(status().isConflict());
        mvc.perform(delete(EXPORTS + "/" + alphaExport).session(owner)
                                                       .with(authentication(ann)))
           .andExpect(status().isConflict());
        setStatus(alphaExport, "COMPLETED");

        // the limit is two archives: the second is made, the third is refused until one is deleted
        long second = start(owner, ann);
        awaitCompleted(owner, ann, second);
        mvc.perform(post(EXPORTS).session(owner)
                                 .with(authentication(ann)))
           .andExpect(status().isConflict());
        // ... and it is each tenant's own
        MockHttpSession betaOwner = enter(beta, BETA_OWNER, "Owner");
        Authentication ben = person(beta, BETA_OWNER, "Owner");
        awaitCompleted(betaOwner, ben, start(betaOwner, ben));

        mvc.perform(delete(EXPORTS + "/" + second).session(owner)
                                                  .with(authentication(ann)))
           .andExpect(status().isNoContent());
        mvc.perform(get(EXPORTS + "/" + second).session(owner)
                                               .with(authentication(ann)))
           .andExpect(status().isNotFound());
        long third = start(owner, ann);
        awaitCompleted(owner, ann, third);
        mvc.perform(delete(EXPORTS + "/" + third).session(owner)
                                                 .with(authentication(ann)))
           .andExpect(status().isNoContent());
        mvc.perform(get(EXPORTS).session(owner)
                                .with(authentication(ann)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].id").value(alphaExport));
    }

    @Test
    @Order(5)
    void theArchiveRestoresTheTablesAndTheDocuments() throws Exception {
        // the whole archive unpacked into an empty folder gives back the documents ...
        mvc.perform(post(DOCUMENTS + "/folder").with(user("admin").roles("ADMINISTRATOR"))
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content("{\"parentFolder\":\"/\",\"name\":\"" + RESTORE_FOLDER + "\"}"))
           .andExpect(status().isOk());
        mvc.perform(multipart(DOCUMENTS + "/zip").file(new MockMultipartFile("file", "archive.zip", "application/zip", alphaArchive))
                                                 .param("path", "/" + RESTORE_FOLDER)
                                                 .with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNoContent());
        String restored = mvc.perform(get(DOCUMENTS + "/preview").param("path", "/" + RESTORE_FOLDER + "/files/" + ALPHA_FILE)
                                                                 .with(user("admin").roles("ADMINISTRATOR")))
                             .andExpect(status().isOk())
                             .andReturn()
                             .getResponse()
                             .getContentAsString();
        assertEquals(ALPHA_FILE_CONTENT, restored);

        // ... and its database part, given to the schema import, gives back the rows
        DataSource target = new DataSource();
        target.setName(RESTORE_DATA_SOURCE);
        target.setDriver("org.h2.Driver");
        target.setUsername("sa");
        target.setPassword("saPass");
        target.setUrl("jdbc:h2:file:./target/dirigible/h2/" + RESTORE_DATA_SOURCE);
        dataSourceInitializer.initialize(target);
        String schema = tenantSchema(alpha);
        try (Connection connection = dataSourcesManager.getDataSource(RESTORE_DATA_SOURCE)
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement("CREATE SCHEMA IF NOT EXISTS \"" + schema + "\"")) {
            statement.executeUpdate();
        }

        mvc.perform(post("/services/data/schema/importProcesses").with(user("admin").roles("ADMINISTRATOR"))
                                                                 .contentType(MediaType.APPLICATION_JSON)
                                                                 .content("{\"dataSource\":\"" + RESTORE_DATA_SOURCE
                                                                         + "\",\"exportPath\":\"/" + RESTORE_FOLDER + "/database\"}"))
           .andExpect(status().isAccepted());
        Awaitility.await()
                  .atMost(WAIT)
                  .pollInterval(Duration.ofSeconds(1))
                  .ignoreExceptions()
                  .until(() -> List.of(ALPHA_NOTE)
                                   .equals(restoredNotes(schema)));
    }

    @Test
    @Order(6)
    void anExpiredArchiveIsGone() throws Exception {
        try (Connection connection = dataSourcesManager.getSystemDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE DIRIGIBLE_TENANT_EXPORTS SET TENEXPORT_EXPIRES_AT = ? WHERE TENEXPORT_ID = ?")) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()
                                                            .minusSeconds(60)));
            statement.setLong(2, alphaExport);
            assertEquals(1, statement.executeUpdate());
        }
        mvc.perform(get(EXPORTS + "/" + alphaExport + "/archive").session(enter(alpha, ALPHA_OWNER, "Owner"))
                                                                 .with(authentication(person(alpha, ALPHA_OWNER, "Owner"))))
           .andExpect(status().isGone());
    }

    // --- the export's routes ----------------------------------------------------------------------

    private long start(MockHttpSession session, Authentication person) throws Exception {
        String accepted = mvc.perform(post(EXPORTS).session(session)
                                                   .with(authentication(person)))
                             .andExpect(status().isAccepted())
                             .andReturn()
                             .getResponse()
                             .getContentAsString();
        return json.readTree(accepted)
                   .get("id")
                   .asLong();
    }

    private JsonNode awaitCompleted(MockHttpSession session, Authentication person, long id) {
        Awaitility.await()
                  .atMost(WAIT)
                  .pollInterval(Duration.ofSeconds(1))
                  .until(() -> {
                      String status = export(session, person, id).get("status")
                                                                 .asString();
                      if ("FAILED".equals(status)) {
                          throw new AssertionError("export [" + id + "] failed: " + export(session, person, id));
                      }
                      return "COMPLETED".equals(status);
                  });
        return export(session, person, id);
    }

    private JsonNode export(MockHttpSession session, Authentication person, long id) {
        try {
            return json.readTree(mvc.perform(get(EXPORTS + "/" + id).session(session)
                                                                    .with(authentication(person)))
                                    .andExpect(status().isOk())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read export [" + id + "]", ex);
        }
    }

    private void setStatus(long id, String status) throws SQLException {
        try (Connection connection = dataSourcesManager.getSystemDataSource()
                                                       .getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("UPDATE DIRIGIBLE_TENANT_EXPORTS SET TENEXPORT_STATUS = ? WHERE TENEXPORT_ID = ?")) {
            statement.setString(1, status);
            statement.setLong(2, id);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static Map<String, String> unzip(byte[] archive) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        return entries;
    }

    /** An entry by its name whatever its case: a table's name is as its database reports it. */
    private static String entry(Map<String, String> entries, String name) {
        return entries.entrySet()
                      .stream()
                      .filter(entry -> entry.getKey()
                                            .equalsIgnoreCase(name))
                      .map(Map.Entry::getValue)
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("no entry [" + name + "] in " + entries.keySet()));
    }

    // --- the tenants' data ------------------------------------------------------------------------

    private void publishTheTable() {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/" + TABLE + ".table", """
                {
                    "name": "%s",
                    "type": "TABLE",
                    "columns": [
                        { "name": "NOTE_ID", "type": "INTEGER", "length": "0", "nullable": "false", "primaryKey": "true" },
                        { "name": "NOTE_TEXT", "type": "VARCHAR", "length": "100", "nullable": "true" }
                    ]
                }
                """.formatted(TABLE)
                   .getBytes(StandardCharsets.UTF_8),
                false, "application/octet-stream", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void insertNote(DirigibleTestTenant tenant, int id, String text) {
        // the table reaches a tenant's schema with the synchronization of that tenant
        Awaitility.await()
                  .atMost(WAIT)
                  .pollInterval(Duration.ofSeconds(2))
                  .ignoreExceptions()
                  .until(() -> {
                      try (Connection connection = tenantConnection(tenant);
                              PreparedStatement statement = connection.prepareStatement(
                                      "INSERT INTO \"" + TABLE + "\" (\"NOTE_ID\", \"NOTE_TEXT\") VALUES (?, ?)")) {
                          statement.setInt(1, id);
                          statement.setString(2, text);
                          return statement.executeUpdate() == 1;
                      }
                  });
    }

    private static DirigibleTestTenant tenant(String id) {
        return new DirigibleTestTenant(false, id, id, id, id + "-user", UUID.randomUUID()
                                                                            .toString());
    }

    private Connection tenantConnection(DirigibleTestTenant tenant) throws SQLException {
        return tenantContext.execute(tenant.getId(), () -> dataSourcesManager.getDefaultDataSource()
                                                                             .getConnection());
    }

    private String tenantSchema(DirigibleTestTenant tenant) throws SQLException {
        try (Connection connection = tenantConnection(tenant)) {
            return connection.getSchema();
        }
    }

    private List<String> restoredNotes(String schema) throws SQLException {
        try (Connection connection = dataSourcesManager.getDataSource(RESTORE_DATA_SOURCE)
                                                       .getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT \"NOTE_TEXT\" FROM \"" + schema + "\".\"" + TABLE + "\" ORDER BY \"NOTE_ID\"");
                ResultSet rows = statement.executeQuery()) {
            List<String> notes = new java.util.ArrayList<>();
            while (rows.next()) {
                notes.add(rows.getString(1));
            }
            return notes;
        }
    }

    private void upload(MockHttpSession session, Authentication person, String name, String content) throws Exception {
        mvc.perform(multipart(DOCUMENTS).file(new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8)))
                                        .session(session)
                                        .with(authentication(person)))
           .andExpect(status().isOk());
    }

    // --- who calls --------------------------------------------------------------------------------

    private MockHttpSession enter(DirigibleTestTenant tenant, String email, String role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post(SELECTION).session(session)
                                   .with(authentication(person(tenant, email, role)))
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content("{\"tenantId\":\"" + tenant.getId() + "\"}"))
           .andExpect(status().isOk());
        return session;
    }

    /** A signed-in person whose groups make them [role] of the tenant, as a selection leaves them. */
    private static Authentication person(DirigibleTestTenant tenant, String email, String role) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", email, "email", email, GROUPS_CLAIM, List.of(tenant.getId() + "." + APP_ID + "." + role)));
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken),
                List.of(new SimpleGrantedAuthority("ROLE_" + role)), "keycloak");
    }
}
