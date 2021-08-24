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
var streams = require('io/v3/streams');

var baos = streams.createByteArrayOutputStream();
baos.writeText("some text");
var result = baos.getBytes();
var bais = streams.createByteArrayInputStream(result);
result = bais.readText();

result == "some text";
