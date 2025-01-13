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
 * The Class CustomerDemographic.
 */
@EdmEntityType(name = "CustomerDemographic")
@EdmEntitySet(name = "CustomerDemographics", container = "NorthwindEntities")
public class CustomerDemographic {

    /** The customer type id. */
    @EdmKey
    @EdmProperty(name = "CustomerTypeID", facets = @EdmFacets(nullable = false, maxLength = 10))
    private String customerTypeId;

    /** The customer desc. */
    @EdmProperty
    private String customerDesc;

    /** The customers. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = Customer.class, //
            toRole = "Customers", //
            association = "CustomerCustomerDemo" //
    )
    private List<Customer> customers;
}
