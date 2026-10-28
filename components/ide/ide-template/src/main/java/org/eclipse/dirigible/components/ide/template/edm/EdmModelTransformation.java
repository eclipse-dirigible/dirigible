/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.edm;

import static org.eclipse.dirigible.components.ide.template.edm.EcmaValues.UNDEFINED;
import static org.eclipse.dirigible.components.ide.template.edm.EcmaValues.isTruthy;
import static org.eclipse.dirigible.components.ide.template.edm.EcmaValues.keys;
import static org.eclipse.dirigible.components.ide.template.edm.EcmaValues.member;
import static org.eclipse.dirigible.components.ide.template.edm.EcmaValues.strictlyEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.utils.xml2json.Xml2Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import tools.jackson.core.JacksonException;

/**
 * Derives the {@code .model} of an entity data model from its {@code .edm} - what saving a diagram
 * in the entity modeler writes beside it.
 *
 * <p>
 * The {@code .edm} is read through the platform's XML-to-JSON conversion, which turns an attribute
 * into a {@code -}-prefixed member and a repeated element into an array, and is rebuilt as the
 * {@code .model}: the document's attributes first, then its entities with their properties, the
 * relations and composite business keys resolved onto them, and its perspectives and navigations.
 * The {@code .model} is written exactly as the script this replaces wrote it, so a project's
 * committed {@code .model} files do not change by being saved again; that is why its values follow
 * {@link EcmaValues ECMAScript semantics}.
 *
 * <p>
 * The structured (List/Map) values the generator writes into the {@code .edm} as JSON text in a
 * flat attribute are parsed back into objects, per level. Together the three lists below mirror
 * {@code EdmIntentGenerator.STRUCTURED_ATTRIBUTES} (engine-intent): a key handled on one side but
 * not the other round-trips as a JSON string, which {@code EdmModelRoundTripIT}'s structural diff
 * catches. {@code uniqueConstraints} is deliberately not among them: the composite-unique-key
 * feature writes it as a {@code <constraints>/<uniqueKey>} section, rebuilt by
 * {@link #addUniqueKey}, and parsing it here too would duplicate it (#6826).
 */
final class EdmModelTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdmModelTransformation.class);

    /**
     * The structured entity attributes. The singular {@code rollupGuard} is the shape before #7448,
     * kept so that an {@code .edm} authored before the guard became a list still parses into an object.
     */
    private static final List<String> ENTITY_STRUCTURED =
            List.of("rollupGuards", "rollupGuard", "checks", "labelParts", "aggregateKeys", "groupingKeys", "relatedEntities",
                    "scopedCalendars", "agreeGuards", "lifecycleStatusNameList", "duplicateReset", "duplicateDefaults", "singleFlags");

    /** The structured property attributes. */
    private static final List<String> PROPERTY_STRUCTURED = List.of("lookupColumns");

    /**
     * The structured document attributes: what the {@code .model} carries above its entities (#6882).
     */
    private static final List<String> MODEL_STRUCTURED =
            List.of("languages", "widgets", "customActionLabels", "processTaskLabels", "reports");

    /** The indentation of the {@code .model}. */
    private static final String INDENT = "    ";

    /** The prefix the XML-to-JSON conversion gives an attribute. */
    private static final String ATTRIBUTE_PREFIX = "-";

    private static final String MODEL = "model";

    private static final String ENTITIES = "entities";

    private static final String ENTITY = "entity";

    private static final String RELATION = "relation";

    private static final String PROPERTY = "property";

    private static final String PROPERTIES = "properties";

    private static final String PERSPECTIVES = "perspectives";

    private static final String PERSPECTIVE = "perspective";

    private static final String NAVIGATIONS = "navigations";

    private static final String ITEM = "item";

    private static final String CONSTRAINTS = "constraints";

    private static final String UNIQUE_KEY = "uniqueKey";

    private static final String UNIQUE_CONSTRAINTS = "uniqueConstraints";

    private static final String NAME = "name";

    private static final String DATA_NAME = "dataName";

    private static final String MESSAGE = "message";

    /**
     * Not instantiable.
     */
    private EdmModelTransformation() {}

    /**
     * Derives the {@code .model} of an {@code .edm}.
     *
     * @param edm the {@code .edm} content
     * @return the {@code .model} content
     * @throws IllegalArgumentException when the {@code .edm} is not well-formed XML, or has a shape the
     *         {@code .model} cannot be built from
     */
    static String transform(String edm) {
        Object raw = EcmaJson.parse(toJson(edm));
        Object rawModel = member(raw, MODEL);

        Map<String, Object> model = documentMetadata(rawModel);
        List<Object> entities = new ArrayList<>();
        List<Object> perspectives = new ArrayList<>();
        List<Object> navigations = new ArrayList<>();
        model.put(ENTITIES, entities);
        model.put(PERSPECTIVES, perspectives);
        model.put(NAVIGATIONS, navigations);

        if (isTruthy(rawModel)) {
            addEntities(member(rawModel, ENTITIES), entities);
            addUniqueKeys(member(rawModel, CONSTRAINTS), entities);
            addPerspectives(member(rawModel, PERSPECTIVES), perspectives);
            addNavigations(member(rawModel, NAVIGATIONS), navigations);
        } else {
            LOGGER.warn("Invalid source model: 'model' element is null");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put(MODEL, model);
        return EcmaJson.stringify(root, INDENT);
    }

    private static String toJson(String edm) {
        try {
            return Xml2Json.toJson(edm);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("The .edm is not well-formed XML: " + e.getMessage(), e);
        }
    }

    /**
     * Every attribute of the {@code <model>} element, generically - so that a future document-level
     * scalar needs no change here. Before #6882 the {@code .model} root was rebuilt with its entities,
     * perspectives and navigations only, so a diagram save deleted the title, the languages and the two
     * label maps.
     */
    private static Map<String, Object> documentMetadata(Object rawModel) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (!isTruthy(rawModel)) {
            return metadata;
        }
        for (String key : keys(rawModel)) {
            // The child elements (entities, perspectives, ...) have their own transformations below.
            if (key.startsWith(ATTRIBUTE_PREFIX)) {
                metadata.put(key.substring(ATTRIBUTE_PREFIX.length()), member(rawModel, key));
            }
        }
        parseStructured(metadata, MODEL_STRUCTURED);
        return metadata;
    }

    private static void addEntities(Object rawEntities, List<Object> entities) {
        if (!isTruthy(rawEntities)) {
            LOGGER.warn("Invalid source model: 'entities' element is null");
            return;
        }
        Object rawEntity = member(rawEntities, ENTITY);
        if (!isTruthy(rawEntity)) {
            LOGGER.warn("Invalid source model: 'entity' element is null");
            return;
        }
        for (Object entity : elements(rawEntity)) {
            entities.add(transformEntity(entity));
        }
        Object rawRelation = member(rawEntities, RELATION);
        if (isTruthy(rawRelation)) {
            for (Object relation : elements(rawRelation)) {
                applyRelation(relation, entities);
            }
        }
    }

    /**
     * An entity: its {@code properties} first, then its attributes and any other child element.
     */
    private static Map<String, Object> transformEntity(Object raw) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put(PROPERTIES, new ArrayList<>());
        for (String key : keys(raw)) {
            if (!PROPERTY.equals(key)) {
                entity.put(withoutFirstCharacter(key), member(raw, key));
            }
        }
        // An entity without a <property> still gets one property, an empty one, as it always did.
        for (Object property : elements(member(raw, PROPERTY))) {
            push(entity.get(PROPERTIES), transformProperty(property), PROPERTIES);
        }
        parseStructured(entity, ENTITY_STRUCTURED);
        return entity;
    }

    private static Map<String, Object> transformProperty(Object raw) {
        Map<String, Object> property = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            property.put(withoutFirstCharacter(key), member(raw, key));
        }
        parseStructured(property, PROPERTY_STRUCTURED);
        return property;
    }

    /**
     * The JSON text of a structured attribute, parsed back into its object. A value that is not JSON
     * stays the text it is.
     */
    private static void parseStructured(Map<String, Object> object, List<String> keys) {
        for (String key : keys) {
            if (object.get(key) instanceof String text && (text.startsWith("{") || text.startsWith("["))) {
                try {
                    object.put(key, EcmaJson.parse(text));
                } catch (JacksonException e) {
                    LOGGER.warn("Failed to parse structured .edm attribute [{}]; it is kept as text", key, e);
                }
            }
        }
    }

    /**
     * Applies a {@code <relation>} element onto the foreign-key property it describes.
     *
     * <p>
     * The element RESTATES naming that the foreign-key {@code <property>} element already carries. The
     * property is the source of truth and its value is kept: it is what the modeler itself edits, and
     * the only one of the two that can carry a cross-model target's perspective. The relation's copies
     * are a fallback - for an {@code .edm} written before the two writers were converged (#6883), or a
     * modeler save that dropped one. Trusting them renamed every relationship whose name is not its
     * target's and repointed every cross-model lookup.
     *
     * <p>
     * {@code relationshipEntityName} is the exception and is always taken from the relation: no writer
     * puts it on the {@code <property>}, so the edge's {@code referenced} is the only value.
     */
    private static void applyRelation(Object relation, List<Object> entities) {
        for (Object entity : entities) {
            if (!strictlyEquals(member(entity, NAME), member(relation, "-entity"))) {
                continue;
            }
            for (Object candidate : elements(member(entity, PROPERTIES))) {
                if (candidate instanceof Map<?, ?> && strictlyEquals(member(candidate, NAME), member(relation, "-property"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> property = (Map<String, Object>) candidate;
                    property.put("relationshipEntityName", member(relation, "-referenced"));
                    fillIfAbsent(property, "relationshipName", member(relation, "-name"));
                    fillIfAbsent(property, "relationshipEntityPerspectiveName", member(relation, "-relationshipEntityPerspectiveName"));
                    fillIfAbsent(property, "relationshipEntityPerspectiveLabel", member(relation, "-relationshipEntityPerspectiveLabel"));
                }
            }
        }
    }

    private static void fillIfAbsent(Map<String, Object> property, String key, Object fallback) {
        Object own = member(property, key);
        if (own == UNDEFINED || own == null || (own instanceof String text && text.isEmpty())) {
            property.put(key, fallback);
        }
    }

    private static void addUniqueKeys(Object rawConstraints, List<Object> entities) {
        if (!isTruthy(rawConstraints)) {
            return;
        }
        Object rawUniqueKey = member(rawConstraints, UNIQUE_KEY);
        if (isTruthy(rawUniqueKey)) {
            for (Object uniqueKey : elements(rawUniqueKey)) {
                addUniqueKey(uniqueKey, entities);
            }
        }
    }

    /**
     * A composite business key, declared top-level and resolved back onto its entity by name, the way a
     * {@code <relation>} is. A key the model cannot resolve - a renamed or deleted entity, an unknown
     * property - is dropped rather than emitted: a constraint over a column that is not there fails the
     * whole schema, and silently doing nothing is the lesser of the two.
     */
    private static void addUniqueKey(Object raw, List<Object> entities) {
        Object keyName = member(raw, NAME);
        Object entityName = member(raw, ENTITY);
        Object entity = entities.stream()
                                .filter(candidate -> strictlyEquals(member(candidate, NAME), entityName))
                                .findFirst()
                                .orElse(null);
        if (entity == null) {
            LOGGER.warn("Skipping unique key [{}]: it names no entity [{}] of this model", sanitized(keyName), sanitized(entityName));
            return;
        }
        List<String> propertyNames = propertyNames(member(raw, PROPERTIES));
        if (propertyNames.size() < 2) {
            LOGGER.warn("Skipping unique key [{}] of [{}]: a key spans two or more properties", sanitized(keyName), sanitized(entityName));
            return;
        }
        List<Object> columns = new ArrayList<>();
        for (String propertyName : propertyNames) {
            Object property = elements(member(entity, PROPERTIES)).stream()
                                                                  .filter(candidate -> strictlyEquals(member(candidate, NAME),
                                                                          propertyName))
                                                                  .findFirst()
                                                                  .orElse(null);
            if (property == null || !isTruthy(member(property, DATA_NAME))) {
                LOGGER.warn("Skipping unique key [{}] of [{}]: it names no property [{}]", sanitized(keyName), sanitized(entityName),
                        LogSanitizer.sanitize(propertyName));
                return;
            }
            Map<String, Object> column = new LinkedHashMap<>();
            column.put(NAME, member(property, DATA_NAME));
            columns.add(column);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) entity;
        if (!isTruthy(target.get(UNIQUE_CONSTRAINTS))) {
            target.put(UNIQUE_CONSTRAINTS, new ArrayList<>());
        }
        Object message = member(raw, MESSAGE);
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put(NAME, keyName);
        constraint.put("columns", columns);
        constraint.put("columnsCsv", EcmaValues.join(columns.stream()
                                                            .map(column -> member(column, NAME))
                                                            .toList(),
                ","));
        constraint.put(MESSAGE, isTruthy(message) ? message
                : "A " + EcmaValues.toText(entityName) + " with the same " + String.join(", ", propertyNames) + " already exists");
        push(target.get(UNIQUE_CONSTRAINTS), constraint, UNIQUE_CONSTRAINTS);
    }

    /**
     * The property names a key lists, comma-separated, each trimmed, the empty ones dropped.
     */
    private static List<String> propertyNames(Object properties) {
        Object list = isTruthy(properties) ? properties : "";
        if (!(list instanceof String text)) {
            throw new IllegalArgumentException(
                    "The properties of a unique key must be a comma-separated list, not " + EcmaValues.toText(list));
        }
        List<String> names = new ArrayList<>();
        for (String name : text.split(",", -1)) {
            String trimmed = EcmaValues.trim(name);
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    private static void addPerspectives(Object rawPerspectives, List<Object> perspectives) {
        if (!isTruthy(rawPerspectives)) {
            LOGGER.warn("Invalid source model: 'perspectives' element is null");
            return;
        }
        Object rawPerspective = member(rawPerspectives, PERSPECTIVE);
        if (!isTruthy(rawPerspective)) {
            LOGGER.warn("Invalid source model: 'perspective' element is null");
            return;
        }
        for (Object perspective : elements(rawPerspective)) {
            perspectives.add(copyMembers(perspective));
        }
    }

    private static void addNavigations(Object rawNavigations, List<Object> navigations) {
        if (!isTruthy(rawNavigations)) {
            return;
        }
        Object rawItem = member(rawNavigations, ITEM);
        if (isTruthy(rawItem)) {
            for (Object item : elements(rawItem)) {
                navigations.add(copyMembers(item));
            }
        }
    }

    /**
     * A perspective or a navigation item: every member as it is, child elements and attributes alike.
     */
    private static Map<String, Object> copyMembers(Object raw) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (String key : keys(raw)) {
            copy.put(key, member(raw, key));
        }
        return copy;
    }

    /**
     * The elements of a repeated XML element, which the conversion makes an array only from two on: the
     * array's elements, or the one value - {@code undefined} included - as the only element.
     */
    private static List<Object> elements(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        List<Object> single = new ArrayList<>(1);
        single.add(value);
        return single;
    }

    /**
     * Appends to an array that a member is expected to hold. A member an attribute of the same name
     * replaced is not one, and the transformation fails as the script did.
     */
    @SuppressWarnings("unchecked")
    private static void push(Object array, Object value, String name) {
        if (!(array instanceof List<?>)) {
            throw new IllegalArgumentException("The member [" + name + "] is not an array but " + EcmaValues.toText(array));
        }
        ((List<Object>) array).add(value);
    }

    private static String withoutFirstCharacter(String key) {
        return key.isEmpty() ? key : key.substring(1);
    }

    private static String sanitized(Object value) {
        return LogSanitizer.sanitize(EcmaValues.toText(value));
    }

}
