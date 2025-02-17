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
const perspectiveData = {
	id: 'perspectiveDocuments',
	label: 'Documents',
	path: '/services/web/perspective-documents/documents.html',
	order: 150,
	icon: '/services/web/perspective-documents/images/documents.svg',
};
const viewData = {
	lazyLoad: true,
	region: 'center',
	...perspectiveData
};
if (typeof exports !== 'undefined') {
	exports.getPerspective = () => perspectiveData;
	exports.getView = () => viewData;
}