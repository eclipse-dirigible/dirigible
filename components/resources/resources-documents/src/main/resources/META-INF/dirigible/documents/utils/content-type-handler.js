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
import { extensions } from '@aerokit/sdk/extensions';

let contentTypeExtension = null;

const contentTypeExtensions = await extensions.loadExtensionModules('ui-documents-content-type');
if (contentTypeExtensions !== null && contentTypeExtensions.length > 0) {
	contentTypeExtension = contentTypeExtensions[0];
}

export const getContentTypeBeforeUpload = (fileName, contentType) => {
	let extension = getContentTypeExtension();
	if (extension !== null) {
		return extension.getContentTypeBeforeUpload(fileName, contentType);
	}
	return contentType;
};

export const getContentTypeBeforeDownload = (fileName, contentType) => {
	let extension = getContentTypeExtension();
	if (extension !== null) {
		return extension.getContentTypeBeforeDownload(fileName, contentType);
	}
	return contentType;
};

function getContentTypeExtension() {
	return contentTypeExtension;
}
