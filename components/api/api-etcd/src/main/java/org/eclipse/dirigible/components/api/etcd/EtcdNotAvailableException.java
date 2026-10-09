/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.etcd;

/**
 * Thrown by every etcd API call when the etcd client library is not on the classpath. The library
 * is an add-on that the default bundle does not ship (#7783).
 */
public class EtcdNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The artifact an application adds to get the etcd client library. */
    public static final String ADD_ON_ARTIFACT = "org.eclipse.dirigible:dirigible-components-api-etcd-client";

    /**
     * Instantiates a new etcd not available exception.
     *
     * @param cause the failed lookup of the client library
     */
    EtcdNotAvailableException(Throwable cause) {
        super("the etcd client library is not bundled in this edition; add the " + ADD_ON_ARTIFACT
                + " add-on (<type>pom</type>) to the application", cause);
    }
}
