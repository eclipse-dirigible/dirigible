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
 * The PLATFORM's supported language set (DIRIGIBLE_APPLICATION_LANGUAGES, default "en,bg").
 * The Region & Language picker in every Harmonia shell offers exactly this set; individual
 * modules never define what languages the stack supports - they only provide translations,
 * falling back to the first (default) language for anything missing.
 */
import { configurations } from "@aerokit/sdk/core";
import { response } from "@aerokit/sdk/http";

const configured = configurations.get("DIRIGIBLE_APPLICATION_LANGUAGES", "en,bg");
const languages = configured.split(",")
                            .map(code => code.trim())
                            .filter(code => code.length > 0);

response.setContentType("application/json");
response.println(JSON.stringify(languages.length > 0 ? languages : ["en"]));
response.flush();
response.close();
