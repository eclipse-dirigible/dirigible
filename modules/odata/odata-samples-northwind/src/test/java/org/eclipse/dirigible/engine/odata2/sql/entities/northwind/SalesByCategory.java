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
import org.apache.olingo.odata2.api.annotation.edm.EdmType;

/**
 * The Class SalesByCategory.
 */
@EdmEntityType(name = "Sales_by_Category")
@EdmEntitySet(name = "Sales_by_Categories", container = "NorthwindEntities")
public class SalesByCategory {

    /** The category id. */
    @EdmKey
    @EdmProperty(name = "CategoryID", facets = @EdmFacets(nullable = false))
    private Integer categoryId;

    /** The category name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 15))
    private String categoryName;

    /** The product name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String productName;

    /** The product sales. */
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(precision = 19, scale = 4))
    private Double productSales;
}
