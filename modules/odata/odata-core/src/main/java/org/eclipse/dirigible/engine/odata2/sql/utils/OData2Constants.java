/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.utils;

import org.apache.olingo.odata2.api.processor.ODataContext;

/**
 * The Class OData2Constants.
 */
public final class OData2Constants {

    /** Name of the parameter at the {@link ODataContext} in which the tenantId is available. */
    public static final String ODATA_CTX_PARAMETER_TENANT_ID = "com.sap.it.commons.odata.api.ctx.parameter.tenantid";

    /** Name of the parameter at the {@link ODataContext} in which the tenantName is available. */
    public static final String ODATA_CTX_PARAMETER_TENANT_NAME = "com.sap.it.commons.odata.api.ctx.parameter.tenantname";

    /**
     * Name of the parameter at the {@link ODataContext} in which the JWT Token is available. For the
     * time being, this parameter is only available if the processor runs in a multicloud environment
     */
    public static final String ODATA_CTX_PARAMETER_JWT_TOKEN = "com.sap.it.commons.odata.api.ctx.parameter.jwttoken";
}
