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
var java = require('core/v3/java');

exports.deployProcess = function(location) {
	var processId = java.call('org.eclipse.dirigible.api.v3.bpm.BpmFacade', 'deployProcess', [location]);
	return processId;
};

exports.undeployProcess = function(processId) {
	java.call('org.eclipse.dirigible.api.v3.bpm.BpmFacade', 'undeployProcess', [processId]);
};

