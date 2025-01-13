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

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;

/**
 * The Class Entity3.
 */
@EdmEntityType
@EdmEntitySet(name = "Entities3")
public class Entity3 {

    /** The id. */
    @EdmKey
    @EdmProperty
    private Long id;

    /** The description. */
    @EdmProperty
    private String description;

    /** The complex type property. */
    @EdmProperty
    private CTEntity complexTypeProperty;

    /** The entity 2. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.ZERO_OR_ONE, toType = Entity2.class, association = "Entities3OfEntity2")
    private Entity2 entity2;
}
