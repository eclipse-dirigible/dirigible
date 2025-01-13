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
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;

/**
 * The Class Customer.
 */
@EdmEntityType(name = "Customer")
@EdmEntitySet(name = "Customers", container = "NorthwindEntities")
public class Customer {

    /** The customer id. */
    @EdmKey
    @EdmProperty(name = "CustomerID", facets = @EdmFacets(nullable = false, maxLength = 5))
    private String customerId;

    /** The company name. */
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String companyName;

    /** The contact name. */
    @EdmProperty(facets = @EdmFacets(maxLength = 30))
    private String contactName;

    /** The contact title. */
    @EdmProperty(facets = @EdmFacets(maxLength = 30))
    private String contactTitle;

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

    /** The phone. */
    @EdmProperty(facets = @EdmFacets(maxLength = 24))
    private String phone;

    /** The fax. */
    @EdmProperty(facets = @EdmFacets(maxLength = 24))
    private String fax;

    /** The orders. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = Order.class, //
            toRole = "Orders", //
            association = "FK_Orders_Customers" //
    )
    private List<Order> orders;

    /** The customer demographics. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = CustomerDemographic.class, //
            toRole = "CustomerDemographics", //
            association = "CustomerCustomerDemo" //
    )
    private List<CustomerDemographic> customerDemographics;
}
