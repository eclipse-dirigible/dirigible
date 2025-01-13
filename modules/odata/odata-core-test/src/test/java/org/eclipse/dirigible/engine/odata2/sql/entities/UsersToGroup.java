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

import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmKey;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;

/**
 * The Class UsersToGroup.
 */
@EdmEntityType(name = "UsersToGroup")
@EdmEntitySet(name = "UsersToGroup")
public class UsersToGroup {

    /** The User id. */
    // @EdmKey
    @EdmProperty
    private String UserId;

    /** The Group id. */
    // @EdmKey
    @EdmProperty
    private String GroupId;
}
