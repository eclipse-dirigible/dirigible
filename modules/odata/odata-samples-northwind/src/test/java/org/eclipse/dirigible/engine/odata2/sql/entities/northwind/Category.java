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

import java.util.List;

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmType;

/**
 * The Class Category.
 */
@EdmEntityType(name = "Category")
@EdmEntitySet(name = "Categories", container = "NorthwindEntities")
public class Category {

    /** The category id. */
    @EdmKey
    @EdmProperty(name = "CategoryID", facets = @EdmFacets(nullable = false))
    private Integer categoryId;

    /** The category name. */
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 15))
    private String categoryName;

    /** The description. */
    @EdmProperty
    private String description;

    /** The picture. */
    @EdmProperty(type = EdmType.BINARY)
    private String picture;

    /** The products. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = Product.class, //
            toRole = "Products", //
            association = "FK_Products_Categories" //
    )
    private List<Product> products;
}
