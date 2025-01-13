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
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmType;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;

/**
 * The Class Product.
 */
@EdmEntityType(name = "Product")
@EdmEntitySet(name = "Products", container = "NorthwindEntities")
public class Product {

    /** The product id. */
    @EdmKey
    @EdmProperty(name = "ProductID", facets = @EdmFacets(nullable = false))
    private Integer productId;

    /** The product name. */
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
    @EdmProperty(facets = @EdmFacets(nullable = false))
    private Boolean discontinued;

    /** The category. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.ZERO_OR_ONE, //
            toType = Category.class, //
            toRole = "Categories", //
            association = "FK_Products_Categories" //
    )
    private Category category;

    /** The order details. */
    @EdmNavigationProperty( //
            name = "Order_Details", //
            toMultiplicity = Multiplicity.MANY, //
            toType = OrderDetail.class, //
            toRole = "Order_Details", //
            association = "FK_Order_Details_Products" //
    )
    private List<OrderDetail> orderDetails;

    /** The supplier. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.ZERO_OR_ONE, //
            toType = Supplier.class, //
            toRole = "Suppliers", //
            association = "FK_Products_Suppliers" //
    )
    private Supplier supplier;
}
