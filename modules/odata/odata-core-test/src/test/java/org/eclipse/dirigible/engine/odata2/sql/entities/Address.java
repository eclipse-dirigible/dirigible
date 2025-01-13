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

import static org.eclipse.dirigible.engine.odata2.sql.entities.Owner.OWNER_2_ADDRESS_ASSOCIATION;


/**
 * The Class Address.
 */
@EdmEntityType(name = "Address")
@EdmEntitySet(name = "Addresses")
public class Address {


    /** The id. */
    @EdmKey
    @EdmProperty
    private String id;

    /** The street. */
    @EdmProperty
    private String street;

    /** The city. */
    @EdmProperty
    private String city;

    /** The postal code. */
    @EdmProperty
    private Integer postalCode;


    /** The owner. */
    @EdmNavigationProperty(toMultiplicity = EdmNavigationProperty.Multiplicity.ZERO_OR_ONE, toType = Owner.class,
            association = OWNER_2_ADDRESS_ASSOCIATION)
    private Owner owner;

}
