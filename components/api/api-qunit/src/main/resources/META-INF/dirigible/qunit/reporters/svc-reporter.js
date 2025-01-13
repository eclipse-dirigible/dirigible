/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
(function () {
	'use strict';

	var response = require("http/response");

	var QUnit;
	if (this) {
		QUnit = this.QUnit || require("qunit/qunit");
	} else {
		QUnit = require("qunit/qunit");
	}

	var attachJSONhandler = function () {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		var data = {
			tests: [],
			moduleTests: []
		};

		QUnit.moduleDone(function (details) {
			data.moduleTests.push(details);
		});
		QUnit.testDone(function (details) {
			data.tests.push(details);
		});
		QUnit.done(function (details) {
			data.testSuite = details;
			response.print(JSON.stringify(data));
		});
	};

	var attachJUnitXmlhandler = function () {
		response.setContentType("text/xml; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		require("qunit/reporters/reporter-junit").QUnit = QUnit;

		QUnit.jUnitDone(function (report) {
			response.print(report.xml);
		});
	};

	var svc_reporters = {
		"json": attachJSONhandler,
		"xml": attachJUnitXmlhandler
	};

	exports.forMedia = function (media) {
		svc_reporters[media].apply(this);
	};

})();
