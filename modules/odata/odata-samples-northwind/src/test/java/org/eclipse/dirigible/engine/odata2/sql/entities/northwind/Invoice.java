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

import java.util.Date;

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmType;

/**
 * The Class Invoice.
 */
@EdmEntityType(name = "Invoice")
@EdmEntitySet(name = "Invoices", container = "NorthwindEntities")
public class Invoice {

    /** The ship name. */
    @EdmProperty(facets = @EdmFacets(maxLength = 40))
    private String shipName;

    /** The ship address. */
    @EdmProperty(facets = @EdmFacets(maxLength = 60))
    private String shipAddress;

    /** The ship city. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String shipCity;

    /** The ship region. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String shipRegion;

    /** The ship postal code. */
    @EdmProperty(facets = @EdmFacets(maxLength = 10))
    private String shipPostalCode;

    /** The ship country. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String shipCountry;

    /** The customer id. */
    @EdmProperty(name = "CustomerID", facets = @EdmFacets(maxLength = 5))
    private String customerId;

    /** The customer name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String customerName;

    /** The address. */
    @EdmProperty(facets = @EdmFacets(maxLength = 60))
    private String address;

    /** The city. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String city;

    /** The region. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String region;

    /** The postal code. */
    @EdmProperty(facets = @EdmFacets(maxLength = 10))
    private String postalCode;

    /** The country. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String country;

    /** The salesperson. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 31))
    private String salesperson;

    /** The order id. */
    @EdmKey
    @EdmProperty(name = "OrderID", facets = @EdmFacets(nullable = false))
    private Integer orderId;

    /** The order date. */
    @EdmProperty
    private Date orderDate;

    /** The required date. */
    @EdmProperty
    private Date requiredDate;

    /** The shipped date. */
    @EdmProperty
    private Date shippedDate;

    /** The shipper name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String shipperName;

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

    /** The freight. */
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(precision = 19, scale = 4))
    private Double freight;

}
