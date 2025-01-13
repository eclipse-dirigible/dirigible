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
var QUnit = require("qunit/qunit");

QUnit.module('Module 1:');

QUnit.test("Test 1", function(assert) {
	assert.ok(true, 'Passing assertion');
	assert.ok(false, 'Failing assertion');
});

require("qunit/runner").run();
