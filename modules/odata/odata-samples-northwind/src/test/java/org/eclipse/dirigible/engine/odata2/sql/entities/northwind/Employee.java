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
 * The Class Employee.
 */
@EdmEntityType(name = "Employee")
@EdmEntitySet(name = "Employees", container = "NorthwindEntities")
public class Employee {

    /** The employee id. */
    @EdmKey
    @EdmProperty(name = "EmployeeID", facets = @EdmFacets(nullable = false))
    private Integer employeeId;

    /** The last name. */
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 20))
    private String lastName;

    /** The first name. */
    @EdmProperty(facets = @EdmFacets(nullable = false, maxLength = 10))
    private String firstName;

    /** The title. */
    @EdmProperty(facets = @EdmFacets(maxLength = 30))
    private String title;

    /** The title of courtesy. */
    @EdmProperty(facets = @EdmFacets(maxLength = 25))
    private String titleOfCourtesy;

    /** The birth date. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date birthDate;

    /** The hire date. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date hireDate;

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

    /** The home phone. */
    @EdmProperty(facets = @EdmFacets(maxLength = 24))
    private String homePhone;

    /** The extension. */
    @EdmProperty(facets = @EdmFacets(maxLength = 4))
    private String extension;

    /** The photo. */
    @EdmProperty(type = EdmType.BINARY)
    private String photo;

    /** The notes. */
    @EdmProperty
    private String notes;

    /** The reports to. */
    @EdmProperty
    private Integer reportsTo;

    /** The photo path. */
    @EdmProperty(facets = @EdmFacets(maxLength = 255))
    private String photoPath;

    /** The employees. */
    @EdmNavigationProperty( //
            name = "Employees1", //
            toMultiplicity = Multiplicity.MANY, //
            toType = Employee.class, //
            toRole = "Employees" //
    )
    private List<Employee> employees;

    /** The employees 1. */
    @EdmNavigationProperty( //
            name = "Employee1", //
            toMultiplicity = Multiplicity.MANY, //
            toType = Employee.class, //
            toRole = "Employees1" //
    )
    private List<Employee> employees1;

    /** The orders. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = Order.class, //
            toRole = "Orders", //
            association = "FK_Orders_Employees" //
    )
    private List<Order> orders;

    /** The territories. */
    @EdmNavigationProperty( //
            toMultiplicity = Multiplicity.MANY, //
            toType = Territory.class, //
            toRole = "Territories", //
            association = "EmployeeTerritories" //
    )
    private List<Territory> territories;
}
