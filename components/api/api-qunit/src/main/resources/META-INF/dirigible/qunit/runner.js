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
	"use strict";
	exports.run = function (QUnit, config) {
		if (arguments.length == 1) {
			//try to guess using well-known properties in QUnit
			if (!arguments[0].version && !arguments[0].assert) {
				config = arguments[0];
				QUnit = undefined;
			}
		}
		config = config || {};
		var console_reporter, svc_reporter;
		if (!config["disable-console-reporter"])
			console_reporter = require("qunit/reporters/console-reporter");
		if (!config["disable-service-reporter"])
			svc_reporter = require("qunit/reporters/svc-reporter");
		if (!QUnit) {
			QUnit = require('qunit/qunit');
		} else {
			if (console_reporter)
				console_reporter.QUnit = QUnit;
			if (svc_reporter)
				svc_reporter.QUnit = QUnit;
		}
		require("test/runner").run({
			"execute": function () {
				QUnit.load();
			}.bind(this),
			"serviceReporter": svc_reporter
		});
	};
})();
