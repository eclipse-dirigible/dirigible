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
var escape = require('utils/v4/escape');
var assertTrue = require('utils/assert').assertTrue;

var input = '"1,2,3,4,5,6"';
var result = escape.unescapeCsv(input);

assertTrue(result === '1,2,3,4,5,6');
