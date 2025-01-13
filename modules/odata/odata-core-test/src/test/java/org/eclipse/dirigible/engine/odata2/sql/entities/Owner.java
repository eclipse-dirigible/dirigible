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

import java.util.Calendar;
import java.util.List;

import static org.eclipse.dirigible.engine.odata2.sql.entities.Car.CAR_2_TOWNER_ASSOCIATION;

/**
 * The Class Owner.
 */
@EdmEntityType(name = "Owner")
@EdmEntitySet(name = "Owners")
public class Owner {

    /** The Constant OWNER_2_ADDRESS_ASSOCIATION. */
    static final String OWNER_2_ADDRESS_ASSOCIATION = "Owner2AddressAssociation";

    /** The id. */
    @EdmKey
    @EdmProperty
    private String id;

    /** The first name. */
    @EdmProperty
    private String firstName;

    /** The last name. */
    @EdmProperty
    private String lastName;

    /** The car. */
    @EdmNavigationProperty(toMultiplicity = EdmNavigationProperty.Multiplicity.ZERO_OR_ONE, toType = Car.class,
            association = CAR_2_TOWNER_ASSOCIATION)
    private Car car;

    /** The addresses. */
    @EdmNavigationProperty(toMultiplicity = EdmNavigationProperty.Multiplicity.MANY, toType = Address.class,
            association = OWNER_2_ADDRESS_ASSOCIATION)
    private List<Address> addresses;

    /** The purchase date. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Calendar purchaseDate;

}


