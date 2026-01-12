/**
 * Provides a JavaScript/TypeScript wrapper (Facade) for making synchronous HTTP requests.
 *
 */

const HttpClientFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpClientFacade");

/**
 * Defines a single HTTP header, used for both request and response.
 */
export interface HttpClientHeader {
    /** The name of the header (e.g., 'Content-Type', 'Authorization'). */
    name: string;
    /** The value of the header. */
    value: string;
}

/**
 * Defines a query parameter that will be appended to the URL.
 */
export interface HttpClientParam {
    /** The name of the URL query parameter (e.g., 'id'). */
    name: string;
    /** The value of the URL query parameter. */
    value: string;
}

/**
 * Defines a file to be included in a multi-part form data request.
 */
export interface HttpClientFile {
    /** The form field name for the file. */
    name: string;
    /** The file path or content (behavior depends on the underlying Java implementation). */
    value: string;
}

/**
 * Configuration options for an HTTP request, mirroring the capabilities of the underlying Java client.
 */
export interface HttpClientRequestOptions {
    /** Whether 'Expect: 100-Continue' handshake is enabled. Defaults to false. */
    expectContinueEnabled?: boolean;
    /** The proxy hostname to use for the request. */
    proxyHost?: string;
    /** The proxy port number. */
    proxyPort?: number;
    /** The cookie specification to use (e.g., 'default', 'netscape'). */
    cookieSpec?: string;
    /** Whether automatic redirects are enabled. Defaults to true. */
    redirectsEnabled?: boolean;
    /** Whether relative redirects should be allowed. Defaults to true. */
    relativeRedirectsAllowed?: boolean;
    /** Whether circular redirects (infinite loops) should be allowed. Defaults to false. */
    circularRedirectsAllowed?: boolean;
    /** The maximum number of redirects to follow. */
    maxRedirects?: number;
    /** Whether authentication handling is enabled. */
    authenticationEnabled?: boolean;
    /** Array of preferred authentication schemes for the target host. */
    targetPreferredAuthSchemes?: string[];
    /** Array of preferred authentication schemes for the proxy. */
    proxyPreferredAuthSchemes?: string[];
    /** Timeout in milliseconds for requesting a connection from the connection manager. */
    connectionRequestTimeout?: number;
    /** Timeout in milliseconds for establishing the connection. */
    connectTimeout?: number;
    /** Socket timeout (timeout for waiting for data) in milliseconds. */
    socketTimeout?: number;
    /** Whether automatic content compression (e.g., gzip) is enabled. */
    contentCompressionEnabled?: boolean;
    /** Whether to trust all SSL certificates (use only for testing non-production systems). */
    sslTrustAllEnabled?: boolean;
    /** Data to be sent as the request body, typically as an array of bytes/integers. */
    data?: any[];
    /** Text content to be sent as the request body. Takes precedence over `data`. */
    text?: string;
    /** Array of files for multi-part form data submissions. */
    files?: HttpClientFile[];
    /** The character encoding (charset) to use for the request body (e.g., 'UTF-8'). */
    characterEncoding?: string;
    /** Whether to enforce the character encoding. */
    characterEncodingEnabled?: boolean;
    /** The Content-Type header value for the request body. */
    contentType?: string;
    /** Array of custom headers to include in the request. */
    headers?: HttpClientHeader[];
    /** Array of query parameters to be appended to the URL. */
    params?: HttpClientParam[];
    /** If true, treats the response body as binary data. */
    binary?: boolean;
    /** Optional context map for advanced configuration of the underlying Java client. */
    context?: { [key: string]: any };
}

/**
 * The structure of the response returned by the HttpClient methods.
 */
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

/**
 * A static class providing methods for making synchronous HTTP requests.
 * All methods call the underlying Java Facade and parse the JSON response.
 */
export class HttpClient {

    /**
     * Executes a synchronous HTTP GET request.
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object containing status, headers, and body.
     */
    public static get(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        // The Java Facade expects the configuration options as a JSON string
        const response = HttpClientFacade.get(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * Executes a synchronous HTTP POST request.
     * @param url The target URL.
     * @param options Configuration options for the request, including request body in `text` or `data`.
     * @returns The parsed response object.
     */
    public static post(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.post(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * Executes a synchronous HTTP PUT request.
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object.
     */
    public static put(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.put(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * Executes a synchronous HTTP PATCH request.
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object.
     */
    public static patch(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.patch(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * Executes a synchronous HTTP DELETE request.
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object.
     */
    public static delete(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.delete(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * Alias for {@link HttpClient.delete}. Executes a synchronous HTTP DELETE request.
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object.
     */
    public static del(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        return HttpClient.delete(url, options);
    }

    /**
     * Executes a synchronous HTTP HEAD request (fetches headers only).
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object. The body (`text` and `data`) will typically be empty.
     */
    public static head(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.head(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * Executes a synchronous HTTP TRACE request.
     * @param url The target URL.
     * @param options Configuration options for the request.
     * @returns The parsed response object.
     */
    public static trace(url: string, options: HttpClientRequestOptions = {}): HttpClientResponse {
        const requestUrl = HttpClient.buildUrl(url, options);
        const response = HttpClientFacade.trace(requestUrl, JSON.stringify(options));
        return JSON.parse(response);
    }

    /**
     * @private
     * Builds the request URL by appending query parameters from options.params,
     * then removes `params` from the options object before passing it to the Java Facade.
     * @param url The base URL.
     * @param options The request options object.
     * @returns The URL with appended query parameters.
     */
    private static buildUrl(url: string, options: HttpClientRequestOptions): string {
        if (!options || !options.params || options.params.length === 0) {
            return url;
        }

        let processedUrl = url;
        let firstParam = true;

        for (const param of options.params) {
            if (firstParam) {
                // Check if the original URL already has query parameters
                if (processedUrl.includes('?')) {
                    processedUrl += '&';
                } else {
                    processedUrl += '?';
                }
                firstParam = false;
            } else {
                processedUrl += '&';
            }

            // Simple URL encoding of the value is assumed here for basic operation
            processedUrl += `${param.name}=${param.value}`;
        }

        // Remove params from options because they are now part of the URL string.
        delete options.params;

        return processedUrl;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = HttpClient;
}