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

import java.util.List;

/**
 * The Class Group.
 */
@EdmEntityType(name = "Group")
@EdmEntitySet(name = "Groups")
public class Group {

    /** The Constant USER_2_GROUP_ASSOCIATION. */
    static final String USER_2_GROUP_ASSOCIATION = "UserToGroup";

    /** The id. */
    @EdmKey
    @EdmProperty
    private String id;

    /** The name. */
    @EdmProperty
    private String name;

    /** The users. */
    @EdmNavigationProperty(toMultiplicity = EdmNavigationProperty.Multiplicity.MANY, toType = User.class,
            association = USER_2_GROUP_ASSOCIATION)
    private List<User> users;
}
