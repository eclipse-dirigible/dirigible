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
import { rs } from "sdk/http";
import { escape } from "sdk/utils";
import { streams } from "sdk/io";
import { repository as repositoryManager } from "sdk/platform";
import { configurations } from "sdk/core";
import * as themesManager from "./extensions/themes";
import * as cacheUtils from "./utils/cache";

let THEME_CACHE = cacheUtils.getCache();

let PATH_REGISTRY_PUBLIC = '/registry/public';
let DIRIGIBLE_THEME_DEFAULT = 'DIRIGIBLE_THEME_DEFAULT';
let DEFAULT_THEME = 'default';
let NAME_PARAM = 'name';
let THEME_COOKIE = 'dirigible-theme';
let DEFAULT_THEME_MODULE_NAME = 'theme-';

rs.service()
	.resource('')
	.get(function (ctx, request, response) {
		let theme = getCurrentTheme(request, response);
		response.setContentType('text/plain');
		response.print(theme);
	})
	.resource('themes')
	.get(function (ctx, request, response) {
		let legacy = ctx.queryParameters.legacy;
		if (legacy === "false") legacy = false;
		else legacy = true;
		let themes = themesManager.getThemes(legacy);
		response.setContentType('application/json');
		response.print(JSON.stringify(themes));
	})
	.resource('{path}')
	.get(function (ctx, request, response) {
		let path = ctx.pathParameters.path;

		if (isCached(request, path)) {
			response.setContentType('text/css');
			response.setHeader('ETag', getTag(request));
			response.setStatus(response.NOT_MODIFIED);
		} else {
			let content = getContent(request, response, path);

			if (content !== null && content !== '') {
				let tag = cacheResource(path);

				response.setContentType('text/css');
				response.setStatus(response.OK);
				response.setHeader('ETag', tag);
				response.setHeader('Cache-Control', 'public, must-revalidate, max-age=0');
				response.write(content);
			} else {
				response.setStatus(response.NOT_FOUND);
				response.println('');
			}
		}
	})
	.execute();

function getContent(request, response, path) {
	let content = null;
	let cookieValue = getCurrentTheme(request, response);
	let themes = themesManager.getThemes();

	let themeModule = null;
	for (let i = 0; i < themes.length; i++) {
		if (themes[i].id === cookieValue) {
			themeModule = themes[i].module;
			break;
		}
	}
	if (themes.length === 0) {
		themeModule = DEFAULT_THEME_MODULE_NAME + cookieValue;
	}

	let resource = repositoryManager.getResource(PATH_REGISTRY_PUBLIC + '/' + themeModule + '/' + path);
	if (resource.exists()) {
		let resourceContent = resource.getContent();
		let repositoryInputStream = streams.createByteArrayInputStream(resourceContent);
		content = repositoryInputStream.readBytes();
	} else {
		let inputStream = streams.getResourceAsByteArrayInputStream('/' + themeModule + '/' + path);
		content = inputStream.readBytes();
	}
	return content;
}

function getCurrentTheme(request, response) {
	let env = configurations.get(DIRIGIBLE_THEME_DEFAULT);
	let cookieValue = env ? env : DEFAULT_THEME;
	let themeId = request.getParameter(NAME_PARAM);
	themeId = escape.escapeHtml4(themeId);
	themeId = escape.escapeJavascript(themeId);
	if (themeId !== null && themeId !== '') {
		setThemeCookie(response, themeId);
		THEME_CACHE.clear();
		cookieValue = themeId;
	} else {
		let themeCookie = getThemeCookie(request);
		cookieValue = themeCookie !== null ? themeCookie : cookieValue;
	}

	cookieValue = escape.escapeHtml4(cookieValue);
	cookieValue = escape.escapeJavascript(cookieValue);
	let themes = themesManager.getThemes();
	let themeFound = false;
	themes.forEach(function (e) {
		if (e.id === cookieValue) {
			themeFound = true;
		}
	})
	cookieValue = themeFound ? cookieValue : DEFAULT_THEME;
	return cookieValue;
}

function getThemeCookie(request) {
	let cookies = request.getCookies();
	for (let i = 0; cookies !== null && i < cookies.length; i++) {
		if (cookies[i].name === THEME_COOKIE) {
			return cookies[i].value;
		}
	}
	return null;
}

function setThemeCookie(response, themeId) {
	response.addCookie({
		'name': THEME_COOKIE,
		'value': themeId,
		'path': '/',
		'maxAge': 30 * 24 * 60 * 60
	});
}

function cacheResource(path) {
	let tag = THEME_CACHE.generateTag();
	THEME_CACHE.setTag(path, tag);
	return tag;
}

function isCached(request, path) {
	let tag = getTag(request);
	let cachedTag = THEME_CACHE.getTag(path);
	return tag === null || cachedTag === null ? false : tag === cachedTag;
}

function getTag(request) {
	return request.getHeader('If-None-Match');
}
