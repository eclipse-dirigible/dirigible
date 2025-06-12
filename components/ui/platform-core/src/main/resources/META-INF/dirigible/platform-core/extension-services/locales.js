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
import { request, response } from 'sdk/http';
import { extensions } from 'sdk/extensions';
import { registry } from 'sdk/platform';
import { uuid } from 'sdk/utils';

let commonPath = '';
const allLocales = [];
const lang = request.getParameter('translations');
const namespaces = request.getParameterValues('namespaces');
const extensionPoints = request.getParameterValues('extensionPoints') ?? ['platform-locales'];

function sort(a, b) {
	if (a.order !== undefined && b.order !== undefined) {
		return (parseInt(a.order) - parseInt(b.order));
	} else if (a.order === undefined && b.order === undefined) {
		return a.label < b.label ? -1 : 1
	} else if (a.order === undefined) {
		return 1;
	} else if (b.order === undefined) {
		return -1;
	}
	return 0;
}

function setETag() {
	const maxAge = 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader('ETag', etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

try {
	for (let i = 0; i < extensionPoints.length; i++) {
		// @ts-ignore
		const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
		for (let e = 0; e < extensionList.length; e++) {
			const locale = extensionList[e].getLocale();
			allLocales.push(locale);
			if (locale.id === lang) commonPath = locale.common;
		}
	}
	allLocales.sort(sort);
	let responseContent = {
		locales: allLocales,
		translations: lang ? { [lang]: {} } : undefined
	};
	if (lang && allLocales.some((locale) => locale.id === lang)) {
		const root = registry.getRoot();
		const modules = root.getDirectoriesNames();
		responseContent.translations[lang]['common'] = JSON.parse(registry.getText(commonPath));
		for (let p = 0; p < modules.length; p++) {
			if (namespaces && !namespaces.includes(modules[p])) continue;
			const langDir = root.getDirectory(`${modules[p]}/translations/${lang}`);
			if (langDir.exists()) {
				const jsons = langDir.getArtefactsNames();
				for (let j = 0; j < jsons.length; j++) {
					const translationPath = `/${modules[p]}/translations/${lang}/${jsons[j]}`;
					if (translationPath !== commonPath) {
						responseContent.translations[lang][modules[p]] = JSON.parse(registry.getText(translationPath))
					}
				}
			}
		}
	} else throw Error(`Language '${lang}' is not registered`);
	response.setContentType('application/json');
	response.println(JSON.stringify(responseContent));
	setETag();
} catch (e) {
	console.error(`Error while loading locale modules: ${e}`);
	response.sendError(500, `Error while loading locale modules: ${e}`);
}

response.flush();
response.close();