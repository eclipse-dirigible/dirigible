/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/*
 * The TENANT's country (DIRIGIBLE_APPLICATION_COUNTRY, an ISO 3166-1 alpha-2 code such as "BG",
 * blank when the deployment declares none - tenant-overridable through the tenant configuration).
 * The shared i18n runtime resolves a generated application's country-scoped field labels against
 * it: what a national identifier is called is a property of the company, not of the language its
 * users read the UI in.
 */
import { configurations } from "@aerokit/sdk/core";
import { response } from "@aerokit/sdk/http";

const configured = configurations.get("DIRIGIBLE_APPLICATION_COUNTRY", "") || "";
const country = configured.trim().toUpperCase();

response.setContentType("application/json");
response.println(JSON.stringify({ country: country }));
response.flush();
response.close();
