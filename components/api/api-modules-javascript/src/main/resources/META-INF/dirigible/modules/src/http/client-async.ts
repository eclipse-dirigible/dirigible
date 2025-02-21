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
/**
 * HTTP API ClientAsync
 *
 */

const HttpClientAsyncFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpClientAsyncFacade");
const SpringBeanProvider = Java.type("org.eclipse.dirigible.components.spring.SpringBeanProvider");
const httpClient = SpringBeanProvider.getBean(HttpClientAsyncFacade.class);

export class HttpAsyncClient {

	getAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.getAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.getAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	postAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.postAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.postAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	putAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.putAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.putAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	patchAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.patchAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.patchAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	deleteAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.deleteAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.deleteAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	headAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.headAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.headAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	traceAsync(url, config, options) {
		const newUrl = buildUrl(url, options);
		const callback = createHttpResponseCallback(
			httpClient,
			config.success,
			config.error,
			config.cancel
		);
		if (options) {
			httpClient.traceAsync(newUrl, JSON.stringify(options), callback);
		} else {
			httpClient.traceAsync(newUrl, JSON.stringify({}), callback);
		}
	};

	execute() {
		httpClient.execute();
	};
}

export function getInstance() {
	return new HttpAsyncClient();
};

function createHttpResponseCallback(httpClient, successCallback, errorCallback, cancelCallback) {
	return httpClient.createCallback(
		createSuccessCallback(successCallback),
		createErrorCallback(errorCallback),
		createCancelCallback(cancelCallback)
	);
}

function createSuccessCallback(callback) {
	return "(function(httpResponse, isBinary, context) {\n"
		+ "var response = {};\n"
		+ "response.statusCode = httpResponse.getStatusLine().getStatusCode();\n"
		+ "response.statusMessage = httpResponse.getStatusLine().getReasonPhrase();\n"
		+ "response.protocol = httpResponse.getProtocolVersion();\n"
		+ "response.binary = isBinary;\n"

		+ "var headers = httpResponse.getAllHeaders();\n"
		+ "response.headers = [];\n"
		+ "for (var i = 0; i < headers.length; i ++) {\n"
		+ "    response.headers.push({\n"
		+ "        name: headers[i].getName(),\n"
		+ "        value: headers[i].getValue()\n"
		+ "    });\n"
		+ "}\n"

		+ "var entity = httpResponse.getEntity();\n"
		+ "if (entity) {\n"
		+ "    var inputStream = entity.getContent();\n"
		+ "    if (isBinary) {\n"
		+ "        response.data = org.apache.commons.io.IOUtils.toByteArray(inputStream);\n"
		+ "    } else {\n"
		+ "        response.text = org.apache.commons.io.IOUtils.toString(inputStream);\n"
		+ "    }\n"
		+ "}\n"

		+ "(" + callback + ")(response, JSON.parse(context));\n"
		+ "})(__context.get('response'), __context.get('httpClientRequestOptions').isBinary(), __context.get('httpClientRequestOptions').getContext());\n";
}

function createErrorCallback(callback) {
	if (callback) {
		return "(" + callback + ")(__context.get('exception'))";
	}
}

function createCancelCallback(callback) {
	if (callback) {
		return "(" + callback + ")()";
	}
}

function buildUrl(url, options) {
	if (options === undefined || options === null || options.params === undefined || options.params === null || options.params.length === 0) {
		return url;
	}
	let newUrl = url;
	for (let i = 0; i < options.params.length; i ++) {
		if (i === 0) {
			newUrl += '?' + options.params[i].name + '=' + options.params[i].value;
		} else {
			newUrl += '&' + options.params[i].name + '=' + options.params[i].value;
		}
	}
	return newUrl;
}