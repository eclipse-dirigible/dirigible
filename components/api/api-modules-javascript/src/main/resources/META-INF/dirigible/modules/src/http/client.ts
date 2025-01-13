/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * HTTP API Client
 *
 */

const HttpClientFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpClientFacade");

export interface HttpClientHeader {
    name: string;
    value: string;
}

export interface HttpClientParam {
    name: string;
    value: string;
}

export interface HttpClientRequestOptions {
    expectContinueEnabled?: boolean;
    proxyHost?: string;
    proxyPort?: number;
    cookieSpec?: string;
    redirectsEnabled?: boolean;
    relativeRedirectsAllowed?: boolean;
    circularRedirectsAllowed?: boolean;
    maxRedirects?: number;
    authenticationEnabled?: boolean;
    targetPreferredAuthSchemes?: string[];
    proxyPreferredAuthSchemes?: string[];
    connectionRequestTimeout?: number;
    connectTimeout?: number;
    socketTimeout?: number;
    contentCompressionEnabled?: boolean;
    sslTrustAllEnabled?: boolean;
    data?: any[];
    text?: string;
    files?: string[];
    characterEncoding?: string;
    characterEncodingEnabled?: boolean;
    contentType?: string;
    headers?: HttpClientHeader[];
    params?: HttpClientParam[];
    binary?: boolean;
    context?: { [key: string]: any };
}

export interface HttpClientResponse {
    statusCode: number;
    statusMessage: string;
    data: any[];
    text: string;
    protocol: string;
    binary: boolean;
    headers: HttpClientHeader[]
}

export class HttpClient {

    public static get(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.get(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    public static post(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.post(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    public static put(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.put(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    public static patch(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.patch(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    public static delete(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.delete(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    public static del(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        return HttpClient.delete(url, options);
    }

    public static head(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.head(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    public static trace(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.trace(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    private static buildUrl(url: string, options: HttpClientRequestOptions): string {
        if (options === undefined || options === null || options.params === undefined || options.params === null || options.params.length === 0) {
            return url;
        }
        for (let i = 0; i < options.params.length; i++) {
            if (i === 0) {
                url += '?' + options.params[i].name + '=' + options.params[i].value;
            } else {
                url += '&' + options.params[i].name + '=' + options.params[i].value;
            }
        }
        return url;
    }    
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = HttpClient;
}
