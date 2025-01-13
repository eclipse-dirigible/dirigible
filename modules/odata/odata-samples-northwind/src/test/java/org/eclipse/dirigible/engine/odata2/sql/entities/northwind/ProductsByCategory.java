/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.entities.northwind;

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;

/**
 * The Class ProductsByCategory.
 */
@EdmEntityType(name = "Products_by_Category")
@EdmEntitySet(name = "Products_by_Categories", container = "NorthwindEntities")
public class ProductsByCategory {

    /** The category name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 15))
    private String categoryName;

    /** The product name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String productName;

    /** The quantity per unit. */
    @EdmProperty(facets = @EdmFacets(maxLength = 20))
    private String quantityPerUnit;

    /** The units in stock. */
    @EdmProperty
    private Short unitsInStock;

    /** The discontinued. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false))
    private Boolean discontinued;
}
