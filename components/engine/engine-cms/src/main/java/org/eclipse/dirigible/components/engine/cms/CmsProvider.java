/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

/**
 * The Interface CmsProvider.
 */
public interface CmsProvider {

    /** The Constant DIRIGIBLE_CMS_INTERNAL_VERSIONING_ENABLED. */
    String DIRIGIBLE_CMS_INTERNAL_VERSIONING_ENABLED = "DIRIGIBLE_CMS_INTERNAL_VERSIONING_ENABLED"; //$NON-NLS-1$

    /**
     * Getter for the underlying repository session object.
     *
     * @return the session object
     */
    Object getSession();

    /**
     * Getter for the underlying repository type object.
     *
     * @return the type of the object
     */
    String getType();

}
