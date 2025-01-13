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

import org.apache.olingo.odata2.api.annotation.edm.*;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;

/**
 * The Class Entity6.
 */
@EdmEntityType
@EdmEntitySet(name = "Entities6")
public class Entity6 {

    /** The Current employee id. */
    @EdmKey
    @EdmProperty
    private Long CurrentEmployeeId;

    /** The Current employee name. */
    @EdmKey
    @EdmProperty
    private String CurrentEmployeeName;

    /** The id. */
    @EdmKey
    @EdmProperty
    private Long ID;

    /** The name. */
    @EdmProperty
    private String NAME;
}
