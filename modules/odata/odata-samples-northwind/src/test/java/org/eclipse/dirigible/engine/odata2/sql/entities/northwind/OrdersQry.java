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
 * The Class OrdersQry.
 */
@EdmEntityType(name = "Orders_Qry")
@EdmEntitySet(name = "Orders_Qries", container = "NorthwindEntities")
public class OrdersQry {

    /** The order id. */
    @EdmKey
    @EdmProperty(name = "OrderID", facets = @EdmFacets(nullable = false))
    private Integer orderId;

    /** The customer id. */
    @EdmProperty(name = "CustomerID", facets = @EdmFacets(maxLength = 5))
    private String customerId;

    /** The employee id. */
    @EdmProperty(name = "EmployeeID")
    private Integer employeeId;

    /** The order date. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date orderDate;

    /** The required date. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date requiredDate;

    /** The shipped date. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date shippedDate;

    /** The ship via. */
    @EdmProperty
    private Integer shipVia;

    /** The freight. */
    @EdmProperty(type = EdmType.DECIMAL, facets = @EdmFacets(precision = 19, scale = 4))
    private Double freight;

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

    /** The company name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String companyName;

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

}
