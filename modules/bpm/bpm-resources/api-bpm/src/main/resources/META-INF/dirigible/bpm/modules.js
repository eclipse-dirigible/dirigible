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
exports.getDeployer = function() {
	var deployer = require('bpm/v4/deployer');
	return deployer;
};

exports.getProcess = function() {
	var process = require('bpm/v4/process');
	return process;
};

exports.getTasks = function() {
	var tasks = require('bpm/v4/tasks');
	return tasks;
};
