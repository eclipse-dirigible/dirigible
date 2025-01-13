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
 * The Class AlphabeticalListOfProduct.
 */
@EdmEntityType(name = "Alphabetical_list_of_product")
@EdmEntitySet(name = "Alphabetical_list_of_products", container = "NorthwindEntities")
public class AlphabeticalListOfProduct {

    /** The product id. */
    @EdmKey
    @EdmProperty(name = "ProductID", facets = @EdmFacets(nullable = false))
    private Integer productId;

    /** The product name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String productName;

    /** The supplier id. */
    @EdmProperty(name = "SupplierID")
    private Integer supplierId;

    /** The category id. */
    @EdmProperty(name = "CategoryID")
    private Integer categoryId;

    /** The quantity per unit. */
    @EdmProperty(facets = @EdmFacets(maxLength = 20))
    private String quantityPerUnit;

    /** The unit price. */
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(precision = 19, scale = 4))
    private Double unitPrice;

    /** The units in stock. */
    @EdmProperty
    private Short unitsInStock;

    /** The units on order. */
    @EdmProperty
    private Short unitsOnOrder;

    /** The reorder level. */
    @EdmProperty
    private Short reorderLevel;

    /** The discontinued. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false))
    private Boolean discontinued;

    /** The category name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 15))
    private String categoryName;

}
