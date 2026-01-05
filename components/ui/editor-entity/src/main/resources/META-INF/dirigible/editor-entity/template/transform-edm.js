/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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

export function transform(workspaceName, projectName, filePath) {

    if (!filePath.endsWith('.edm')) {
        return null;
    }

    let contents = workspaceManager.getWorkspace(workspaceName)
        .getProject(projectName).getFile(filePath).getContent();

    contents = Bytes.byteArrayToText(contents);

    let raw = JSON.parse(XML.toJson(contents));

    let root = {};
    root.model = {};
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
        return entity;
    }

    function transformProperty(raw) {
        let property = {};
        for (let propertyName in raw) {
            property[propertyName.substring(1, propertyName.length)] = raw[propertyName];
        }
        return property;
    }

    function transformRelation(relation, entities) {
        entities.forEach(entity => {
            if (entity.name === relation['-entity']) {
                entity.properties.forEach(property => {
                    if (property.name === relation['-property']) {
                        property.relationshipName = relation['-name'];
                        property.relationshipEntityName = relation['-referenced'];
                        property.relationshipEntityPerspectiveName = relation['-relationshipEntityPerspectiveName'];
                        property.relationshipEntityPerspectiveLabel = relation['-relationshipEntityPerspectiveLabel'];
                    }
                });
            }
        });
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
