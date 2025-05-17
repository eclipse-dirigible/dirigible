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
exports.getMenu = () => ({
	systemMenu: true,
	id: 'help',
	menu: {
		translation: 'help',
		label: 'Help',
		items: [
			{
				translation: 'menu-help:portal',
				label: 'Help Portal',
				action: 'open',
				link: 'https://www.dirigible.io/help/',
				separator: false
			},
			{
				translation: 'menu-help:support',
				label: 'Contact Support',
				action: 'open',
				link: 'https://github.com/eclipse-dirigible/dirigible/issues',
				separator: false
			},
			{
				translation: 'menu-help:feature',
				label: 'Suggest a Feature',
				action: 'open',
				link: 'https://github.com/eclipse-dirigible/dirigible/issues/new?assignees=&labels=&template=feature_request.md&title=[New%20Feature]',
				separator: false
			},
			{
				translation: 'menu-help:whatsNew',
				label: 'What\'s New',
				action: 'open',
				link: 'https://x.com/dirigible_io',
				separator: false
			},
			{
				translation: 'menu-help:updates',
				label: 'Check for Updates',
				action: 'open',
				link: 'http://download.dirigible.io/',
				separator: true
			},
			{
				translation: 'menu-help:about',
				label: 'About',
				action: 'openWindow',
				separator: false
			}
		]
	}
});