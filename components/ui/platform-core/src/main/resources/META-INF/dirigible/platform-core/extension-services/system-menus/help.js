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
// @ts-nocheck
import { configurations as config } from 'sdk/core';

const defaultMenuItems = [
	{
		label: 'Help Portal',
		action: 'open',
		link: 'https://www.dirigible.io/help/',
		separator: false
	},
	{
		label: 'Contact Support',
		action: 'open',
		link: 'https://github.com/eclipse/dirigible/issues',
		separator: false
	},
	{
		label: 'Suggest a Feature',
		action: 'open',
		link: 'https://github.com/eclipse/dirigible/issues/new?assignees=&labels=&template=feature_request.md&title=[New%20Feature]',
		separator: false
	},
	{
		label: 'What\'s New',
		action: 'open',
		link: 'https://twitter.com/dirigible_io',
		separator: false
	},
	{
		label: 'Check for Updates',
		action: 'open',
		link: 'http://download.dirigible.io/',
		separator: true
	}
];

export const getMenu = () => {
	const menu = {
		label: 'Help',
		items: []
	};

	const brandingHelpItems = config.get('DIRIGIBLE_BRANDING_HELP_ITEMS', '');
	if (brandingHelpItems && typeof brandingHelpItems === 'string') {
		const helpItems = brandingHelpItems.split(',');
		helpItems.forEach(e => {
			const item = e.trim();
			menu.items.push({
				label: config.get(`DIRIGIBLE_BRANDING_HELP_ITEM_${item}_NAME`, item),
				order: parseInt(config.get(`DIRIGIBLE_BRANDING_HELP_ITEM_${item}_ORDER`, '0')),
				action: 'open',
				link: config.get(`DIRIGIBLE_BRANDING_HELP_ITEM_${item}_URL`, '#'),
				separator: config.get(`DIRIGIBLE_BRANDING_HELP_ITEM_${item}_DIVIDER`, 'false').toLowerCase() === 'true'
			});
		});
		menu.items.sort((a, b) => a.order - b.order);
	} else {
		menu.items = defaultMenuItems;
	}
	menu.items.push({
		id: 'about',
		label: 'About',
		action: 'openWindow',
		separator: false
	});
	return {
		systemMenu: true,
		id: 'help',
		menu: menu
	};
};