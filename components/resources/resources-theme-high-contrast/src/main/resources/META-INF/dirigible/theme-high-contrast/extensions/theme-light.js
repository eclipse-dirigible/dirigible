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
exports.getTheme = () => ({
	id: 'high-contrast-light',
	name: 'High Contrast Light',
	type: 'light',
	version: 2,
	links: [
		'/webjars/sap-theming__theming-base-content/content/Base/baseLib/sap_horizon_hcw/css_variables.css',
		'/webjars/fundamental-styles/dist/theming/sap_horizon_hcw.css',
		'/services/web/theme-high-contrast/css/contrast-light.css'
	]
});