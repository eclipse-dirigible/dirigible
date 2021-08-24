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
var writer = require('indexing/v4/writer');
var searcher = require('indexing/v4/searcher');

writer.add("index1", "myfile1", "apache lucene", new Date(), {"name1":"value1"});
writer.add("index1", "myfile2", "lucene - the search engine", new Date(), {"name2":"value2"});

var found = searcher.search("index1", "lucene");

console.log(JSON.stringify(found));

((found !== null) && (found !== undefined) && found.length === 2);