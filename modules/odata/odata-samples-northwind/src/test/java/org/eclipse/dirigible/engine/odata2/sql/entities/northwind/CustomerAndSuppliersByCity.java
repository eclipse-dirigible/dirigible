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

/**
 * The Class CustomerAndSuppliersByCity.
 */
@EdmEntityType(name = "Customer_and_Suppliers_by_City")
@EdmEntitySet(name = "Customer_and_Suppliers_by_Cities", container = "NorthwindEntities")
public class CustomerAndSuppliersByCity {

    /** The city. */
    @EdmProperty(facets = @EdmFacets(maxLength = 15))
    private String city;

    /** The company name. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 40))
    private String companyName;

    /** The contact name. */
    @EdmProperty(facets = @EdmFacets(maxLength = 30))
    private String contactName;

    /** The relationship. */
    @EdmKey
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 9))
    private String relationship;
}
