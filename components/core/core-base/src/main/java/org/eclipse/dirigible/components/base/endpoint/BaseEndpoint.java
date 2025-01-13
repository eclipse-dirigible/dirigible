/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.endpoint;

/**
 * The Class BaseEndpoint.
 */
public abstract class BaseEndpoint {

    /** The Constant DEFAULT_PAGE_SIZE. */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /** The Constant PREFIX_ENDPOINT_SECURED. */
    public static final String PREFIX_ENDPOINT_SECURED = "services/";

    /** The Constant PREFIX_ENDPOINT_CORE. */
    public static final String PREFIX_ENDPOINT_CORE = "services/core/";

    /** The Constant PREFIX_ENDPOINT_DATA. */
    public static final String PREFIX_ENDPOINT_DATA = "services/data/";

    /** The Constant PREFIX_ENDPOINT_IDE. */
    public static final String PREFIX_ENDPOINT_IDE = "services/ide/";

    /** The Constant PREFIX_ENDPOINT_BPM. */
    public static final String PREFIX_ENDPOINT_BPM = "services/bpm";

    /** The Constant PREFIX_ENDPOINT_SECURITY. */
    public static final String PREFIX_ENDPOINT_SECURITY = "services/security/";

    /** The Constant PREFIX_ENDPOINT_PUBLIC. */
    public static final String PREFIX_ENDPOINT_PUBLIC = "public/";

    /** The Constant PREFIX_ENDPOINT_WEBSOCKETS. */
    public static final String PREFIX_ENDPOINT_WEBSOCKETS = "/websockets/";

    /**
     * Instantiates a new base endpoint.
     */
    public BaseEndpoint() {
        super();
    }

}
