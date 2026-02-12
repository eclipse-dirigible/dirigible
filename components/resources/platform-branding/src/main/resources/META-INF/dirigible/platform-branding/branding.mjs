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
// @ts-nocheck
import { Configurations } from '@aerokit/sdk/core';

const BRANDING_NAME = 'DIRIGIBLE_BRANDING_NAME';
const BRANDING_BRAND = 'DIRIGIBLE_BRANDING_BRAND';
const BRANDING_BRAND_URL = 'DIRIGIBLE_BRANDING_BRAND_URL';
const BRANDING_FAVICON = 'DIRIGIBLE_BRANDING_FAVICON';
const BRANDING_LOGO = 'DIRIGIBLE_BRANDING_LOGO';
const BRANDING_THEME = 'DIRIGIBLE_BRANDING_THEME';
const BRANDING_PREFIX = 'DIRIGIBLE_BRANDING_PREFIX';
// const BRANDING_ANALYTICS = 'DIRIGIBLE_BRANDING_ANALYTICS';

const BRANDING_NAME_DEFAULT = 'Dirigible';
const BRANDING_BRAND_DEFAULT = 'Eclipse';
const BRANDING_BRAND_URL_DEFAULT = "https://www.dirigible.io/";
const BRANDING_FAVICON_DEFAULT = '/services/web/platform-branding/images/favicon.ico';
const BRANDING_LOGO_DEFAULT = '/services/web/platform-branding/images/dirigible.svg';
const BRANDING_THEME_DEFAULT = 'blimpkit-auto';
const BRANDING_PREFIX_DEFAULT = 'dirigible';

export function getBrandingJs() {
    return `if (!top.hasOwnProperty('PlatformBranding')) top.PlatformBranding = {
    name: '${Configurations.get(BRANDING_NAME, BRANDING_NAME_DEFAULT)}',
    brand: '${Configurations.get(BRANDING_BRAND, BRANDING_BRAND_DEFAULT)}',
    brandUrl: '${Configurations.get(BRANDING_BRAND_URL, BRANDING_BRAND_URL_DEFAULT)}',
    icons: {
        favicon: '${Configurations.get(BRANDING_FAVICON, BRANDING_FAVICON_DEFAULT)}',
    },
    logo: '${Configurations.get(BRANDING_LOGO, BRANDING_LOGO_DEFAULT)}',
	theme: '${Configurations.get(BRANDING_THEME, BRANDING_THEME_DEFAULT)}',
    prefix: '${Configurations.get(BRANDING_PREFIX, BRANDING_PREFIX_DEFAULT)}'
};`;
}

export function getKeyPrefix() {
    return Configurations.get(BRANDING_PREFIX, BRANDING_PREFIX_DEFAULT);
}

// export function getAnalyticsLink() {
//     return Configurations.get(BRANDING_ANALYTICS, '');
// }
