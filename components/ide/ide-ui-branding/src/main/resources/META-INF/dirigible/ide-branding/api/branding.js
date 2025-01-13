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
// Deprecated

import { rs } from 'sdk/http';
import { Configurations } from 'sdk/core';

let DIRIGIBLE_BRANDING_NAME = 'DIRIGIBLE_BRANDING_NAME';
let DIRIGIBLE_BRANDING_BRAND = 'DIRIGIBLE_BRANDING_BRAND';
let DIRIGIBLE_BRANDING_BRAND_URL = 'DIRIGIBLE_BRANDING_BRAND_URL';
let DIRIGIBLE_BRANDING_ICON = 'DIRIGIBLE_BRANDING_ICON';
let DIRIGIBLE_BRANDING_LOGO = 'DIRIGIBLE_BRANDING_LOGO';
let DIRIGIBLE_BRANDING_WELCOME_PAGE = 'DIRIGIBLE_BRANDING_WELCOME_PAGE';

let DIRIGIBLE_BRANDING_NAME_DEFAULT = 'Eclipse Dirigible';
let DIRIGIBLE_BRANDING_BRAND_DEFAULT = 'Eclipse Dirigible';
let DIRIGIBLE_BRANDING_BRAND_URL_DEFAULT = "https://www.dirigible.io/";
let DIRIGIBLE_BRANDING_ICON_DEFAULT = '/services/web/resources/images/favicon.png';
let DIRIGIBLE_BRANDING_LOGO_DEFAULT = '/services/web/resources/images/dirigible.svg';
let DIRIGIBLE_BRANDING_WELCOME_PAGE_DEFAULT = '/services/web/ide/welcome.html';

rs.service()
	.resource('')
	.get(function (ctx, request, response) {
		let branding = {
			'name': Configurations.get(DIRIGIBLE_BRANDING_NAME, DIRIGIBLE_BRANDING_NAME_DEFAULT),
			'brand': Configurations.get(DIRIGIBLE_BRANDING_BRAND, DIRIGIBLE_BRANDING_BRAND_DEFAULT),
			'brandUrl': Configurations.get(DIRIGIBLE_BRANDING_BRAND_URL, DIRIGIBLE_BRANDING_BRAND_URL_DEFAULT),
			'icon': Configurations.get(DIRIGIBLE_BRANDING_ICON, DIRIGIBLE_BRANDING_ICON_DEFAULT),
			'logo': Configurations.get(DIRIGIBLE_BRANDING_LOGO, DIRIGIBLE_BRANDING_LOGO_DEFAULT),
			'welcomePage': Configurations.get(DIRIGIBLE_BRANDING_WELCOME_PAGE, DIRIGIBLE_BRANDING_WELCOME_PAGE_DEFAULT)
		};
		response.println(JSON.stringify(branding));
	})
	.execute();