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
const perspectiveData = {
	id: 'processInbox',
	label: 'Process Inbox',
	translation: {
		key: 'inbox:processInbox',
	},
	path: '/services/web/inbox/index.html',
	order: -2,
	lazyLoad: true,
	icon: '/services/web/inbox/images/inbox.svg',
};
if (typeof exports !== 'undefined') {
	exports.getPerspective = () => perspectiveData;
}