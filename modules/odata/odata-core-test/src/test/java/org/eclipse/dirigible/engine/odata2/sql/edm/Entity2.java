/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.edm;

import org.apache.olingo.odata2.api.annotation.edm.*;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class Entity2.
 */
@EdmEntityType
@EdmEntitySet(name = "Entities2")
public class Entity2 {

    /** The id. */
    @EdmKey
    @EdmProperty
    private Long id;

    /** The name. */
    @EdmProperty
    private String name;

    /** The value. */
    @EdmProperty
    private String value;

    /** The entity 1. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.ZERO_OR_ONE, toType = Entity1.class, association = "Entities2OfEntity1")
    private Entity1 entity1;

    /** The entity 3. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.MANY, toType = Entity3.class, association = "Entities3OfEntity2")
    private List<Entity3> entity3 = new ArrayList<Entity3>();

}
