/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

import java.io.Serializable;

/**
 * The Interface Tenant.
 */
public interface Tenant extends Serializable {

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Checks if is default.
     *
     * @return true, if is default
     */
    boolean isDefault();

    /**
     * Gets the name.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the subdomain.
     *
     * @return the subdomain
     */
    String getSubdomain();
}
