/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.entities;

import org.apache.olingo.odata2.api.annotation.edm.*;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;

import java.util.Date;
import java.util.List;

/**
 * The Class Car.
 */
@EdmEntityType(name = "Car")
@EdmEntitySet(name = "Cars")
public class Car {

    /** The Constant CAR_2_TOWNER_ASSOCIATION. */
    static final String CAR_2_TOWNER_ASSOCIATION = "Car2TOwnerAssociation";

    /** The Constant CAR_2_DRIVERS_ASSOCIATION. */
    static final String CAR_2_DRIVERS_ASSOCIATION = "Car2DriversAssociation";

    /** The id. */
    @EdmKey
    @EdmProperty
    private String id;

    /** The make. */
    @EdmProperty
    private String make;

    /** The model. */
    @EdmProperty
    private String model;

    /** The year. */
    @EdmProperty
    private Integer year;

    /** The price. */
    @EdmProperty
    private Double price;

    /** The updated. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date updated;

    /** The drivers. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.MANY, toType = Driver.class, association = CAR_2_DRIVERS_ASSOCIATION)
    private List<Driver> drivers;

    /** The owners. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.MANY, toType = Owner.class, association = CAR_2_TOWNER_ASSOCIATION)
    private List<Owner> owners;
}
