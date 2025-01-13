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
 * The Class Entity5.
 */
@EdmEntityType
@EdmEntitySet(name = "Entities5")
public class Entity5 {

    /** The id 5. */
    @EdmKey
    @EdmProperty
    private Long id5;

    /** The fk id 4 1. */
    @EdmProperty
    private Long fk_id4_1;

    /** The fk id 4 2. */
    @EdmProperty
    private Long fk_id4_2;

    /** The entity 4. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.ZERO_OR_ONE, toType = Entity4.class, association = "Entities4OfEntity5")
    private Entity4 entity4;
}
