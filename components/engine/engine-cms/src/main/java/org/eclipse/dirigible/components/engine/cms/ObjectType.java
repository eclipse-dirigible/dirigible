/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

/**
 * The Class ObjectType.
 */
public class ObjectType {

    /** The type. */
    private String type;

    /**
     * Instantiates a new object type.
     *
     * @param type the type
     */
    public ObjectType(String type) {
        this.type = type;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return this.type;
    }

    /** The Constant FOLDER. */
    public static final ObjectType FOLDER = new ObjectType(CmisConstants.OBJECT_TYPE_FOLDER);

    /** The Constant DOCUMENT. */
    public static final ObjectType DOCUMENT = new ObjectType(CmisConstants.OBJECT_TYPE_DOCUMENT);

}
