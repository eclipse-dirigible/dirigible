/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.qldb;

/**
 * Thrown when a {@link QLDBRepository} is opened and the Amazon QLDB driver is not on the
 * classpath. The driver is an add-on that the default bundle does not ship (#7783).
 */
public class QldbNotAvailableException extends QLDBRepositoryException {

    private static final long serialVersionUID = 1L;

    /** The artifact an application adds to get the QLDB driver. */
    public static final String ADD_ON_ARTIFACT = "org.eclipse.dirigible:dirigible-components-api-qldb-driver";

    /**
     * Instantiates a new QLDB not available exception.
     *
     * @param cause the failed lookup of the driver
     */
    QldbNotAvailableException(Throwable cause) {
        super("the Amazon QLDB driver is not bundled in this edition; add the " + ADD_ON_ARTIFACT
                + " add-on (<type>pom</type>) to the application", cause);
    }
}
