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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import io.restassured.response.Response;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.engine.document.domain.CmsSeed;
import org.eclipse.dirigible.components.engine.document.service.CmsSeedService;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Print templates as a versioned, tenant-selectable catalogue (#7755), over HTTP: a shipped
 * template becomes an immutable {@code <name>@<version>} beside the earlier ones, the tenant picks
 * the active one through its configuration, and a customised template seeded before versions
 * existed is kept and selected rather than replaced.
 */
// One Dirigible boot for the whole class: each method uses its own document type and cleans up.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrintTemplateCatalogIT extends IntegrationTest {

    private static final String PROJECT = "print-template-catalog-it";
    private static final String PROJECT_DESCRIPTOR = registryPath("/" + PROJECT + "/project.json");
    private static final String VERSIONED_TEMPLATE = registryPath("/" + PROJECT + "/doc/Templates/VersionedDoc/Print/en/standard.print");
    private static final String LEGACY_TEMPLATE = registryPath("/" + PROJECT + "/doc/Templates/LegacyDoc/Print/en/standard.print");
    private static final String UPGRADED_LOCATION = "/" + PROJECT + "/doc/Templates/UpgradedDoc/Print/en/standard.print";
    private static final String UPGRADED_TEMPLATE = registryPath(UPGRADED_LOCATION);

    private static final String VERSIONED = "/services/print/VersionedDoc";
    private static final String LEGACY = "/services/print/LegacyDoc";
    private static final String UPGRADED = "/services/print/UpgradedDoc";
    private static final String DOCUMENTS = "/services/documents";
    private static final String CONFIGURATIONS = "/services/core/configurations/tenant";
    private static final String VERSIONED_KEY = "DIRIGIBLE_PRINT_TEMPLATE_VERSIONEDDOC_EN";
    private static final String LEGACY_KEY = "DIRIGIBLE_PRINT_TEMPLATE_LEGACYDOC_EN";
    private static final String UPGRADED_KEY = "DIRIGIBLE_PRINT_TEMPLATE_UPGRADEDDOC_EN";

    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DefinitionService definitionService;

    @Autowired
    private CmsSeedService cmsSeedService;

    @Test
    void releasesAddVersionsAndTheTenantSelectsTheOneThatPrints() {
        publish(PROJECT_DESCRIPTOR, descriptor("1.28.0"));
        publish(VERSIONED_TEMPLATE, template("v1"));

        // Seeded as a version, not as a plain CMS file: no bare standard.print beside it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(VERSIONED + "/templates?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("name", contains("standard@1.28.0"))
                                                 .body("[0].kind", equalTo("shipped"))
                                                 .body("[0].active", equalTo(true)),
                ASSERTION_TIMEOUT_SECONDS);

        publish(PROJECT_DESCRIPTOR, descriptor("1.30.0"));
        publish(VERSIONED_TEMPLATE, template("v2"));

        // The new release is added beside the previous one, and a tenant that never chose gets it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(VERSIONED + "/templates?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("name", contains("standard@1.30.0", "standard@1.28.0"))
                                                 .body("find { it.name == 'standard@1.30.0' }.active", equalTo(true)),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(VERSIONED + "/templates/standard@1.28.0?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("v1")));

        // A tenant customisation is a duplicate that records the version it derives from.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(VERSIONED + "/templates/standard@1.28.0/duplicate?lang=en&as=acme")
                                                 .then()
                                                 .statusCode(201)
                                                 .body("name", equalTo("acme")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(VERSIONED + "/templates/acme?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(startsWith("<!-- derived-from: standard@1.28.0 -->")));

        // Selecting it is a tenant configuration; the newer shipped version is flagged.
        setConfiguration(VERSIONED_KEY, "acme");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(VERSIONED + "/templates?lang=en&details=true")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.name == 'acme' }.active", equalTo(true))
                                                 .body("find { it.name == 'acme' }.derivedFrom", equalTo("standard@1.28.0"))
                                                 .body("find { it.name == 'standard@1.30.0' }.newer", equalTo(true)),
                ASSERTION_TIMEOUT_SECONDS);

        // Shipped versions are immutable, the active template cannot be deleted, a broken one not saved.
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(template("tampered"))
                                                 .when()
                                                 .put(VERSIONED + "/templates/standard@1.30.0?lang=en")
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(VERSIONED + "/templates/standard@1.28.0?lang=en")
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(VERSIONED + "/templates/acme?lang=en")
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body("<document><page>")
                                                 .when()
                                                 .put(VERSIONED + "/templates/acme?lang=en")
                                                 .then()
                                                 .statusCode(400));

        // ... and the Documents perspective cannot change a shipped version behind the catalogue's back.
        String shippedPath = "/Templates/VersionedDoc/Print/en/standard@1.30.0.print";
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"path\": \"" + shippedPath + "\", \"name\": \"mine.print\"}")
                                                 .when()
                                                 .put(DOCUMENTS)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message", containsString("shipped print template version")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("[\"" + shippedPath + "\"]")
                                                 .when()
                                                 .delete(DOCUMENTS)
                                                 .then()
                                                 .statusCode(409));

        // The active template prints, and so does any other one asked for explicitly.
        restAssuredExecutor.execute(() -> print(VERSIONED, "").then()
                                                              .statusCode(200)
                                                              .contentType("application/pdf"));
        restAssuredExecutor.execute(() -> print(VERSIONED, "&template=standard@1.30.0").then()
                                                                                       .statusCode(200));
        restAssuredExecutor.execute(() -> print(VERSIONED, "&template=nope").then()
                                                                            .statusCode(404));

        // The document type is listed for the Settings page.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/print/document-types")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("entity", hasItem("VersionedDoc")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(VERSIONED + "/languages")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].code", equalTo("en"))
                                                 .body("[0].selectionKey", equalTo(VERSIONED_KEY)));
    }

    @Test
    void anUpgradedInstanceRetiresTheGenericSeedAndRecognisesTheTenantsUneditedCopyOfThePreviousRelease() throws Exception {
        String previous = template("previous");
        // What an instance upgraded from the generic create-if-absent seed carries: the file's
        // cms-seed definition and row (FAILED, as one is when the CMS was unreachable on the last
        // boot), and in the tenant the copy that seed made of the previous release.
        Definition definition =
                new Definition(UPGRADED_LOCATION, "standard", CmsSeed.ARTEFACT_TYPE, previous.getBytes(StandardCharsets.UTF_8));
        definition.setState(DefinitionState.PARSED);
        definition = definitionService.save(definition);
        CmsSeed row = new CmsSeed(UPGRADED_LOCATION, "standard.print", null);
        row.setCmsPath("Templates/UpgradedDoc/Print/en/standard.print");
        row.setContent(previous.getBytes(StandardCharsets.UTF_8));
        row.setLifecycle(ArtefactLifecycle.FAILED);
        row.updateKey();
        cmsSeedService.save(row);
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(previous)
                                                 .when()
                                                 .put(UPGRADED + "/templates/standard?lang=en")
                                                 .then()
                                                 .statusCode(204));

        // One pass for both files: a pass between them would reap the legacy row, whose file does not
        // exist yet - before the print template synchronizer could retire it.
        write(PROJECT_DESCRIPTOR, descriptor("1.28.0"));
        publish(UPGRADED_TEMPLATE, template("current"));

        // The unedited copy became the version it is, not a customisation: no selection, and the
        // tenant prints the current release.
        String previousVersion = "standard@" + shortHash(previous);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(UPGRADED + "/templates?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("name", contains("standard@1.28.0", previousVersion))
                                                 .body("find { it.name == 'standard@1.28.0' }.active", equalTo(true)),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONFIGURATIONS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.key == '" + UPGRADED_KEY + "' }", nullValue()));

        // The generic seed's definition and row are retired, so nothing waits for them any more.
        assertEquals(DefinitionState.DELETED, definitionService.findByKey(definition.getKey())
                                                               .getState());
        assertTrue(cmsSeedService.findByLocation(UPGRADED_LOCATION)
                                 .isEmpty());
    }

    @Test
    void aCustomisedLegacyTemplateIsKeptAndSelectedSoTheTenantOutputDoesNotChange() {
        // The shape the create-if-absent seed left in every tenant before versions existed, customised.
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(template("customised"))
                                                 .when()
                                                 .put(LEGACY + "/templates/standard?lang=en")
                                                 .then()
                                                 .statusCode(204));

        publish(LEGACY_TEMPLATE, template("shipped"));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LEGACY + "/templates?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("kind", containsInAnyOrder("shipped", "tenant"))
                                                 .body("find { it.kind == 'tenant' }.name", equalTo("standard-custom"))
                                                 .body("find { it.kind == 'tenant' }.active", equalTo(true)),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONFIGURATIONS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.key == '" + LEGACY_KEY + "' }.value", equalTo("standard-custom")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LEGACY + "/templates/standard-custom?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(startsWith("<!-- derived-from: standard@legacy -->"))
                                                 .body(containsString("customised")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LEGACY + "/templates?lang=en&details=true")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.kind == 'shipped' }.newer", equalTo(true)));
    }

    @AfterEach
    void cleanup() {
        for (String key : new String[] {VERSIONED_KEY, LEGACY_KEY, UPGRADED_KEY}) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete(CONFIGURATIONS + "?key=" + key)
                                                     .then()
                                                     .statusCode(204));
        }
        boolean any = false;
        for (String path : new String[] {VERSIONED_TEMPLATE, LEGACY_TEMPLATE, UPGRADED_TEMPLATE, PROJECT_DESCRIPTOR}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                any = true;
            }
        }
        if (any) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void publish(String path, String content) {
        write(path, content);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void write(String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(bytes);
        } else {
            repository.createResource(path, bytes, false, "text/plain", true);
        }
    }

    private void setConfiguration(String key, String value) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"key\": \"" + key + "\", \"value\": \"" + value + "\"}")
                                                 .when()
                                                 .put(CONFIGURATIONS)
                                                 .then()
                                                 .statusCode(200));
    }

    private static Response print(String endpoint, String query) {
        return given().contentType("application/json")
                      .body("{\"document\": {\"number\": \"INV-1\"}, \"items\": []}")
                      .when()
                      .post(endpoint + "?lang=en" + query);
    }

    private static String shortHash(String content) throws Exception {
        return HexFormat.of()
                        .formatHex(MessageDigest.getInstance("SHA-256")
                                                .digest(content.getBytes(StandardCharsets.UTF_8)))
                        .substring(0, 8);
    }

    private static String registryPath(String location) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
    }

    private static String descriptor(String version) {
        return "{\"guid\": \"" + PROJECT + "\", \"version\": \"" + version + "\"}";
    }

    private static String template(String marker) {
        return """
                <document id="%s">
                    <page>
                        <section>
                            <field label="Number">{{document.number}}</field>
                        </section>
                    </page>
                </document>
                """.formatted(marker);
    }
}
