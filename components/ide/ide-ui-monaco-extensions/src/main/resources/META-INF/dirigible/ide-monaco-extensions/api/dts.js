/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { response } from "@dirigible/http";
import * as dtsParser from "./utils/dtsParser";
import * as javaDtsData from "./java-dts-data";

const dtsPaths = dtsParser.getDtsPaths();
const coreModulesDtsContent = dtsParser.getDtsFileContents(dtsPaths);
const dtsContent = javaDtsData.dts + coreModulesDtsContent;
response.setContentType('text/javascript');
response.println(dtsContent);