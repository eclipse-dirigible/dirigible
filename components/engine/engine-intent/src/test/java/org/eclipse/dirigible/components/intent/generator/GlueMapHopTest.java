/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A one-hop {@code relation.field} source in a create-from's {@code map:}.
 *
 * <p>
 * The defect it closes: {@code map:} could copy only a property of the source ROW, so a value one
 * relation out was inexpressible - and the values an audit log most needs are exactly those. The
 * reported shape is this fixture's: a log of vehicle checks that must record the plate number the
 * check was made against, where the plate lives on Vehicle and the log is written from Fine.
 *
 * <p>
 * Holding the relation instead and displaying through it is not the same thing. That reads the
 * CURRENT value, so correcting a plate typo silently rewrites every past log row; a map copies the
 * value that was true when the row was written, which is what an append-only log means by a value.
 *
 * <p>
 * The mechanism is not new: it is the load-by-foreign-key a notification's {@code relation.field}
 * recipient has always used, so a hop costs one query per distinct relation and reads through the
 * same null guard.
 */
class GlueMapHopTest {

    private static final String YAML = """
            name: fines
            entities:
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Vehicle
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: plateNumber, type: string, length: 16 }
                  - { name: colour, type: string, length: 16 }
                relations:
                  - { name: owner, kind: manyToOne, to: Driver }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: violationAt, type: timestamp }
                relations:
                  - { name: vehicle, kind: manyToOne, to: Vehicle }
              - name: FineLog
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: plateNumberChecked, type: string, length: 16 }
                  - { name: colourChecked, type: string, length: 16 }
                  - { name: violationAtChecked, type: timestamp }
                  - { name: event, type: string, length: 32 }
                relations:
                  - { name: fine, kind: manyToOne, to: Fine }
            generates:
              - name: log-the-check
                from: Fine
                to: FineLog
                map:
                  fine: id
                  plateNumberChecked: vehicle.plateNumber
                  violationAtChecked: violationAt
                defaults:
                  event: VEHICLE_CHECKED
            """;

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> assignmentsOf(String yaml) {
        return (List<Map<String, Object>>) generateOf(yaml).get("fieldAssignments");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadsOf(String yaml) {
        return (List<Map<String, Object>>) generateOf(yaml).get("relationLoads");
    }

    private static Map<String, Object> generateOf(String yaml) {
        return GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(yaml))
                                  .get(0);
    }

    /** The expression of the assignment writing the given target property. */
    private static String expressionFor(String yaml, String targetProp) {
        return assignmentsOf(yaml).stream()
                                  .filter(a -> targetProp.equals(a.get("targetProp")))
                                  .map(a -> String.valueOf(a.get("expr")))
                                  .findFirst()
                                  .orElseThrow(
                                          () -> new AssertionError("no assignment for [" + targetProp + "] in " + assignmentsOf(yaml)));
    }

    /**
     * The hop reads the LOADED row, not the source: {@code source.Vehicle} is the foreign key integer,
     * so walking it as if it were an object is the thing that could never work. The null guard is not
     * defensive dressing - a nullable relation is a legitimate empty value, and a Fine with no vehicle
     * must still write its log row.
     */
    @Test
    void readsTheHopOffTheLoadedRowThroughANullGuard() {
        assertEquals("(vehicle == null ? null : vehicle.PlateNumber)", expressionFor(YAML, "PlateNumberChecked"));
    }

    /** A direct property is still read off the source row the create-from already holds. */
    @Test
    void keepsADirectPropertyOnTheSourceRow() {
        assertEquals("source.ViolationAt", expressionFor(YAML, "ViolationAtChecked"));
    }

    /** A defaults entry is untouched by any of this - it never reads the source at all. */
    @Test
    void leavesADefaultALiteral() {
        assertEquals("\"VEHICLE_CHECKED\"", expressionFor(YAML, "Event"));
    }

    /**
     * The load the template emits: the relation's own name as the local, and the PascalCase foreign key
     * property to read it by. Both come from the relation, so the template decides nothing.
     */
    @Test
    void declaresOneLoadForTheRelationItHopsThrough() {
        List<Map<String, Object>> loads = loadsOf(YAML);
        assertEquals(1, loads.size(), "got: " + loads);
        assertEquals("vehicle", loads.get(0)
                                     .get("local"));
        assertEquals("Vehicle", loads.get(0)
                                     .get("targetEntity"));
        assertEquals("Vehicle", loads.get(0)
                                     .get("fkProperty"));
        assertEquals(false, loads.get(0)
                                 .get("crossModel"));
    }

    /**
     * Two fields off the same relation share ONE load. Worth pinning: the obvious implementation emits
     * a query per mapped field, and a log that snapshots four columns of a vehicle would then read that
     * vehicle four times per row written.
     */
    @Test
    void readsTwoFieldsOfOneRelationThroughASingleLoad() {
        String yaml =
                YAML.replace("violationAtChecked: violationAt", "colourChecked: vehicle.colour\n      violationAtChecked: violationAt");
        assertEquals(1, loadsOf(yaml).size(), "got: " + loadsOf(yaml));
        assertEquals("(vehicle == null ? null : vehicle.Colour)", expressionFor(yaml, "ColourChecked"));
        assertEquals("(vehicle == null ? null : vehicle.PlateNumber)", expressionFor(yaml, "PlateNumberChecked"));
    }

    /**
     * A create-from that hops nowhere carries an empty list, not a null. An undefined Velocity
     * reference renders as its own name, so a template must never be handed absence.
     */
    @Test
    void aCreateFromWithoutAHopCarriesAnEmptyListOfLoads() {
        String yaml = YAML.replace("plateNumberChecked: vehicle.plateNumber", "plateNumberChecked: id");
        assertTrue(loadsOf(yaml).isEmpty(), "got: " + loadsOf(yaml));
    }

    /**
     * A second hop is refused. It would need a load off a loaded row - and the value two relations out
     * belongs in a field of the entity in between, which is where the model can keep it correct.
     */
    @Test
    void rejectsAMultiHopPath() {
        assertIssue(YAML.replace("vehicle.plateNumber", "vehicle.owner.name"), "multi-hop path");
    }

    /**
     * The last step must be a field. A relation there would copy Vehicle's foreign key into a column
     * whose own relation points elsewhere - a key from the wrong numbering space, which nothing can
     * read back, and which no type error would catch (both are integers).
     */
    @Test
    void rejectsAHopWhoseLastStepIsARelation() {
        assertIssue(YAML.replace("plateNumberChecked: vehicle.plateNumber", "plateNumberChecked: vehicle.owner"),
                "is a relation of [Vehicle], not a field");
    }

    /** The head must be a to-one relation of the source. */
    @Test
    void rejectsAHopThroughSomethingThatIsNotAToOneRelation() {
        assertIssue(YAML.replace("vehicle.plateNumber", "violationAt.plateNumber"), "[violationAt] is not a to-one relation of [Fine]");
    }

    /** And the tail must exist on the entity the relation points at. */
    @Test
    void rejectsAHopOntoAnUnknownFieldOfTheRelatedEntity() {
        assertIssue(YAML.replace("vehicle.plateNumber", "vehicle.plate"), "[plate] is not a field of [Vehicle]");
    }

    /**
     * An {@code items} map is refused, with the reason. Its source is the item row being cloned, so the
     * load would have to run once per row inside the clone loop - a different shape from the single
     * load a create-from emits, and one nothing generates yet.
     */
    @Test
    void rejectsAHopInAnItemsMap() {
        String yaml = """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: QuoteItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: quote, kind: manyToOne, to: Quote, composition: true, required: true }
                      - { name: customer, kind: manyToOne, to: Customer }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal, precision: 18, scale: 2 }
                      - { name: customerName, type: string }
                    relations:
                      - { name: order, kind: manyToOne, to: Order, composition: true, required: true }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    items:
                      from: QuoteItem
                      to: OrderItem
                      map:
                        amount: amount
                        customerName: customer.name
                """;
        assertIssue(yaml, "an items map does not support a hop");
    }

    /**
     * A schedule's generate maps the row it queried, so the same hop applies there - and the loads
     * reach the job template through the same key, for either of its two actions.
     */
    @Test
    void resolvesTheHopForAScheduledGenerationToo() {
        String yaml = YAML.replace("""
                generates:
                  - name: log-the-check
                    from: Fine
                    to: FineLog
                    map:
                      fine: id
                      plateNumberChecked: vehicle.plateNumber
                      violationAtChecked: violationAt
                    defaults:
                      event: VEHICLE_CHECKED
                """, """
                schedules:
                  - name: log-every-fine
                    entity: Fine
                    cron: "0 0 3 * * ?"
                    generate:
                      to: FineLog
                      map:
                        fine: id
                        plateNumberChecked: vehicle.plateNumber
                """);
        Map<String, Object> schedule = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                          .get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assignments = (List<Map<String, Object>>) schedule.get("genFieldAssignments");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> loads = (List<Map<String, Object>>) schedule.get("relationLoads");
        assertEquals(1, loads.size(), "got: " + loads);
        assertTrue(assignments.stream()
                              .anyMatch(a -> "(vehicle == null ? null : vehicle.PlateNumber)".equals(a.get("expr"))),
                "got: " + assignments);
    }

    /**
     * A relation whose name is one the generated create-from already uses for a local of its own. The
     * load is declared in a local named after the RELATION - the shared resolver embeds that name in
     * the read it renders - so {@code source} would emit a second declaration of a name already in
     * scope. That is a Java compile error on the generated file: loud, but with nothing explaining it,
     * which is why the collision is named here instead. {@code map:} without a hop is unaffected - the
     * relation only becomes a local when something reads THROUGH it.
     */
    @Test
    void refusesAHopThroughARelationNamedLikeAGeneratedLocal() {
        String yaml = YAML.replace("- { name: vehicle, kind: manyToOne, to: Vehicle }", "- { name: source, kind: manyToOne, to: Vehicle }")
                          .replace("plateNumberChecked: vehicle.plateNumber", "plateNumberChecked: source.plateNumber");
        List<Map<String, Object>> generates = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(yaml));
        assertTrue(generates.isEmpty(), "a colliding hop must drop the create-from rather than emit uncompilable Java, got: " + generates);
    }

    /**
     * Case matters, because Java locals do: a relation called {@code Vehicle} is not the local
     * {@code vehicle}, and refusing it would reject a model that compiles perfectly well.
     */
    @Test
    void allowsARelationWhoseNameOnlyDiffersFromALocalByCase() {
        String yaml = YAML.replace("- { name: vehicle, kind: manyToOne, to: Vehicle }", "- { name: Source, kind: manyToOne, to: Vehicle }")
                          .replace("plateNumberChecked: vehicle.plateNumber", "plateNumberChecked: Source.plateNumber");
        assertEquals("(Source == null ? null : Source.PlateNumber)", expressionFor(yaml, "PlateNumberChecked"));
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
    }
}
