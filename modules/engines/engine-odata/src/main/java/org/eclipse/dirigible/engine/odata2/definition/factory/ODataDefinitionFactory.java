/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.definition.factory;

import java.sql.Timestamp;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.dirigible.api.v3.security.UserFacade;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.engine.odata2.definition.ODataDefinition;

public class ODataDefinitionFactory {
	
	public static ODataDefinition parseOData(String contentPath, String data) {
		ODataDefinition odataDefinition = GsonHelper.GSON.fromJson(data, ODataDefinition.class);
		odataDefinition.setLocation(contentPath);
		odataDefinition.setHash(DigestUtils.md5Hex(data));
		odataDefinition.setCreatedBy(UserFacade.getName());
		odataDefinition.setCreatedAt(new Timestamp(new java.util.Date().getTime()));
		return odataDefinition;
	}

}
