/**
 * Provides a JavaScript/TypeScript wrapper (Facade) for making asynchronous HTTP requests.
 *
 */

// --- Shared Interfaces (for documentation clarity) ---

/** Defines a single HTTP header, used for both request and response. */
export interface HttpClientHeader {
    /** The name of the header (e.g., 'Content-Type', 'Authorization'). */
    name: string;
    /** The value of the header. */
    value: string;
}

/** Defines a query parameter that will be appended to the URL. */
export interface HttpClientParam {
    /** The name of the URL query parameter (e.g., 'id'). */
    name: string;
    /** The value of the URL query parameter. */
    value: string;
}

/** Defines a file to be included in a multi-part form data request. */
export interface HttpClientFile {
    /** The form field name for the file. */
    name: string;
    /** The file path or content. */
    value: string;
}

/** Configuration options for an HTTP request, mirroring the capabilities of the underlying Java client. */
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
    files?: HttpClientFile[];
    characterEncoding?: string;
    characterEncodingEnabled?: boolean;
    contentType?: string;
    headers?: HttpClientHeader[];
    params?: HttpClientParam[];
    binary?: boolean;
    context?: { [key: string]: any };
}

/** The structure of the response returned by the HttpClient methods. */
export interface HttpClientResponse {
    /** The HTTP status code (e.g., 200, 404, 500). */
    statusCode: number;
    /** The status message returned by the server (e.g., 'OK', 'Not Found'). */
    statusMessage: string;
    /** Response body content as an array of bytes (if requested as binary). */
    data: any[];
    /** Response body content as a decoded string. */
    text: string;
    /** The protocol used (e.g., 'HTTP/1.1'). */
    protocol: string;
    /** Indicates if the response was processed as binary data. */
    binary: boolean;
    /** Array of all headers received in the response. */
    headers: HttpClientHeader[]
}

// --- Asynchronous Client Interfaces ---

/**
 * Defines the callback structure for asynchronous requests.
 * Note: Callbacks are provided as strings containing executable JavaScript code.
 */
export interface HttpClientAsyncConfig {
    /**
     * Mandatory success callback as a **string of JavaScript code**.
     * It will be executed with signature: `(response: HttpClientResponse, context: { [key: string]: any }) => void`
     */
    success: string;
    /**
     * Optional error callback as a **string of JavaScript code**.
     * It will be executed with signature: `(exception: any) => void`
     */
    error?: string;
    /**
     * Optional cancel callback as a **string of JavaScript code**.
     * It will be executed with signature: `() => void`
     */
    cancel?: string;
}


// --- Java Type Imports (Crucial for JVM integration) ---

/** Imports the native Java class responsible for asynchronous HTTP execution. */
const HttpClientAsyncFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpClientAsyncFacade");

/** Imports the native Java class for retrieving Spring-managed beans. */
const SpringBeanProvider = Java.type("org.eclipse.dirigible.components.spring.SpringBeanProvider");

/** Retrieves the singleton instance of the asynchronous HTTP client from the Spring context. */
const httpClient = SpringBeanProvider.getBean(HttpClientAsyncFacade.class);


/**
 * The asynchronous HTTP client class. All request methods return immediately
 * and execute callbacks upon completion.
 */
export class HttpAsyncClient {

    /**
     * Executes an asynchronous HTTP GET request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options (e.g., headers, body, params).
     */
	getAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Executes an asynchronous HTTP POST request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options.
     */
	postAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Executes an asynchronous HTTP PUT request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options.
     */
	putAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Executes an asynchronous HTTP PATCH request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options.
     */
	patchAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Executes an asynchronous HTTP DELETE request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options.
     */
	deleteAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Executes an asynchronous HTTP HEAD request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options.
     */
	headAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Executes an asynchronous HTTP TRACE request.
     * @param url The target URL.
     * @param config The callback configuration object.
     * @param options Request configuration options.
     */
	traceAsync(url: string, config: HttpClientAsyncConfig, options?: HttpClientRequestOptions): void {
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

    /**
     * Initiates the execution of queued asynchronous requests (depending on the underlying Java client's threading model).
     */
	execute(): void {
		httpClient.execute();
	};
}

/**
 * Factory function to retrieve a new instance of the asynchronous client.
 * @returns A new instance of HttpAsyncClient.
 */
export function getInstance(): HttpAsyncClient {
	return new HttpAsyncClient();
};

/**
 * Creates a native Java callback object that wraps the JavaScript success, error, and cancel callbacks.
 * This function uses the underlying Java HTTP client instance to generate the final callback object.
 * @param httpClient The Java HttpClientAsyncFacade instance.
 * @param successCallback JS code string for success.
 * @param errorCallback JS code string for error.
 * @param cancelCallback JS code string for cancel.
 * @returns A Java callback object.
 */
function createHttpResponseCallback(httpClient: any, successCallback: string, errorCallback?: string, cancelCallback?: string): any {
	return httpClient.createCallback(
		createSuccessCallback(successCallback),
		createErrorCallback(errorCallback),
		createCancelCallback(cancelCallback)
	);
}

/**
 * Generates the raw JavaScript string that will be executed in the JVM environment upon successful request completion.
 * This string handles parsing the Java HTTP response object and executing the user's defined callback function.
 * @param callback The user-provided success callback function code string.
 * @returns The complex JavaScript string for the success handler.
 */
function createSuccessCallback(callback: string): string {
	// Note: This string uses Java utility classes (org.apache.commons.io.IOUtils) and context variables 
    // (__context) that are available in the specific JVM environment.
    
    // The structure maps the Java HTTP Response object into the standard HttpClientResponse interface.
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
		+ "        // Uses Java IOUtils to read binary data\n"
		+ "        response.data = org.apache.commons.io.IOUtils.toByteArray(inputStream);\n"
		+ "    } else {\n"
		+ "        // Uses Java IOUtils to read text data\n"
		+ "        response.text = org.apache.commons.io.IOUtils.toString(inputStream);\n"
		+ "    }\n"
		+ "}\n"

		+ "(" + callback + ")(response, JSON.parse(context));\n"
		+ "})(__context.get('response'), __context.get('httpClientRequestOptions').isBinary(), __context.get('httpClientRequestOptions').getContext());\n";
}

/**
 * Generates the raw JavaScript string for the error callback.
 * @param callback The user-provided error callback function code string.
 * @returns The JavaScript string for the error handler, or undefined.
 */
function createErrorCallback(callback?: string): string | undefined {
	if (callback) {
		return "(" + callback + ")(__context.get('exception'))";
	}
    return undefined;
}

/**
 * Generates the raw JavaScript string for the cancel callback.
 * @param callback The user-provided cancel callback function code string.
 * @returns The JavaScript string for the cancel handler, or undefined.
 */
function createCancelCallback(callback?: string): string | undefined {
	if (callback) {
		return "(" + callback + ")()";
	}
    return undefined;
}

/**
 * Builds the final URL by appending all query parameters defined in options.params.
 * The parameters are not removed from the options object as they are for the synchronous client,
 * since the options are passed as JSON string to the facade anyway.
 * @param url The base URL.
 * @param options The request options which may contain `params`.
 * @returns The URL with appended query parameters.
 */
function buildUrl(url: string, options?: HttpClientRequestOptions): string {
	if (options === undefined || options === null || options.params === undefined || options.params === null || options.params.length === 0) {
		return url;
	}
	let newUrl = url;
	for (let i = 0; i < options.params.length; i ++) {
        const param = options.params[i];
		if (i === 0) {
            // Check if URL already contains '?' and use '&' if so (improves robustness)
            if (newUrl.includes('?')) {
                newUrl += '&';
            } else {
                newUrl += '?';
            }
			newUrl += param.name + '=' + param.value;
		} else {
			newUrl += '&' + param.name + '=' + param.value;
		}
	}
	return newUrl;
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = { HttpAsyncClient, getInstance };
}