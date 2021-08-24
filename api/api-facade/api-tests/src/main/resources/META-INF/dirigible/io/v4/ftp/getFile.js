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
var ftp = require("io/v4/ftp");

var host = "test.rebex.net";
var port = 21;
var userName = "demo";
var password = "password";

var ftpClient = ftp.getClient(host, port, userName, password);
var fileText = ftpClient.getFileText("/", "readme.txt");

fileText !== undefined && fileText !== null;