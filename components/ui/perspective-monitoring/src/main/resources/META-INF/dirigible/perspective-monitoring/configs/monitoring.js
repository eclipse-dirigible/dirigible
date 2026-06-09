/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const perspectiveData = {
	id: 'monitoring',
	label: 'Monitoring',
	path: '/services/web/perspective-monitoring/index.html',
	order: 1100,
	icon: '/services/web/perspective-monitoring/images/monitoring.svg',
};
if (typeof exports !== 'undefined') {
	exports.getPerspective = () => perspectiveData;
}
