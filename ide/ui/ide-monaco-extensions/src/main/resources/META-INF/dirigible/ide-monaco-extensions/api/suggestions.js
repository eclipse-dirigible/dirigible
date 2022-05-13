/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
let response = require("http/v4/response");
let request = require("http/v4/request");
let moduleInfoCache = require("ide-monaco-extensions/api/utils/moduleInfoCache");
let moduleInfo = moduleInfoCache.get(request.getParameter("moduleName"));
response.setContentType("application/json");
response.print(JSON.stringify(moduleInfo.suggestions));
response.flush();
response.close();