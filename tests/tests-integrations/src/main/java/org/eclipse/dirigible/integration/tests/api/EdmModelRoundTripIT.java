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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@code .edm} must be a lossless source for the derived {@code .model} (#6826). Intent
 * Generate writes {@code <name>.edm} and a complete {@code <name>.model} together; the loss used to
 * happen when the {@code .edm} was used to rebuild the {@code .model} - opening a diagram and
 * saving it, which regenerates the model from the {@code .edm} via {@code transform-edm.js},
 * dropped every structured (List/Map) attribute (rollupGuard, checks, uniqueConstraints, ...)
 * because they never reached the {@code .edm}.
 *
 * <p>
 * This drives the real round-trip over HTTP, no browser: generate from an intent that exercises the
 * structured values, capture the intent's {@code .model} (the oracle), then save the {@code .edm}
 * back through the workspace API (which fires the same {@code ide-workspace-on-save} transform the
 * editor's save does) and assert the regenerated {@code .model} carries every structured value
 * intact. A structural diff of every entity/property key against the oracle then makes the
 * guarantee complete: a future structured attribute forgotten in the generator's set is dropped
 * from the {@code .edm} and caught here as a missing key, and one written but not parsed back is
 * caught as a value mismatch - so a structured attribute cannot ship without {@code .edm}
 * serialization support.
 *
 * <p>
 * Losslessness has a second failure mode the same round-trip exposes: not a value DROPPED, but a
 * value written TWICE and disagreeing with itself (#6883). A to-one relation's identity used to be
 * derived independently by the {@code <property>} element (from the TARGET entity) and by the
 * {@code <relation>} element (from the RELATION name), and the save applied the second over the
 * first - so an unmodified save renamed relationships and, for a cross-model target, repointed the
 * generated lookup URL at a controller that does not exist. That divergence was invisible to this
 * test because every relation in its intent was named after its target, which makes the two
 * derivations produce the same string. The fixture below deliberately breaks that coincidence -
 * relations named for their ROLE, two of them to the same target, and one cross-model target
 * published under a perspective that is not its entity name - and asserts the {@code .edm} is
 * internally consistent before the save is even attempted.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EdmModelRoundTripIT extends IntegrationTest {

    private static final String PROJECT = "edm-roundtrip-test";
    /** Owns the cross-model target, so its perspective is resolvable only from ITS model. */
    private static final String OWNER_PROJECT = "edm-roundtrip-refs";
    private static final String WORKSPACE = "workspace";
    private static final String WORKSPACE_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE;
    private static final String PROJECT_PATH = WORKSPACE_PATH + "/" + PROJECT;
    private static final String OWNER_PROJECT_PATH = WORKSPACE_PATH + "/" + OWNER_PROJECT;
    private static final String EDM_PUT_URL = "/services/ide/workspaces/" + WORKSPACE + "/" + PROJECT + "/rt.edm";

    // The owner of the cross-model target. Currency is a SETTING, so its owner publishes it under the
    // "Settings" perspective - a perspective that is not the entity's name, which is the only shape in
    // which the perspective half of #6883 is visible. Nothing but this model can say so, which is the
    // point: the consuming generator has to read it from here.
    private static final String OWNER_INTENT_YAML = """
            name: refs
            entities:
              - name: Currency
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string,  length: 3 }
                  - { name: name, type: string,  length: 100 }
            """;

    // A mostly same-model intent whose entities exercise the structured (List/Map) values #6826 was
    // losing: labelParts + uniqueConstraints + relatedEntities on Category, lookupColumns on Product's
    // FK, checks on Booking, and rollupGuard on Booking (the child of the capacity-bearing seat
    // roll-up).
    //
    // Product additionally carries the shapes #6883 needs, none of which existed here before: `owner`
    // and `reviewer` are named for their ROLE rather than for their target, so the target-derived and
    // relation-derived identities differ; they both point at the SAME target, so the target-derived
    // form is not even unique within the entity; and `currency` reaches into another model whose
    // perspective for the target ("Settings") is not the entity's name.
    private static final String INTENT_YAML = """
            name: rt
            uses:
              - { model: refs, project: edm-roundtrip-refs }
            entities:
              - name: Employee
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  length: 100 }

              - name: Category
                label: "{code} - {title}"
                unique:
                  - { fields: [code, title], message: "Code and title must be unique together" }
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: code,  type: string,  length: 20 }
                  - { name: title, type: string,  length: 100 }
                related:
                  - { entity: Product }

              - name: Product
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string,  length: 100 }
                  - { name: price, type: decimal }
                relations:
                  - { name: Category, kind: manyToOne, to: Category, show: [code, title] }
                  - { name: owner,    kind: manyToOne, to: Employee }
                  - { name: reviewer, kind: manyToOne, to: Employee }
                  - { name: currency, kind: manyToOne, to: Currency, model: refs }

              - name: Event
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: name,       type: string,  length: 100 }
                  - { name: capacity,   type: integer }
                  - { name: seatsTaken, type: integer }
                  - { name: seatsFree,  type: integer }

              - name: Booking
                checks:
                  - { kind: exactlyOne, fields: [seats, waitlistSeats], message: "Either a seat or a waitlist seat" }
                fields:
                  - { name: id,            type: integer, primaryKey: true, generated: true }
                  - { name: seats,         type: integer }
                  - { name: waitlistSeats, type: integer }
                relations:
                  - { name: Event, kind: manyToOne, to: Event, composition: true }

            rollups:
              - { name: eventSeats, entity: Booking, via: Event, field: seatsTaken,
                  op: sum, of: seats, capacity: capacity, balance: seatsFree }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void structured_values_survive_the_edm_to_model_round_trip() {
        // The owner of the cross-model target first: its .model is what the consumer's generation reads
        // the target's perspective from, and resolution fails loudly when it is not there.
        writeIntent(OWNER_PROJECT_PATH, OWNER_INTENT_YAML);
        generate(OWNER_PROJECT);

        writeIntent(PROJECT_PATH, INTENT_YAML);

        // 1. Generate: writes rt.edm AND the complete rt.model (the oracle).
        generate(PROJECT);

        String edm = contentOf("rt.edm");
        JsonObject modelFromIntent = parseModel(contentOf("rt.model"));

        // The generator must have written the structured values into the .edm (as JSON attributes),
        // otherwise there is nothing for the transform to read back. uniqueConstraints is excluded here:
        // it is owned by the composite-unique-key feature, which emits it as a <constraints> section, not
        // a JSON attribute.
        for (String key : new String[] {"rollupGuard", "checks", "labelParts", "relatedEntities", "lookupColumns"}) {
            assertTrue(edm.contains(key + "=\""), "the .edm must carry the structured value [" + key + "] as an attribute");
        }

        // The .edm must not contradict itself: the <relation> element restates the naming its FK
        // <property> element carries, and a save applies the former over the latter. Structural over
        // every relation in the file, so this holds for relation shapes no fixture here has yet (#6883).
        assertEdmRelationsAgreeWithTheirProperties(edm);

        // A relationship identity is unique within its owner - it is what the .schema emits as an FK's
        // constraintName. The target-derived form was not: Product's `owner` and `reviewer` both point
        // at Employee and both used to claim "Product_Employee".
        assertRelationshipNamesAreUniquePerEntity(modelFromIntent);
        assertRelationshipIdentityConvention(modelFromIntent);

        // 2. Rebuild the .model FROM the .edm the way the editor's save does: delete the oracle, then
        // PUT the .edm back through the workspace API (fires the ide-workspace-on-save transform). The
        // regenerated rt.model existing at all proves the transform ran.
        byte[] edmBytes = resource("rt.edm").getContent();
        resource("rt.model").delete();
        restAssuredExecutor.execute(() -> given().contentType("application/octet-stream")
                                                 .body(edmBytes)
                                                 .when()
                                                 .put(EDM_PUT_URL)
                                                 .then()
                                                 .statusCode(200));

        assertTrue(resource("rt.model").exists(), "the .edm -> .model transform must have regenerated rt.model");
        JsonObject modelFromEdm = parseModel(contentOf("rt.model"));

        // 3. Every structured value present in the intent's .model must survive intact in the one
        // rebuilt from the .edm - the acceptance criterion "the .model generated from the .edm equals
        // the .model from intent" for exactly the values that were being dropped.
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Category", "labelParts");
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Category", "relatedEntities");
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Booking", "checks");
        assertEntityStructuredEquals(modelFromIntent, modelFromEdm, "Booking", "rollupGuard");
        assertPropertyStructuredEquals(modelFromIntent, modelFromEdm, "Product", "Category", "lookupColumns");

        // uniqueConstraints is owned by the composite-unique-key feature (a <constraints> section, not a
        // JSON attribute); this fix must not double it. Guard against the duplication that would occur if
        // both mechanisms wrote it: exactly one entry after the round-trip.
        JsonElement uniqueConstraints = entity(modelFromEdm, "Category").get("uniqueConstraints");
        assertNotNull(uniqueConstraints, "uniqueConstraints must survive the round-trip (via the <constraints> feature)");
        assertEquals(1, uniqueConstraints.getAsJsonArray()
                                         .size(),
                "uniqueConstraints must appear exactly once - not duplicated by both the feature and this fix");

        // The headline symptom: rollupGuard is a non-empty object on Booking after the round-trip.
        JsonElement rollupGuard = entity(modelFromEdm, "Booking").get("rollupGuard");
        assertNotNull(rollupGuard, "rollupGuard must survive the save-regenerate cycle");
        assertTrue(rollupGuard.isJsonObject() && rollupGuard.getAsJsonObject()
                                                            .size() > 0,
                "rollupGuard must be a populated object, not an empty or stringified value");

        // 4. The real "future attributes cannot ship without .edm serialization" guard: every
        // entity/property key the intent's .model carries must survive intact in the one rebuilt from the
        // .edm. A new structured attribute forgotten in the generator's set is dropped from the .edm and
        // caught here as a MISSING key; one written but not parsed back is caught as a VALUE mismatch
        // (object vs JSON string). Comparing parsed structure - not pattern-matching for {/[ - means a
        // scalar that legitimately IS a JSON string (e.g. widgetDependsOnValueCases) is compared
        // value-to-value and never falsely flagged.
        assertEveryKeySurvives(modelFromIntent, modelFromEdm);
    }

    /**
     * The {@code .edm} must not contradict itself. A to-one relation's identity and its lookup
     * perspective are written twice - on the FK {@code <property>} element and again on the top-level
     * {@code <relation>} element - and the save applies the {@code <relation>}'s copy over the
     * property's, so a disagreement IS a silent rename on the first save (#6883). Asserted structurally
     * over every relation in the file rather than as a list of known pairs, so a relation shape no
     * fixture here has yet is covered too.
     */
    private void assertEdmRelationsAgreeWithTheirProperties(String edm) {
        Document document = parseXml(edm);
        NodeList relations = document.getElementsByTagName("relation");
        assertTrue(relations.getLength() > 0, "fixture precondition: the .edm should carry <relation> elements");
        for (int i = 0; i < relations.getLength(); i++) {
            Element relation = (Element) relations.item(i);
            String entityName = relation.getAttribute("entity");
            String propertyName = relation.getAttribute("property");
            Element property = edmProperty(document, entityName, propertyName);
            String where = "the .edm's <relation> for [" + entityName + "." + propertyName + "] disagrees with its <property> on ";
            // `name` is the attribute transform-edm.js reads; `relationName` is its twin, kept in step.
            assertEquals(property.getAttribute("relationshipName"), relation.getAttribute("name"),
                    where + "the relationship name - the save applies the <relation>'s value, renaming the relationship");
            assertEquals(property.getAttribute("relationshipName"), relation.getAttribute("relationName"),
                    where + "the relationship name (relationName)");
            assertEquals(property.getAttribute("relationshipEntityPerspectiveName"),
                    relation.getAttribute("relationshipEntityPerspectiveName"),
                    where + "the lookup perspective - the save applies the <relation>'s value, repointing the generated lookup URL");
            assertEquals(property.getAttribute("relationshipEntityName"), relation.getAttribute("referenced"),
                    where + "the referenced entity");
        }
    }

    /** The {@code <property>} element of the named entity, or a failure naming what was missing. */
    private Element edmProperty(Document document, String entityName, String propertyName) {
        NodeList entities = document.getElementsByTagName("entity");
        for (int i = 0; i < entities.getLength(); i++) {
            Element entity = (Element) entities.item(i);
            if (!entityName.equals(entity.getAttribute("name"))) {
                continue;
            }
            NodeList properties = entity.getElementsByTagName("property");
            for (int j = 0; j < properties.getLength(); j++) {
                Element property = (Element) properties.item(j);
                if (propertyName.equals(property.getAttribute("name"))) {
                    return property;
                }
            }
            throw new AssertionError("the .edm's <relation> names property [" + propertyName + "] of [" + entityName
                    + "], which that entity does not declare");
        }
        throw new AssertionError("the .edm's <relation> names entity [" + entityName + "], which the .edm does not declare");
    }

    /**
     * A relationship identity is unique within its owning entity: it is what the generated
     * {@code .schema} emits as the foreign key's {@code constraintName}. Deriving it from the TARGET
     * collapses every relation an entity has to the same target onto one name - Product's {@code owner}
     * and {@code reviewer} both point at Employee (#6883).
     */
    private void assertRelationshipNamesAreUniquePerEntity(JsonObject model) {
        for (JsonElement e : model.getAsJsonObject("model")
                                  .getAsJsonArray("entities")) {
            JsonObject entity = e.getAsJsonObject();
            JsonArray properties = entity.getAsJsonArray("properties");
            if (properties == null) {
                continue;
            }
            Set<String> seen = new HashSet<>();
            for (JsonElement p : properties) {
                JsonObject property = p.getAsJsonObject();
                if (!property.has("relationshipEntityName") || !property.has("relationshipName")) {
                    continue;
                }
                String name = property.get("relationshipName")
                                      .getAsString();
                assertTrue(seen.add(name), "[" + entity.get("name")
                                                       .getAsString()
                        + "] gives two relationships the same identity [" + name + "] - the .schema then emits two foreign keys"
                        + " claiming the same constraintName");
            }
        }
    }

    /**
     * The relationship identity and the cross-model lookup perspective the generator must settle on:
     * the RELATION's name (unique within the owner, unlike the target's) and the perspective the
     * target's OWNER model publishes it under (the only one the generated lookup URL can match). Pinned
     * explicitly because the internal-consistency assertion above would also pass if both writers
     * agreed on the wrong value.
     */
    private void assertRelationshipIdentityConvention(JsonObject model) {
        JsonObject product = entity(model, "Product");
        assertEquals("Product_Owner", property(product, "Owner").get("relationshipName")
                                                                .getAsString(),
                "a relationship is identified by its own name, not by its target");
        assertEquals("Product_Reviewer", property(product, "Reviewer").get("relationshipName")
                                                                      .getAsString(),
                "a second relation to the same target gets its own identity");
        assertEquals("Settings", property(product, "Currency").get("relationshipEntityPerspectiveName")
                                                              .getAsString(),
                "a cross-model target's perspective comes from its OWNER model, where Currency is a Setting");
    }

    /** External entities and DTDs off: this parses generated content, and nothing here needs them. */
    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder()
                          .parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new AssertionError("the generated .edm is not well-formed XML", ex);
        }
    }

    private void assertEntityStructuredEquals(JsonObject fromIntent, JsonObject fromEdm, String entityName, String key) {
        JsonElement expected = entity(fromIntent, entityName).get(key);
        assertNotNull(expected, "fixture precondition: [" + entityName + "] should declare [" + key + "] in the intent's .model");
        JsonElement actual = entity(fromEdm, entityName).get(key);
        assertNotNull(actual, "[" + entityName + "] lost [" + key + "] in the .edm -> .model round-trip");
        assertEquals(expected, actual, "[" + entityName + "]." + key + " changed across the .edm -> .model round-trip");
    }

    private void assertPropertyStructuredEquals(JsonObject fromIntent, JsonObject fromEdm, String entityName, String propertyName,
            String key) {
        JsonElement expected = property(entity(fromIntent, entityName), propertyName).get(key);
        assertNotNull(expected,
                "fixture precondition: [" + entityName + "." + propertyName + "] should declare [" + key + "] in the intent's .model");
        JsonElement actual = property(entity(fromEdm, entityName), propertyName).get(key);
        assertNotNull(actual, "[" + entityName + "." + propertyName + "] lost [" + key + "] in the .edm -> .model round-trip");
        assertEquals(expected, actual, "[" + entityName + "." + propertyName + "]." + key + " changed across the round-trip");
    }

    /**
     * Keys another mechanism owns and round-trips differently, so they are compared by their own tests
     * rather than here: {@code uniqueConstraints} is emitted and rebuilt by the composite-unique-key
     * feature's {@code <constraints>} section (a shape without {@code properties}), and this fix
     * deliberately does not touch it (see {@code EdmIntentGenerator.STRUCTURED_ATTRIBUTES} and
     * {@code transform-edm.js} {@code ENTITY_STRUCTURED}). Its non-duplication is asserted separately
     * above.
     */
    private static final java.util.Set<String> ROUND_TRIP_EXCEPTIONS = java.util.Set.of("uniqueConstraints");

    /**
     * Every entity/property key in the intent's .model must survive intact in the one rebuilt from the
     * .edm.
     */
    private void assertEveryKeySurvives(JsonObject fromIntent, JsonObject fromEdm) {
        for (JsonElement e : fromIntent.getAsJsonObject("model")
                                       .getAsJsonArray("entities")) {
            JsonObject oracle = e.getAsJsonObject();
            String name = oracle.get("name")
                                .getAsString();
            JsonObject actual = entity(fromEdm, name);
            for (Map.Entry<String, JsonElement> member : oracle.entrySet()) {
                String key = member.getKey();
                if (ROUND_TRIP_EXCEPTIONS.contains(key)) {
                    continue;
                }
                if ("properties".equals(key)) {
                    assertPropertiesSurvive(member.getValue()
                                                  .getAsJsonArray(),
                            actual, name);
                    continue;
                }
                assertTrue(actual.has(key), "[" + name + "] lost attribute [" + key + "] in the .edm -> .model round-trip");
                assertEquals(member.getValue(), actual.get(key), "[" + name + "]." + key + " changed across the .edm -> .model round-trip");
            }
        }
    }

    private void assertPropertiesSurvive(JsonArray oracleProperties, JsonObject actualEntity, String entityName) {
        for (JsonElement pe : oracleProperties) {
            JsonObject oracleProperty = pe.getAsJsonObject();
            String propertyName = oracleProperty.get("name")
                                                .getAsString();
            JsonObject actualProperty = property(actualEntity, propertyName);
            for (Map.Entry<String, JsonElement> member : oracleProperty.entrySet()) {
                String key = member.getKey();
                if (ROUND_TRIP_EXCEPTIONS.contains(key)) {
                    continue;
                }
                assertTrue(actualProperty.has(key),
                        "[" + entityName + "." + propertyName + "] lost attribute [" + key + "] in the round-trip");
                assertEquals(member.getValue(), actualProperty.get(key),
                        "[" + entityName + "." + propertyName + "]." + key + " changed across the round-trip");
            }
        }
    }

    private JsonObject entity(JsonObject model, String name) {
        for (JsonElement e : model.getAsJsonObject("model")
                                  .getAsJsonArray("entities")) {
            JsonObject entity = e.getAsJsonObject();
            if (name.equals(entity.get("name")
                                  .getAsString())) {
                return entity;
            }
        }
        throw new AssertionError("entity [" + name + "] not found in the model");
    }

    private JsonObject property(JsonObject entity, String name) {
        for (JsonElement p : entity.getAsJsonArray("properties")) {
            JsonObject property = p.getAsJsonObject();
            if (name.equals(property.get("name")
                                    .getAsString())) {
                return property;
            }
        }
        throw new AssertionError("property [" + name + "] not found on entity [" + entity.get("name")
                                                                                         .getAsString()
                + "]");
    }

    private JsonObject parseModel(String json) {
        return JsonParser.parseString(json)
                         .getAsJsonObject();
    }

    private void writeIntent(String projectPath, String yaml) {
        String path = projectPath + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generate(String project) {
        String url = "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + project + "&path=app.intent";
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(url)
                                                 .then()
                                                 .statusCode(200));
    }

    private IResource resource(String fileName) {
        return repository.getResource(PROJECT_PATH + "/" + fileName);
    }

    private String contentOf(String fileName) {
        return new String(resource(fileName).getContent(), StandardCharsets.UTF_8);
    }

    @AfterEach
    void cleanup() {
        for (String path : new String[] {PROJECT_PATH, OWNER_PROJECT_PATH}) {
            if (repository.hasCollection(path)) {
                repository.removeCollection(path);
            }
        }
    }
}
