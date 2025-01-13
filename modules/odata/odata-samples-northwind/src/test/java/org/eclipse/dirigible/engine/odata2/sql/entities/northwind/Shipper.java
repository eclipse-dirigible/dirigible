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
 * The Class Shipper.
 */
@EdmEntityType(name = "Shipper")
@EdmEntitySet(name = "Shippers", container = "NorthwindEntities")
public class Shipper {

    /** The shipper id. */
    @EdmKey
    @EdmProperty(name = "ShipperID", facets = @EdmFacets(nullable = false))
    private Integer shipperId;

    /** The company name. */
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String companyName;

    /** The phone. */
    @EdmProperty(facets = @EdmFacets(maxLength = 24))
    private String phone;

    /** The orders. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = OrderDetail.class, //
            toRole = "Orders", //
            association = "FK_Orders_Shippers" //
    )
    private List<Order> orders;
}
