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
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmType;

/**
 * The Class OrderDetail.
 */
@EdmEntityType(name = "Order_Detail")
@EdmEntitySet(name = "Order_Details", container = "NorthwindEntities")
public class OrderDetail {

    /** The order id. */
    @EdmKey
    @EdmProperty(name = "OrderID", facets = @EdmFacets(nullable = false))
    private Integer orderId;

    /** The product id. */
    @EdmKey
    @EdmProperty(name = "ProductID", facets = @EdmFacets(nullable = false))
    private Integer productId;

    /** The unit price. */
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(nullable = false, precision = 19, scale = 4))
    private Double unitPrice;

    /** The quantity. */
    @EdmProperty(facets = @EdmFacets(nullable = false))
    private Short quantity;

    /** The discount. */
    @EdmProperty(type = EdmType.SINGLE, facets = @EdmFacets(nullable = false))
    private Integer discount;

    /** The order. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.ONE, //
            toType = Order.class, //
            toRole = "Orders", //
            association = "FK_Order_Details_Orders" //
    )
    private Order order;

    /** The product. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.ONE, //
            toType = Product.class, //
            toRole = "Products", //
            association = "FK_Order_Details_Products" //
    )
    private Product product;

}
