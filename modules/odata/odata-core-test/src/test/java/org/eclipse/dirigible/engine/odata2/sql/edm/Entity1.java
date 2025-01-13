/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.edm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmType;

/**
 * The Class Entity1.
 */
@EdmEntityType
@EdmEntitySet(name = "Entities1")
public class Entity1 {

    /** The message guid. */
    @EdmKey
    @EdmProperty
    private String messageGuid;

    /** The log start. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date logStart;

    /** The log end. */
    @EdmProperty(type = EdmType.DATE_TIME)
    private Date logEnd;

    /** The sender. */
    @EdmProperty
    private String sender;

    /** The receiver. */
    @EdmProperty
    private String receiver;

    /** The status. */
    @EdmProperty
    private String status;

    /** The alternate web link. */
    @EdmProperty
    private String alternateWebLink;

    /** The entity 2. */
    @EdmNavigationProperty(toMultiplicity = Multiplicity.MANY, toType = Entity2.class, association = "Entities2OfEntity1")
    private List<Entity2> entity2 = new ArrayList<Entity2>();

}
