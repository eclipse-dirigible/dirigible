/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

class SimpleCollection {

    private final CollectionType collectionType;
    private final ElementType elementType;
    private final String jsonValue;

    public SimpleCollection(CollectionType collectionType, ElementType elementType, String jsonValue) {
        this.collectionType = collectionType;
        this.elementType = elementType;
        this.jsonValue = jsonValue;
    }

    enum CollectionType {
        SET, LIST, QUEUE
    }


    enum ElementType {
        BYTE, SHORT, INT, LONG, DOUBLE, FLOAT, BOOLEAN, STRING
    }

    public String getJsonValue() {
        return jsonValue;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

    public ElementType getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "SimpleCollection{" + "collectionType=" + collectionType + ", elementType=" + elementType + ", jsonValue='" + jsonValue
                + '\'' + '}';
    }
}
