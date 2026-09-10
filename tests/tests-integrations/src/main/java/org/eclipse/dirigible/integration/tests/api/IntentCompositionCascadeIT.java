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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.annotation.DirtiesContext;

/**
 * Deleting a composition master at runtime, through the published application: what it owns goes
 * with it, what declares {@code whenMasterDeleted: refuse} stops it, and a refusal reached half-way
 * down a chain leaves the whole thing standing.
 *
 * <p>
 * The cascade of #7100 was pinned only by assertions over the emitted DAO
 * ({@code contains("itemsRepository.delete(stale)")}), which compiles the generated code and
 * exercises none of it (issue #7162). The three claims the feature actually makes are all runtime
 * ones, and each is asserted here at the outermost layer:
 *
 * <ol>
 * <li>the children are GONE after the master's delete - the orphan rows that kept a deleted
 * vacation request's days charging its entitlement;
 * <li>they left through their OWN repository, so their {@code -deleted} event fired and the roll-up
 * over them relinquished what it counted - a raw SQL delete would empty the table and leave the
 * count wrong, which is the same defect with the rows hidden;
 * <li>a failure part-way through the cascade rolls the master BACK, children included. Each
 * repository call is otherwise its own transaction, so a chain that refuses at its second level
 * would commit the first level's deletes and keep the master - orphans again, made by the cascade
 * itself.
 * </ol>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCompositionCascadeIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "cascade";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TRIPS = API + "/trip/TripController";
    private static final String LEGS = API + "/trip/TripLegController";
    private static final String COPIES = API + "/trip/TripCopyController";
    private static final String NOTES = API + "/trip/TripLegNoteController";
    private static final String FLEETS = API + "/fleet/FleetController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's delete event, which is dispatched after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    /**
     * A trip owning two kinds of child with the two different decisions, and a chain one level deeper.
     * {@code Fleet.legCount} is the roll-up whose relinquishing proves the children left through their
     * own repositories.
     */
    private static final String INTENT_YAML = """
            name: cascade
            description: composition cascade fixture - a deleted master takes what it owns with it

            entities:
              - name: Fleet
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string, length: 100 }
                  - { name: legCount, type: integer }

              - name: Trip
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }

              # The cascading child: it goes with its trip, and its own delete event is what
              # gives the fleet its count back.
              - name: TripLeg
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: distance, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Trip,  kind: manyToOne, to: Trip, composition: true, required: true }
                  - { name: Fleet, kind: manyToOne, to: Fleet }

              # The author's alternative on a sibling child: the trip's delete is refused
              # while a printed copy of it exists.
              - name: TripCopy
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Trip, kind: manyToOne, to: Trip, composition: true, required: true, whenMasterDeleted: refuse }

              # One level deeper, refusing: a leg carrying a note cannot be swept away, so the
              # trip's cascade fails HALF-WAY - after the sibling leg was already deleted.
              - name: TripLegNote
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string, length: 200 }
                relations:
                  - { name: TripLeg, kind: manyToOne, to: TripLeg, composition: true, required: true, whenMasterDeleted: refuse }

            rollups:
              - { name: fleetLegCount, entity: TripLeg, via: Fleet, field: legCount }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void deleting_a_master_deletes_what_it_owns_and_a_refusal_leaves_the_whole_chain_standing() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int fleet = create(FLEETS, "{\"Name\":\"north\"}");

        // A plain document: a trip and the two legs it owns.
        int cascading = create(TRIPS, "{\"Note\":\"cascading\"}");
        create(LEGS, "{\"Trip\":" + cascading + ",\"Distance\":10,\"Fleet\":" + fleet + "}");
        create(LEGS, "{\"Trip\":" + cascading + ",\"Distance\":20,\"Fleet\":" + fleet + "}");

        // A second one whose second leg carries a note - the chain that refuses one level down.
        int chained = create(TRIPS, "{\"Note\":\"chained\"}");
        create(LEGS, "{\"Trip\":" + chained + ",\"Distance\":30,\"Fleet\":" + fleet + "}");
        int notedLeg = create(LEGS, "{\"Trip\":" + chained + ",\"Distance\":40,\"Fleet\":" + fleet + "}");
        create(NOTES, "{\"TripLeg\":" + notedLeg + ",\"Text\":\"keep me\"}");

        assertFleetCount(fleet, 4);

        // (1) The master's delete takes its children with it.
        delete(TRIPS + "/" + cascading, 200);
        assertGone(TRIPS + "/" + cascading);
        assertLegsOfTrip(cascading, 0);

        // (2) ...through their own repositories, so the roll-up over them relinquished. A cascade that
        // emptied the table with one SQL statement would leave this at 4 with the rows gone.
        assertFleetCount(fleet, 2);

        // (3) The chain refuses at its second level, and the whole delete rolls back with it: the trip
        // is still there, and so is the leg the cascade had ALREADY deleted when the note stopped it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(TRIPS + "/" + chained)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("still has Trip Leg Note records")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TRIPS + "/" + chained)
                                                 .then()
                                                 .statusCode(200));
        assertLegsOfTrip(chained, 2);
        // Nothing was counted away either - the rolled-back deletes recorded no events to act on.
        assertFleetCount(fleet, 2);

        // The author's alternative, at the master's own level: a printed copy refuses the delete
        // before the first child is touched, and the trip keeps its legs.
        int refusing = create(TRIPS, "{\"Note\":\"refusing\"}");
        create(LEGS, "{\"Trip\":" + refusing + ",\"Distance\":50,\"Fleet\":" + fleet + "}");
        create(COPIES, "{\"Trip\":" + refusing + ",\"Note\":\"printed\"}");
        assertFleetCount(fleet, 3);
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(TRIPS + "/" + refusing)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("still has Trip Copy records")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TRIPS + "/" + refusing)
                                                 .then()
                                                 .statusCode(200));
        assertLegsOfTrip(refusing, 1);
        assertFleetCount(fleet, 3);

        // ...and once the copy is gone the same delete goes through, children and count included -
        // so the refusal above was the declared decision and not a broken delete path.
        restAssuredExecutor.execute(() -> {
            int copy = given().when()
                              .get(COPIES + "?Trip=" + refusing)
                              .then()
                              .statusCode(200)
                              .body("", hasSize(1))
                              .extract()
                              .path("[0].Id");
            given().when()
                   .delete(COPIES + "/" + copy)
                   .then()
                   .statusCode(200);
        });
        delete(TRIPS + "/" + refusing, 200);
        assertGone(TRIPS + "/" + refusing);
        assertLegsOfTrip(refusing, 0);
        assertFleetCount(fleet, 2);
    }

    private void assertLegsOfTrip(int trip, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LEGS + "?Trip=" + trip)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(expected)));
    }

    private void assertGone(String path) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(path)
                                                 .then()
                                                 .statusCode(404));
    }

    private void assertFleetCount(int fleet, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(FLEETS + "/" + fleet)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("LegCount", equalTo(expected)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private void delete(String path, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(path)
                                                 .then()
                                                 .statusCode(expectedStatus));
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        writeIntent();
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

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
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
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
