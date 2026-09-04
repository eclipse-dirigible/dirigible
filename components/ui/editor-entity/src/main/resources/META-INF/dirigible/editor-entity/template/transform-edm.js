/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

import { Workspace as workspaceManager } from "@aerokit/sdk/platform";
import { Bytes } from "@aerokit/sdk/io";
import { XML } from "@aerokit/sdk/utils";

// Structured (List/Map) attributes the EDM generator / serializer write as JSON strings in flat attributes
// so the .edm stays lossless; parse them back into objects so the .model matches the intent's .model (#6826).
// The three lists below MUST together mirror EdmIntentGenerator.STRUCTURED_ATTRIBUTES (Java) - a key
// handled on one side but not the other round-trips as a JSON string (caught by EdmModelRoundTripIT's
// structural diff).
// uniqueConstraints is intentionally NOT here: the composite-unique-key feature emits it as a
// <constraints>/<uniqueKey> section that transformUniqueKey (below) rebuilds, so parsing it here too would
// duplicate it (#6826).
const ENTITY_STRUCTURED = ['rollupGuard', 'checks', 'labelParts', 'aggregateKeys', 'groupingKeys', 'relatedEntities', 'scopedCalendars'];
const PROPERTY_STRUCTURED = ['lookupColumns'];
// Document level (#6882): the structured values the .model carries ABOVE its entities.
const MODEL_STRUCTURED = ['languages', 'widgets', 'customActionLabels', 'processTaskLabels'];

// The document-level metadata: every ATTRIBUTE of the <model> element, generically - so a future scalar
// (a new title-like key) needs no change here, only serialization on the Java side. Before #6882 the
// .model root was rebuilt with entities/perspectives/navigations and nothing else, so a diagram save
// deleted title, description, icon, languages[] and the two label maps outright - silently downgrading a
// multilingual application to monolingual and replacing every custom-action and user-task caption with
// its raw identifier.
function documentMetadata(rawModel) {
    const metadata = {};
    if (!rawModel) {
        return metadata;
    }
    for (const key in rawModel) {
        // XML.toJson prefixes an attribute with '-'; the child ELEMENTS (entities, perspectives, ...)
        // have their own transforms below.
        if (key.startsWith('-')) {
            metadata[key.substring(1)] = rawModel[key];
        }
    }
    parseStructured(metadata, MODEL_STRUCTURED);
    return metadata;
}

function parseStructured(obj, keys) {
    for (const key of keys) {
        const val = obj[key];
        if (typeof val === 'string' && (val.startsWith('{') || val.startsWith('['))) {
            try {
                obj[key] = JSON.parse(val);
            } catch (e) {
                console.error("Failed to parse structured .edm attribute [" + key + "]: " + e);
            }
        }
    }
}

export function transform(workspaceName, projectName, filePath) {

    if (!filePath.endsWith('.edm')) {
        return null;
    }

    let contents = workspaceManager.getWorkspace(workspaceName)
        .getProject(projectName).getFile(filePath).getContent();

    contents = Bytes.byteArrayToText(contents);

    let raw = JSON.parse(XML.toJson(contents));

    let root = {};
    // The document-level metadata first, so the regenerated .model reads like the generated one.
    root.model = documentMetadata(raw.model);
    root.model.entities = [];
    root.model.perspectives = [];
    root.model.navigations = [];
    if (raw.model) {
        if (raw.model.entities) {
            if (raw.model.entities.entity) {
                if (Array.isArray(raw.model.entities.entity)) {
                    raw.model.entities.entity.forEach(entity => { root.model.entities.push(transformEntity(entity)) });
                } else {
                    root.model.entities.push(transformEntity(raw.model.entities.entity));
                }
                if (Array.isArray(raw.model.entities.relation)) {
                    raw.model.entities.relation.forEach(relation => { transformRelation(relation, root.model.entities) });
                } else if (raw.model.entities.relation) {
                    transformRelation(raw.model.entities.relation, root.model.entities);
                }
            } else {
                console.error("Invalid source model: 'entity' element is null");
            }
        } else {
            console.error("Invalid source model: 'entities' element is null");
        }

        // Composite business keys - declared top-level, resolved back onto their entity by name, the
        // same way a <relation> is. A key the model cannot resolve (a renamed or deleted entity, an
        // unknown property) is DROPPED rather than emitted: a constraint over a column that is not
        // there fails the whole schema, and silently doing nothing is the lesser of the two.
        if (raw.model.constraints && raw.model.constraints.uniqueKey) {
            if (Array.isArray(raw.model.constraints.uniqueKey)) {
                raw.model.constraints.uniqueKey.forEach(key => { transformUniqueKey(key, root.model.entities) });
            } else {
                transformUniqueKey(raw.model.constraints.uniqueKey, root.model.entities);
            }
        }

        if (raw.model.perspectives) {
            if (raw.model.perspectives.perspective) {
                if (Array.isArray(raw.model.perspectives.perspective)) {
                    raw.model.perspectives.perspective.forEach(perspective => { root.model.perspectives.push(transformPerspective(perspective)) });
                } else {
                    root.model.perspectives.push(transformPerspective(raw.model.perspectives.perspective));
                }
            } else {
                console.error("Invalid source model: 'perspective' element is null");
            }
        } else {
            console.error("Invalid source model: 'perspectives' element is null");
        }

        if (raw.model.navigations) {
            if (raw.model.navigations.item) {
                if (Array.isArray(raw.model.navigations.item)) {
                    raw.model.navigations.item.forEach(item => { root.model.navigations.push(transformNavigations(item)) });
                } else {
                    root.model.navigations.push(transformNavigations(raw.model.navigations.item));
                }
            }
        }

    } else {
        console.error("Invalid source model: 'model' element is null");
    }

    return JSON.stringify(root, null, 4);

    function transformEntity(raw) {
        let entity = {};
        entity.properties = [];
        for (let propertyName in raw) {
            if (propertyName !== 'property') {
                entity[propertyName.substring(1, propertyName.length)] = raw[propertyName];
            }
        }
        if (Array.isArray(raw.property)) {
            raw.property.forEach(property => { entity.properties.push(transformProperty(property)) });
        } else {
            entity.properties.push(transformProperty(raw.property))
        }
        parseStructured(entity, ENTITY_STRUCTURED);
        return entity;
    }

    function transformProperty(raw) {
        let property = {};
        for (let propertyName in raw) {
            property[propertyName.substring(1, propertyName.length)] = raw[propertyName];
        }
        parseStructured(property, PROPERTY_STRUCTURED);
        return property;
    }

    function transformUniqueKey(raw, entities) {
        const entity = entities.find(candidate => candidate.name === raw.entity);
        if (!entity) {
            console.error("Skipping unique key [" + raw.name + "]: it names no entity [" + raw.entity + "] of this model");
            return;
        }
        const properties = (raw.properties || '').split(',')
            .map(name => name.trim())
            .filter(name => name.length > 0);
        if (properties.length < 2) {
            console.error("Skipping unique key [" + raw.name + "] of [" + raw.entity + "]: a key spans two or more properties");
            return;
        }
        const columns = [];
        for (let i = 0; i < properties.length; i++) {
            const property = entity.properties.find(candidate => candidate.name === properties[i]);
            if (!property || !property.dataName) {
                console.error("Skipping unique key [" + raw.name + "] of [" + raw.entity + "]: it names no property [" + properties[i] + "]");
                return;
            }
            columns.push({ name: property.dataName });
        }
        if (!entity.uniqueConstraints) {
            entity.uniqueConstraints = [];
        }
        entity.uniqueConstraints.push({
            name: raw.name,
            columns: columns,
            columnsCsv: columns.map(column => column.name).join(','),
            message: raw.message ? raw.message : 'A ' + raw.entity + ' with the same ' + properties.join(', ') + ' already exists',
        });
    }

    // The <relation> element RESTATES naming that the foreign-key <property> element already carries.
    // The property is the source of truth and its value is kept: it is what the modeler itself edits
    // (editor.js writes the Relationship-properties dialog onto cell.source.value.relationshipName and
    // derives the edge label from it), and it is the only one of the two that can carry a cross-model
    // target's perspective. The <relation> element's copies are a FALLBACK - for an .edm written before
    // the two writers were converged (#6883), or a modeler save that dropped one. Trusting them renamed
    // every relationship whose name is not its target's, because serializer.js writes <relation name>
    // from the source and target ENTITY names; and it repointed every cross-model lookup, because it
    // writes the perspective off the target CELL, which for a projection is deliberately empty.
    //
    // relationshipEntityName is the exception and stays unconditional: no writer puts it on the
    // <property> (serializer.js emits it on neither the explicit block nor the generic pass), so the
    // edge's `referenced` - which the modeler keeps live when an edge is redirected - is the only value.
    function transformRelation(relation, entities) {
        entities.forEach(entity => {
            if (entity.name === relation['-entity']) {
                entity.properties.forEach(property => {
                    if (property.name === relation['-property']) {
                        property.relationshipEntityName = relation['-referenced'];
                        fillIfAbsent(property, 'relationshipName', relation['-name']);
                        fillIfAbsent(property, 'relationshipEntityPerspectiveName', relation['-relationshipEntityPerspectiveName']);
                        fillIfAbsent(property, 'relationshipEntityPerspectiveLabel', relation['-relationshipEntityPerspectiveLabel']);
                    }
                });
            }
        });
    }

    function fillIfAbsent(property, key, fallback) {
        const own = property[key];
        if (own === undefined || own === null || own === '') {
            property[key] = fallback;
        }
    }

    function transformPerspective(raw) {
        let perspective = {};
        for (let propertyName in raw) {
            perspective[propertyName] = raw[propertyName];
        }
        return perspective;
    }

    function transformNavigations(raw) {
        let item = {};
        for (let propertyName in raw) {
            item[propertyName] = raw[propertyName];
        }
        return item;
    }
}
