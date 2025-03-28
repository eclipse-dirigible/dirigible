/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const shellData = {
	id: 'home',
	label: 'Home',
	link: '/services/web/portal/gen/index.html',
};
if (typeof exports !== 'undefined') {
	exports.getShell = () => shellData;
}