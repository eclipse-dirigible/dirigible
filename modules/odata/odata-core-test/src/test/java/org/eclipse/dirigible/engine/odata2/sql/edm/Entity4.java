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
 * The Class Entity4.
 */
@EdmEntityType
@EdmEntitySet(name = "Entities4")
public class Entity4 {

    /** The id 4 1. */
    @EdmKey
    @EdmProperty
    private Long id4_1;

    /** The id 4 2. */
    @EdmKey
    @EdmProperty
    private Long id4_2;

    /** The id 4 3. */
    @EdmProperty
    private String id4_3;

    /** The entity 5. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.MANY, toType = Entity5.class, association = "Entities4OfEntity5")
    private List<Entity5> entity5 = new ArrayList<>();
}
