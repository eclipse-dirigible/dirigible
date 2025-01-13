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
 * The Class OrderDetailsExtended.
 */
@EdmEntityType(name = "Order_Details_Extended")
@EdmEntitySet(name = "Order_Details_Extendeds", container = "NorthwindEntities")
public class OrderDetailsExtended {

    /** The order id. */
    @EdmKey
    @EdmProperty(name = "OrderID", facets = @EdmFacets(nullable = false))
    private Integer orderId;

    /** The product id. */
    @EdmKey
    @EdmProperty(name = "ProductID", facets = @EdmFacets(nullable = false))
    private Integer productId;

    /** The product name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String productName;

    /** The unit price. */
    @EdmKey
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(nullable = false, precision = 19, scale = 4))
    private Double unitPrice;

    /** The quantity. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false))
    private Short quantity;

    /** The discount. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false))
    private Float discount;

    /** The extended price. */
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(precision = 19, scale = 4))
    private Double extendedPrice;

}
