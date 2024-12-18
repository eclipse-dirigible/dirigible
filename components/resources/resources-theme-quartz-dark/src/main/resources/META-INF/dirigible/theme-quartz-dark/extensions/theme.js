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
exports.getTheme = function () {
	return {
		id: 'quartz-dark',
		module: 'theme-quartz-dark',
		name: 'Quartz Dark',
		type: 'dark',
		version: 9,
		oldThemeId: 'default',
		links: [
			'/services/web/theme-quartz-light/css/quartz-fixes.css',
			'/webjars/sap-theming__theming-base-content/11.18.2/content/Base/baseLib/sap_fiori_3_dark/css_variables.css',
			'/webjars/fundamental-styles/0.38.0/dist/theming/sap_fiori_3_dark.css',
		]
	};
};


