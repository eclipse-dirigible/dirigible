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
import { request, response } from '@aerokit/sdk/http';
import { extensions } from '@aerokit/sdk/extensions';
import { registry } from '@aerokit/sdk/platform';
import { uuid } from '@aerokit/sdk/utils';

const langs = request.getParameterValues('langs');
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

function getTranslations(lang, commonPath) {
	let translations = {};
	const root = registry.getRoot();
	const modules = root.getDirectoriesNames();
	translations['common'] = JSON.parse(registry.getText(commonPath));
	for (let p = 0; p < modules.length; p++) {
		if (namespaces && !namespaces.includes(modules[p])) continue;
		const langDir = root.getDirectory(`${modules[p]}/translations/${lang}`);
		if (langDir.exists()) {
			const jsons = langDir.getArtefactsNames();
			for (let j = 0; j < jsons.length; j++) {
				const translationPath = `/${modules[p]}/translations/${lang}/${jsons[j]}`;
				if (translationPath !== commonPath) {
					if (translations[modules[p]]) Object.assign(translations[modules[p]], JSON.parse(registry.getText(translationPath)));
					else translations[modules[p]] = JSON.parse(registry.getText(translationPath));
				}
			}
		}
	}
	return translations;
}

const allLocales = [];
let responseContent = {
	locales: allLocales,
	translations: {}
};

try {
	for (let i = 0; i < extensionPoints.length; i++) {
		// @ts-ignore
		const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
		for (let e = 0; e < extensionList.length; e++) {
			const locale = extensionList[e].getLocale();
			allLocales.push(locale);
		}
	}
	allLocales.sort(sort);
	if (langs && langs.every((lng) => allLocales.some((locale) => locale.id === lng))) {
		for (let l = 0; l < langs.length; l++) {
			const locale = allLocales.find((locale) => locale.id === langs[l]);
			responseContent.translations[locale.id] = getTranslations(locale.id, locale.common);
		}
	} else if (langs === null) {
		for (let l = 0; l < allLocales.length; l++) {
			responseContent.translations[allLocales[l].id] = getTranslations(allLocales[l].id, allLocales[l].common);
		}
	} else throw Error(`Language(s) '${langs}' not registered`);
	response.setContentType('application/json');
	response.println(JSON.stringify(responseContent));
	setETag();
} catch (e) {
	console.error(`Error while loading locale modules: ${e}`);
	response.sendError(500, `Error while loading locale modules: ${e}`);
}

response.flush();
response.close();
