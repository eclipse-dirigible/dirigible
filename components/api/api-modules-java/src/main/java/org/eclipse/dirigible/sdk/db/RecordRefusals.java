/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.sdk.db.GenerateRefusals.Label;

/**
 * Builds the refusals a generated application answers about a record as a whole (dirigible #8775) -
 * an action from the wrong status, a status move its lifecycle does not allow, a write to a record
 * its status, its master or its accounting period has fixed, a share link that cannot be made, a
 * capacity a roll-up guards - their messages resolved for the language of the request exactly as a
 * generated check's are, see {@link CheckMessages}.
 *
 * <p>
 * Each sentence is an entry of the model's catalog, under {@code <catalog>.checks.<key>}, and names
 * the entities, the actions and the statuses by their labels: an entity or an action by its catalog
 * label ({@code <catalog>.t.<name>}, {@code <catalog>.actions.<name>}), a status by the label its
 * row reads in the request's language. A language catalog that does not carry an entry falls back
 * to the platform's translation, then to the default-language text below.
 */
public final class RecordRefusals {

    /** The key of the sentence refusing an action asked from a status it does not accept. */
    public static final String STATUS_NOT_ALLOWED_KEY = "actionStatusNotAllowed";

    /**
     * The default-language sentence refusing an action asked from a status it does not accept:
     * {@code {allowed}} are the statuses it accepts, {@code {status}} the one the record is in.
     */
    public static final String STATUS_NOT_ALLOWED_TEXT = "{action} is allowed only from {allowed} - this {entity} is {status}";

    /** The key of the sentence refusing an action whose condition the record does not meet. */
    public static final String CONDITION_NOT_MET_KEY = "actionConditionNotMet";

    /** The default-language sentence refusing an action whose condition the record does not meet. */
    public static final String CONDITION_NOT_MET_TEXT = "{action} requires {condition}";

    /** The key of the sentence refusing a create-from already run for its source. */
    public static final String ALREADY_GENERATED_KEY = "actionAlreadyGenerated";

    /** The default-language sentence refusing a create-from already run for its source. */
    public static final String ALREADY_GENERATED_TEXT = "{action} was already generated from this {entity}";

    /** The key of the sentence refusing a status cleared on a record with a lifecycle. */
    public static final String LIFECYCLE_NO_STATUS_KEY = "lifecycleNoStatus";

    /** The default-language sentence refusing a status cleared on a record with a lifecycle. */
    public static final String LIFECYCLE_NO_STATUS_TEXT = "This {entity} is {status} and cannot be left without a status";

    /** The key of the sentence refusing a status move the lifecycle does not declare. */
    public static final String LIFECYCLE_MOVE_KEY = "lifecycleMove";

    /** The default-language sentence refusing a status move the lifecycle does not declare. */
    public static final String LIFECYCLE_MOVE_TEXT =
            "This {entity} cannot move from {from} to {to} - that is not a step its lifecycle allows";

    /** The key of the sentence refusing a record created outside its lifecycle's start. */
    public static final String LIFECYCLE_START_KEY = "lifecycleStart";

    /** The default-language sentence refusing a record created outside its lifecycle's start. */
    public static final String LIFECYCLE_START_TEXT = "A new {entity} starts at {initial}, not at {status}";

    /** The key of the sentence refusing a write a roll-up's capacity cannot take. */
    public static final String CAPACITY_EXCEEDED_KEY = "capacityExceeded";

    /**
     * The default-language sentence refusing a write a roll-up's capacity cannot take: {@code {parent}}
     * is the record holding the capacity, {@code {sum}} what the other rows already consume.
     */
    public static final String CAPACITY_EXCEEDED_TEXT =
            "The {parent} capacity is exceeded: capacity {capacity}, already consumed {sum}, requested {requested}";

    /** The key of the sentence refusing a change to an append-only record. */
    public static final String APPEND_ONLY_KEY = "recordAppendOnly";

    /** The default-language sentence refusing a change to an append-only record. */
    public static final String APPEND_ONLY_TEXT = "This {entity} is append-only - records can neither be edited nor deleted";

    /** The key of the sentence refusing a change to a record its status has fixed. */
    public static final String IMMUTABLE_KEY = "recordImmutable";

    /** The default-language sentence refusing a change to a record its status has fixed. */
    public static final String IMMUTABLE_TEXT = "This {entity} is immutable in its current status - corrections go through the workflow";

    /** The key of the sentence refusing a change in a closed accounting period. */
    public static final String PERIOD_CLOSED_KEY = "recordPeriodClosed";

    /** The default-language sentence refusing a change in a closed accounting period. */
    public static final String PERIOD_CLOSED_TEXT = "The {period} covering this date is closed - book the correction in an open period";

    /** The key of the sentence refusing a change to the detail of an append-only master. */
    public static final String MASTER_APPEND_ONLY_KEY = "masterAppendOnly";

    /** The default-language sentence refusing a change to the detail of an append-only master. */
    public static final String MASTER_APPEND_ONLY_TEXT = "This {entity} belongs to an append-only {master} and can no longer be changed";

    /** The key of the sentence refusing a change to the detail of a master its status has fixed. */
    public static final String MASTER_IMMUTABLE_KEY = "masterImmutable";

    /** The default-language sentence refusing a change to the detail of a fixed master. */
    public static final String MASTER_IMMUTABLE_TEXT =
            "The {master} of this {entity} is immutable in its current status - corrections go through the workflow";

    /** The key of the sentence refusing a change to the detail of a master in a closed period. */
    public static final String MASTER_PERIOD_CLOSED_KEY = "masterPeriodClosed";

    /** The default-language sentence refusing a change to the detail of a master in a closed period. */
    public static final String MASTER_PERIOD_CLOSED_TEXT =
            "The {period} covering this {master} is closed - book the correction in an open period";

    /** The key of the sentence refusing the delete of a record whose process is still running. */
    public static final String IN_FLOW_KEY = "recordInFlow";

    /** The default-language sentence refusing the delete of a record whose process is still running. */
    public static final String IN_FLOW_TEXT = "This {entity} is still in its {process} flow - complete or cancel it before deleting";

    /** The key of the sentence refusing a share link for a record its state keeps private. */
    public static final String NOT_SHAREABLE_KEY = "shareNotShareable";

    /** The default-language sentence refusing a share link for a record its state keeps private. */
    public static final String NOT_SHAREABLE_TEXT = "This {entity} cannot be shared in its current state";

    /** The key of the sentence refusing a share link while the platform switches sharing off. */
    public static final String SHARING_OFF_KEY = "shareSwitchedOff";

    /** The default-language sentence refusing a share link while sharing is switched off. */
    public static final String SHARING_OFF_TEXT = "Sharing documents through a link is switched off";

    /** The key of the sentence refusing a share link for a record with no issued copy. */
    public static final String NO_ISSUED_COPY_KEY = "shareNoIssuedCopy";

    /** The default-language sentence refusing a share link for a record with no issued copy. */
    public static final String NO_ISSUED_COPY_TEXT =
            "This {entity} has no issued copy yet - a link serves the stored copy, which is minted when the document is issued";

    /** The key of the sentence refusing a hierarchy too deep to walk. */
    public static final String HIERARCHY_TOO_DEEP_KEY = "hierarchyTooDeep";

    /** The default-language sentence refusing a hierarchy too deep to walk. */
    public static final String HIERARCHY_TOO_DEEP_TEXT = "The {entity} hierarchy is implausibly deep - aborting";

    private RecordRefusals() {}

    /**
     * The refusal of an action - a transition or a create-from - asked from a status it does not
     * accept.
     *
     * @param catalog the model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param action the action
     * @param entity the record's entity
     * @param allowed the labels of the statuses the action accepts, as the request's reader reads them
     * @param status the label of the status the record is in
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException statusNotAllowed(String catalog, Label action, Label entity, String allowed, String status) {
        return refusal(catalog, STATUS_NOT_ALLOWED_KEY, STATUS_NOT_ALLOWED_TEXT, null, "action", action, "allowed", allowed, "entity",
                entity, "status", status);
    }

    /**
     * The refusal of a transition whose {@code when:} condition the record does not meet.
     *
     * @param catalog the model catalog, or {@code null}
     * @param action the transition
     * @param condition the condition, as the model writes it
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException conditionNotMet(String catalog, Label action, String condition) {
        return refusal(catalog, CONDITION_NOT_MET_KEY, CONDITION_NOT_MET_TEXT, null, "action", action, "condition", condition);
    }

    /**
     * The refusal of a create-from whose source already stands in the status its run writes.
     *
     * @param catalog the model catalog, or {@code null}
     * @param action the create-from
     * @param entity the source's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException alreadyGenerated(String catalog, Label action, Label entity) {
        return refusal(catalog, ALREADY_GENERATED_KEY, ALREADY_GENERATED_TEXT, null, "action", action, "entity", entity);
    }

    /**
     * The refusal of a write that clears the status of a record with a lifecycle.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @param field the status property
     * @param status the label of the status the record is in
     * @return the refusal, carrying the key, the parameters and the field
     */
    public static ValidationException lifecycleNoStatus(String catalog, Label entity, String field, String status) {
        return refusal(catalog, LIFECYCLE_NO_STATUS_KEY, LIFECYCLE_NO_STATUS_TEXT, field, "entity", entity, "status", status);
    }

    /**
     * The refusal of a status move the record's lifecycle does not declare.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @param field the status property
     * @param from the label of the status the record is in
     * @param to the label of the status the write moves it to
     * @return the refusal, carrying the key, the parameters and the field
     */
    public static ValidationException lifecycleMove(String catalog, Label entity, String field, String from, String to) {
        return refusal(catalog, LIFECYCLE_MOVE_KEY, LIFECYCLE_MOVE_TEXT, field, "entity", entity, "from", from, "to", to);
    }

    /**
     * The refusal of a record created in another status than its lifecycle's start.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @param field the status property
     * @param initial the label of the status the lifecycle starts at
     * @param status the label of the status the write asks for
     * @return the refusal, carrying the key, the parameters and the field
     */
    public static ValidationException lifecycleStart(String catalog, Label entity, String field, String initial, String status) {
        return refusal(catalog, LIFECYCLE_START_KEY, LIFECYCLE_START_TEXT, field, "entity", entity, "initial", initial, "status", status);
    }

    /**
     * The refusal of a write a roll-up's capacity cannot take, when the roll-up declares no message of
     * its own.
     *
     * @param catalog the model catalog, or {@code null}
     * @param parent the entity holding the capacity
     * @param field the property whose value the roll-up sums
     * @param capacity the capacity
     * @param sum what the other rows already consume
     * @param requested what the write asks for
     * @return the refusal, carrying the key, the parameters and the field
     */
    public static ValidationException capacityExceeded(String catalog, Label parent, String field, Object capacity, Object sum,
            Object requested) {
        return refusal(catalog, CAPACITY_EXCEEDED_KEY, CAPACITY_EXCEEDED_TEXT, field, "parent", parent, "capacity", capacity, "sum", sum,
                "requested", requested);
    }

    /**
     * The refusal of a change to an append-only record.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException appendOnly(String catalog, Label entity) {
        return refusal(catalog, APPEND_ONLY_KEY, APPEND_ONLY_TEXT, null, "entity", entity);
    }

    /**
     * The refusal of a change to a record its status has fixed.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException immutable(String catalog, Label entity) {
        return refusal(catalog, IMMUTABLE_KEY, IMMUTABLE_TEXT, null, "entity", entity);
    }

    /**
     * The refusal of a change to a record whose date falls in a closed period.
     *
     * @param catalog the model catalog, or {@code null}
     * @param period the period register's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException periodClosed(String catalog, Label period) {
        return refusal(catalog, PERIOD_CLOSED_KEY, PERIOD_CLOSED_TEXT, null, "period", period);
    }

    /**
     * The refusal of a change to a detail whose master - or an ancestor further up - is append-only.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the detail's entity
     * @param master the append-only master's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException masterAppendOnly(String catalog, Label entity, Label master) {
        return refusal(catalog, MASTER_APPEND_ONLY_KEY, MASTER_APPEND_ONLY_TEXT, null, "entity", entity, "master", master);
    }

    /**
     * The refusal of a change to a detail whose master - or an ancestor further up - its status has
     * fixed.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the detail's entity
     * @param master the fixed master's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException masterImmutable(String catalog, Label entity, Label master) {
        return refusal(catalog, MASTER_IMMUTABLE_KEY, MASTER_IMMUTABLE_TEXT, null, "entity", entity, "master", master);
    }

    /**
     * The refusal of a change to a detail whose master's date falls in a closed period.
     *
     * @param catalog the model catalog, or {@code null}
     * @param master the master's entity
     * @param period the period register's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException masterPeriodClosed(String catalog, Label master, Label period) {
        return refusal(catalog, MASTER_PERIOD_CLOSED_KEY, MASTER_PERIOD_CLOSED_TEXT, null, "master", master, "period", period);
    }

    /**
     * The refusal of the delete of a record a running process started from.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @param process the process
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException inFlow(String catalog, Label entity, Label process) {
        return refusal(catalog, IN_FLOW_KEY, IN_FLOW_TEXT, null, "entity", entity, "process", process);
    }

    /**
     * The refusal of a share link for a record its current state keeps private.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException notShareable(String catalog, Label entity) {
        return refusal(catalog, NOT_SHAREABLE_KEY, NOT_SHAREABLE_TEXT, null, "entity", entity);
    }

    /**
     * The refusal of a share link while the platform has sharing switched off.
     *
     * @param catalog the model catalog, or {@code null}
     * @return the refusal, carrying the key
     */
    public static ValidationException sharingOff(String catalog) {
        return refusal(catalog, SHARING_OFF_KEY, SHARING_OFF_TEXT, null);
    }

    /**
     * The refusal of a share link for a record that has no issued copy to serve.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the record's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException noIssuedCopy(String catalog, Label entity) {
        return refusal(catalog, NO_ISSUED_COPY_KEY, NO_ISSUED_COPY_TEXT, null, "entity", entity);
    }

    /**
     * The refusal of a hierarchy reference whose chain of parents is too deep to walk.
     *
     * @param catalog the model catalog, or {@code null}
     * @param entity the hierarchy's entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException hierarchyTooDeep(String catalog, Label entity) {
        return refusal(catalog, HIERARCHY_TOO_DEEP_KEY, HIERARCHY_TOO_DEEP_TEXT, null, "entity", entity);
    }

    /**
     * The body a hand-written generated endpoint answers a refusal with - the one a generated
     * controller's refusal carries ({@code message}, {@code messageKey}, {@code messageParams},
     * {@code field}), plus {@code error} holding the same text for the clients that read the older
     * {@code {"error": reason}} shape.
     *
     * @param refusal the refusal
     * @return the body, to be serialized as JSON
     */
    public static Map<String, Object> body(ValidationException refusal) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", refusal.getMessage());
        body.put("message", refusal.getMessage());
        if (refusal.getMessageKey() != null) {
            body.put("messageKey", refusal.getMessageKey());
            body.put("messageParams", refusal.getMessageParams());
        }
        if (refusal.getField() != null) {
            body.put("field", refusal.getField());
        }
        return body;
    }

    private static ValidationException refusal(String catalog, String key, String text, String field, Object... params) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, key, text, field, params);
    }

    /**
     * A refusal resolved in one language: a {@link Label} among the parameters is named by its catalog
     * label in that language before the sentence is.
     */
    static ValidationException refusal(String language, CheckMessages.CatalogReader catalogs, String catalog, String key, String text,
            String field, Object... params) {
        Object[] named = params.clone();
        for (int i = 1; i < named.length; i += 2) {
            if (named[i] instanceof Label label) {
                named[i] = CheckMessages.resolve(language, catalogs, label.key(), label.text(), Map.of());
            }
        }
        Map<String, Object> values = CheckMessages.params(named);
        String messageKey = catalog == null || catalog.isBlank() ? null : catalog + ".checks." + key;
        return new ValidationException(CheckMessages.resolve(language, catalogs, messageKey, text, values), messageKey, values, field);
    }
}
