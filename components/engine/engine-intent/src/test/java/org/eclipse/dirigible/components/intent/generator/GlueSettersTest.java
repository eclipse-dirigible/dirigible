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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code setters} glue collection's {@code {error}} handling - dirigible #6762: a
 * {@code setField} whose value is the whole-value {@code {error}} token carries
 * {@code errorMessage: true} (the template then reads the failure-message process variable instead
 * of assigning a literal), and every other setter's descriptor stays exactly as before, so
 * already-written {@code .glue} files render unchanged.
 *
 * <p>
 * ...and its erasure twin - dirigible #7386: a {@code clearField} step is a setter like any other,
 * flagged {@code clear: true} so the template assigns {@code null} instead of a literal.
 *
 * <p>
 * ...and its stamp - dirigible #7873: a {@code setField} whose value is the whole-value
 * {@code {now}} token carries {@code now: true} and the {@code java.time} expression of the field's
 * Java type, so the template assigns the moment the step runs.
 *
 * <p>
 * ...and its one hop - dirigible #7934: a setter whose path goes through a to-one relation
 * ({@code plan.Status}) carries {@code via} and the target's coordinates, so the template writes
 * through the target's repository; a setter of the trigger record gains none of those keys.
 *
 * <p>
 * ...and its several members - dirigible #8042: a {@code set:} map carries one {@code assignments}
 * entry per member, in the authored order, each with the keys its single-member setter would carry,
 * so the template writes them in one {@code updateProperties}; a one-key map is that key's
 * single-member setter.
 *
 * <p>
 * ...and a status a roll-up holds - dirigible #7869: a status setter of a capacity roll-up's parent
 * carries the statuses that roll-up owns and the column it restores from, so the template writes
 * the step's status there while the row is settled - for the trigger record and one hop alike.
 */
class GlueSettersTest {

    private static final String YAML =
            """
                    name: provisioning
                    entities:
                      - name: ProvisioningStatus
                        function: Setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: Plan
                        fields:
                          - { name: id, type: long, primaryKey: true, generated: true }
                          - { name: label, type: string }
                          - { name: price, type: decimal }
                          - { name: charged, type: decimal }
                        relations:
                          - { name: Status, kind: manyToOne, to: ProvisioningStatus, function: EntityStatus, init: 1 }
                      - name: TenantApplication
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: failureMessage, type: string }
                          - { name: state, type: string }
                          - { name: provisionedAt, type: timestamp }
                          - { name: provisionedOn, type: date }
                          - { name: fee, type: decimal }
                          - { name: paid, type: decimal }
                        relations:
                          - { name: Status, kind: manyToOne, to: ProvisioningStatus, function: EntityStatus, init: 1 }
                          - { name: plan, kind: manyToOne, to: Plan }
                      - name: Charge
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: amount, type: decimal }
                        relations:
                          - { name: application, kind: manyToOne, to: TenantApplication }
                          - { name: plan, kind: manyToOne, to: Plan }
                    rollups:
                      - { name: appPaid, entity: Charge, via: application, field: paid, op: sum, of: amount,
                          capacity: fee, status: Status, statusWhenFull: 5, statusWhenPartial: 4 }
                      - { name: planCharged, entity: Charge, via: plan, field: charged, op: sum, of: amount,
                          capacity: price, status: Status, statusWhenFull: 7, statusWhenPartial: 6 }
                    processes:
                      - name: TenantProvisioning
                        trigger: { onCreate: TenantApplication }
                        steps:
                          - { name: provisionApp, kind: serviceTask, args: { delegate: custom.AppProvisioner, onError: recordFailure, next: markDone } }
                          - { name: markDone, kind: serviceTask, args: { setField: state, value: DONE, next: end } }
                          - { name: recordFailure, kind: serviceTask, args: { setField: failureMessage, value: "{error}", next: markFailed } }
                          - { name: markFailed, kind: serviceTask, args: { setRelationField: Status, value: 3, next: end } }
                          - { name: resetFailure, kind: serviceTask, args: { clearField: failureMessage, next: end } }
                          - { name: stampMoment, kind: serviceTask, args: { setField: provisionedAt, value: "{now}", next: end } }
                          - { name: stampDay, kind: serviceTask, args: { setField: provisionedOn, value: "{now}", next: end } }
                          - { name: activatePlan, kind: serviceTask, args: { setRelationField: plan.Status, value: 2, next: end } }
                          - { name: labelPlan, kind: serviceTask, args: { setField: plan.label, value: ACTIVE, next: end } }
                          - { name: restart, kind: serviceTask, args: { set: { failureMessage: null, Status: 2, state: 'RUNNING "again"' }, next: end } }
                          - { name: provisionPlan, kind: serviceTask, args: { delegate: custom.PlanProvisioner, onError: failAll, next: end } }
                          - { name: failAll, kind: serviceTask, args: { set: { failureMessage: "{error}", provisionedAt: "{now}", Status: 3 }, next: end } }
                          - { name: openPlan, kind: serviceTask, args: { set: { plan.Status: 2, plan.label: ACTIVE }, next: end } }
                          - { name: markDoneOnce, kind: serviceTask, args: { set: { state: DONE }, next: end } }
                    """;

    @Test
    void anErrorValueSetterCarriesTheErrorMessageFlag() {
        Map<String, Object> setter = setter("TenantProvisioningRecordFailure");

        assertEquals("true", setter.get("errorMessage"), "the {error} setter must be flagged: " + setter);
        assertEquals("{error}", setter.get("value"));
        assertEquals("FailureMessage", setter.get("field"));
    }

    /** A literal setter's descriptor is untouched, so pre-existing .glue files render unchanged. */
    @Test
    void aLiteralSetterCarriesNoErrorMessageKey() {
        Map<String, Object> setter = setter("TenantProvisioningMarkDone");

        assertFalse(setter.containsKey("errorMessage"), "a literal setter must stay exactly as before: " + setter);
        assertEquals("DONE", setter.get("value"));
    }

    @Test
    void aRelationSetterCarriesNoErrorMessageKey() {
        Map<String, Object> setter = setter("TenantProvisioningMarkFailed");

        assertFalse(setter.containsKey("errorMessage"), "a relation setter assigns a seed id, never a message: " + setter);
        assertEquals("true", setter.get("relation"));
    }

    /** The erasure is a setter of the same collection - one descriptor, flagged. */
    @Test
    void aClearFieldSetterCarriesTheClearFlagAndNoValue() {
        Map<String, Object> setter = setter("TenantProvisioningResetFailure");

        assertEquals("true", setter.get("clear"), "the erasure must be flagged: " + setter);
        assertEquals("FailureMessage", setter.get("field"));
        assertEquals("false", setter.get("relation"));
        assertEquals("", setter.get("value"), "a clearField assigns nothing: " + setter);
        assertFalse(setter.containsKey("errorMessage"), "an erasure reads no failure message: " + setter);
    }

    /** A literal setter's descriptor gains no key, so pre-existing .glue files render unchanged. */
    @Test
    void aLiteralSetterCarriesNoClearKey() {
        assertFalse(setter("TenantProvisioningMarkDone").containsKey("clear"),
                "a literal setter must stay exactly as before: " + setter("TenantProvisioningMarkDone"));
    }

    @Test
    void nowSetterOnATimestampIsFlaggedWithInstant() {
        Map<String, Object> setter = setter("TenantProvisioningStampMoment");

        assertEquals("true", setter.get("now"), "the {now} setter must be flagged: " + setter);
        assertEquals("java.time.Instant.now()", setter.get("nowExpression"));
        assertEquals("ProvisionedAt", setter.get("field"));
        assertEquals("false", setter.get("relation"));
    }

    @Test
    void nowSetterOnADateIsFlaggedWithLocalDate() {
        Map<String, Object> setter = setter("TenantProvisioningStampDay");

        assertEquals("true", setter.get("now"), "the {now} setter must be flagged: " + setter);
        assertEquals("java.time.LocalDate.now()", setter.get("nowExpression"));
        assertEquals("ProvisionedOn", setter.get("field"));
    }

    /** A literal setter's descriptor gains no key, so pre-existing .glue files render unchanged. */
    @Test
    void literalSetterDescriptorIsUntouched() {
        Map<String, Object> setter = setter("TenantProvisioningMarkDone");

        assertFalse(setter.containsKey("now"), "a literal setter must stay exactly as before: " + setter);
        assertFalse(setter.containsKey("nowExpression"), "a literal setter must stay exactly as before: " + setter);
    }

    @Test
    void aOneHopRelationSetterCarriesTheHopAndTheTargetKeys() {
        Map<String, Object> setter = setter("TenantProvisioningActivatePlan");

        assertEquals("Plan", setter.get("via"), "the FK the target is reached through: " + setter);
        assertEquals("Plan", setter.get("targetEntity"));
        assertEquals("Plan", setter.get("targetPerspective"));
        assertEquals("longValue", setter.get("targetKeyAccessor"), "the TARGET's key type, not the trigger's: " + setter);
        assertEquals("TenantApplication", setter.get("entity"), "the trigger row is still the one the key loads: " + setter);
        assertEquals("intValue", setter.get("keyAccessor"));
        assertEquals("Status", setter.get("field"));
        assertEquals("2", setter.get("value"));
        assertEquals("true", setter.get("relation"));
    }

    @Test
    void aOneHopFieldSetterCarriesTheHop() {
        Map<String, Object> setter = setter("TenantProvisioningLabelPlan");

        assertEquals("Plan", setter.get("via"));
        assertEquals("Label", setter.get("field"));
        assertEquals("ACTIVE", setter.get("value"));
        assertEquals("false", setter.get("relation"));
    }

    /** A setter of the trigger record gains no key, so pre-existing .glue files render unchanged. */
    @Test
    void aPlainSetterCarriesNoHopKeys() {
        for (String className : List.of("TenantProvisioningMarkDone", "TenantProvisioningMarkFailed", "TenantProvisioningResetFailure",
                "TenantProvisioningStampMoment")) {
            Map<String, Object> setter = setter(className);
            for (String key : List.of("via", "targetEntity", "targetPerspective", "targetKeyAccessor")) {
                assertFalse(setter.containsKey(key), "a plain setter must stay exactly as before: " + setter);
            }
        }
    }

    @Test
    void aSetMapIsOneSetterWithItsMembersInTheAuthoredOrder() {
        Map<String, Object> setter = setter("TenantProvisioningRestart");
        List<Map<String, Object>> assignments = assignments(setter);

        assertEquals(List.of("FailureMessage", "Status", "State"), assignments.stream()
                                                                              .map(assignment -> assignment.get("field"))
                                                                              .toList());
        assertEquals("true", assignments.get(0)
                                        .get("clear"),
                "null is the erasure: " + assignments);
        assertEquals("true", assignments.get(1)
                                        .get("relation"));
        assertEquals("2", assignments.get(1)
                                     .get("value"));
        assertEquals("RUNNING \"again\"", assignments.get(2)
                                                     .get("value"));
        assertFalse(setter.containsKey("field"), "the members are the assignments, not a single field: " + setter);
        assertFalse(setter.containsKey("via"), "a set: map of the trigger record carries no hop: " + setter);
    }

    @Test
    void aSetMapWithErrorAndNow() {
        List<Map<String, Object>> assignments = assignments(setter("TenantProvisioningFailAll"));

        assertEquals("true", assignments.get(0)
                                        .get("errorMessage"));
        assertEquals("true", assignments.get(1)
                                        .get("now"));
        assertEquals("java.time.Instant.now()", assignments.get(1)
                                                           .get("nowExpression"));
        assertEquals("3", assignments.get(2)
                                     .get("value"));
    }

    @Test
    void aOneHopSetMapWritesTheTarget() {
        Map<String, Object> setter = setter("TenantProvisioningOpenPlan");

        assertEquals("Plan", setter.get("via"));
        assertEquals("Plan", setter.get("targetEntity"));
        assertEquals("longValue", setter.get("targetKeyAccessor"));
        assertEquals(List.of("Status", "Label"), assignments(setter).stream()
                                                                    .map(assignment -> assignment.get("field"))
                                                                    .toList());
    }

    /** A one-key map is its key's single-member setter: the same descriptor, so the same delegate. */
    @Test
    void aOneKeySetMapIsTheSingleMemberSetter() {
        Map<String, Object> single = new java.util.LinkedHashMap<>(setter("TenantProvisioningMarkDone"));
        Map<String, Object> set = new java.util.LinkedHashMap<>(setter("TenantProvisioningMarkDoneOnce"));
        single.remove("className");
        set.remove("className");

        assertEquals(single, set);
    }

    @Test
    void aStatusSetterOfARollupParentCarriesTheStatusesTheRollupHolds() {
        for (String className : List.of("TenantProvisioningMarkFailed", "TenantProvisioningRestart")) {
            Map<String, Object> setter = setter(className);
            assertEquals("Status", setter.get("rollupStatus"), "the status the roll-up drives: " + setter);
            assertEquals("DisplacedStatus", setter.get("rollupDisplacedStatus"), "the column the roll-up restores from: " + setter);
            assertEquals("5, 4", setter.get("rollupOwnedStatuses"), "statusWhenFull, statusWhenPartial: " + setter);
        }
    }

    @Test
    void aOneHopStatusSetterCarriesTheTargetsRollupStatuses() {
        for (String className : List.of("TenantProvisioningActivatePlan", "TenantProvisioningOpenPlan")) {
            assertEquals("7, 6", setter(className).get("rollupOwnedStatuses"),
                    "the written row is the Plan, so its roll-up decides: " + setter(className));
        }
    }

    /** A setter that writes no roll-up's status gains no key, so its delegate renders unchanged. */
    @Test
    void aSetterOfAnotherMemberCarriesNoRollupKeys() {
        for (String className : List.of("TenantProvisioningMarkDone", "TenantProvisioningLabelPlan", "TenantProvisioningResetFailure")) {
            Map<String, Object> setter = setter(className);
            for (String key : List.of("rollupStatus", "rollupDisplacedStatus", "rollupOwnedStatuses")) {
                assertFalse(setter.containsKey(key), "a setter of another member must stay exactly as before: " + setter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> assignments(Map<String, Object> setter) {
        return (List<Map<String, Object>>) setter.get("assignments");
    }

    private static Map<String, Object> setter(String className) {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> setters = GlueIntentGenerator.buildSettersForTest(model);
        return setters.stream()
                      .filter(entry -> className.equals(entry.get("className")))
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("expected a setter [" + className + "] but got " + setters));
    }
}
