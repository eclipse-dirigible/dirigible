/**
 * Provides a static façade (`Response` class) for managing the HTTP response.
 * This class wraps a native Java HTTP response object, offering methods for setting
 * status codes, headers, cookies, and writing content (text, JSON, or binary).
 */

import { OutputStream } from "@aerokit/sdk/io/streams"

const HttpResponseFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpResponseFacade");
const OutputStreamWriter = Java.type("java.io.OutputStreamWriter");
const StandardCharsets = Java.type("java.nio.charset.StandardCharsets");

/**
 * Defines the structure for an HTTP cookie, including its name, value, and optional attributes.
 */
export interface Cookie {
    /** The name of the cookie. */
    name: string;
    /** The value of the cookie. */
    value: string;
    /** Key-value map of cookie attributes (e.g., 'maxAge', 'path', 'domain', 'secure', 'httpOnly'). */
    attributes: { [key: string]: string };
 }

/**
 * The static Response class providing standardized HTTP status codes and methods
 * for constructing the server's response.
 */
export class Response {

    // --- Standard HTTP Status Code Constants (Informational, Success, Redirection, Client Error, Server Error) ---

    public static readonly ACCEPTED = 202;
    public static readonly BAD_GATEWAY = 502;
    public static readonly BAD_REQUEST = 400;
    public static readonly CONFLICT = 409;
    public static readonly CONTINUE = 100;
    public static readonly CREATED = 201;
    public static readonly EXPECTATION_FAILED = 417;
    public static readonly FORBIDDEN = 403;
    public static readonly FOUND = 302;
    public static readonly GATEWAY_TIMEOUT = 504;
    public static readonly GONE = 410;
    public static readonly HTTP_VERSION_NOT_SUPPORTED = 505;
    public static readonly INTERNAL_SERVER_ERROR = 500;
    public static readonly LENGTH_REQUIRED = 411;
    public static readonly METHOD_NOT_ALLOWED = 405;
    public static readonly MOVED_PERMANENTLY = 301;
    public static readonly MOVED_TEMPORARILY = 302;
    public static readonly MULTIPLE_CHOICES = 300;
    public static readonly NO_CONTENT = 204;
    public static readonly NON_AUTHORITATIVE_INFORMATION = 203;
    public static readonly NOT_ACCEPTABLE = 406;
    public static readonly NOT_FOUND = 404;
    public static readonly NOT_IMPLEMENTED = 501;
    public static readonly NOT_MODIFIED = 304;
    public static readonly OK = 200;
    public static readonly PARTIAL_CONTENT = 206;
    public static readonly PAYMENT_REQUIRED = 402;
    public static readonly PRECONDITION_FAILED = 412;
    public static readonly PROXY_AUTHENTICATION_REQUIRED = 407;
    public static readonly REQUEST_ENTITY_TOO_LARGE = 413;
    public static readonly REQUEST_TIMEOUT = 408;
    public static readonly REQUEST_URI_TOO_LONG = 414;
    public static readonly REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    public static readonly RESET_CONTENT = 205;
    public static readonly SEE_OTHER = 303;
    public static readonly SERVICE_UNAVAILABLE = 503;
    public static readonly SWITCHING_PROTOCOLS = 101;
    public static readonly TEMPORARY_REDIRECT = 307;
    public static readonly UNAUTHORIZED = 401;
    public static readonly UNSUPPORTED_MEDIA_TYPE = 415;
    public static readonly USE_PROXY = 305;
    public static readonly UNPROCESSABLE_CONTENT = 422;

    /**
     * Mapping between HTTP response codes (string) and their corresponding reason-phrases
     * as defined in RFC 7231, section 6.1.
     */
    public static readonly HttpCodesReasons = {
        "100": "Continue", "101": "Switching Protocols", "200": "OK", "201": "Created",
        "202": "Accepted", "203": "Non-Authoritative Information", "204": "No Content",
        "205": "Reset Content", "206": "Partial Content", "300": "Multiple Choices",
        "301": "Moved Permanently", "302": "Found", "303": "See Other", "304": "Not Modified",
        "305": "Use Proxy", "307": "Temporary Redirect", "400": "Bad Request",
        "401": "Unauthorized", "402": "Payment Required", "403": "Forbidden",
        "404": "Not Found", "405": "Method Not Allowed", "406": "Not Acceptable",
        "407": "Proxy Authentication Required", "408": "Request Timeout", "409": "Conflict",
        "410": "Gone", "411": "Length Required", "412": "Precondition Failed",
        "413": "Payload Too Large", "414": "URI Too Large", "415": "Unsupported Media Type",
        "416": "Range Not Satisfiable", "417": "Expectation Failed", "422": "Unprocessable Content",
        "426": "Upgrade Required", "500": "Internal Server Error", "501": "Not Implemented",
        "502": "Bad Gateway", "503": "Service Unavailable", "504": "Gateway Timmeout",
        "505": "HTTP Version Not Supported",

        /**
        * Utility method that accepts an HTTP code and returns its corresponding reason-phrase.
        * @param code The HTTP status code (number).
        * @returns The reason phrase string.
        * @throws Error if the code is not a valid integer in the range [100-505].
        */
        getReason: (code: number): string => {
            if (isNaN(code) || code < 100 || code > 505) {
                throw Error('Illegal argument for code[' + code + ']. Valid HTTP codes are integer numbers in the range [100-505].')
            }
            return Response.HttpCodesReasons[String(code)];
        }
    }

    /**
     * Checks if the response façade is currently valid or connected to an active request context.
     * @returns True if valid, false otherwise.
     */
    public static isValid(): boolean {
        return HttpResponseFacade.isValid();
    }

    /**
     * Serializes a JavaScript object to JSON, sets the `Content-Type: application/json` header,
     * and writes the JSON string to the response output stream.
     * @param obj The JavaScript object to be serialized and sent.
     */
    public static json(obj: any): void {
        Response.addHeader("Content-Type", "application/json")
        const objJson = JSON.stringify(obj);
        Response.print(objJson);
    }

    /**
     * Writes a string of text to the response body using **UTF-8** encoding.
     * Note: This method automatically handles flushing the output stream.
     * @param text The string content to write.
     */
    public static print(text: string): void {
        text = (text && text.toString()) || "";
        const out = Response.getOutputStream().native;
        // Use Java I/O to ensure explicit UTF-8 encoding
        const writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write(text);
        writer.flush();
    }

    /**
     * Writes a string of text followed by a newline character (`\n`) to the response body
     * using **UTF-8** encoding.
     * @param text The string content to write.
     */
    public static println(text: string): void {
        text = (text && text.toString()) || "";
        const out = Response.getOutputStream().native;
        const writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write(text);
        writer.write("\n");
        writer.flush();
    }

    /**
     * Writes an array of bytes directly to the response output stream, typically used for binary data.
     * @param bytes The array of bytes to write.
     */
    public static write(bytes: any[]): void {
        if (!bytes) {
            bytes = [];
        }
        HttpResponseFacade.write(bytes);
    }

    /**
     * Checks if the response headers and status have already been sent to the client.
     * @returns True if the response is committed, false otherwise.
     */
    public static isCommitted(): boolean {
        return HttpResponseFacade.isCommitted();
    }

    /**
     * Sets the value of the `Content-Type` header.
     * @param contentType The MIME type string (e.g., 'text/html', 'application/pdf').
     */
    public static setContentType(contentType: string): void {
        HttpResponseFacade.setContentType(contentType);
    }

    /**
     * Forces any buffered output to be written to the client.
     */
    public static flush(): void {
        HttpResponseFacade.flush();
    }

    /**
     * Closes the response output stream.
     */
    public static close(): void {
        HttpResponseFacade.close();
    }

    /**
     * Adds a cookie to the response. The cookie object is serialized to JSON before being passed
     * to the underlying Java facade.
     * @param cookie The cookie definition object.
     */
    public static addCookie(cookie: Cookie): void {
        HttpResponseFacade.addCookie(JSON.stringify(cookie));
    }

    /**
     * Checks if a response header with the specified name has already been set.
     * @param name The name of the header.
     * @returns True if the header exists, false otherwise.
     */
    public static containsHeader(name: string): boolean {
        return HttpResponseFacade.containsHeader(name);
    }

    /**
     * Encodes a URL for use in redirects or forms, including session information if necessary.
     * @param url The URL to encode.
     * @returns The encoded URL string.
     */
    public static encodeURL(url: string): string {
        return HttpResponseFacade.encodeURL(url);
    }

    /**
     * Gets the character encoding used for the response body.
     * @returns The character encoding string.
     */
    public static getCharacterEncoding(): string {
        return HttpResponseFacade.getCharacterEncoding();
    }

    /**
     * Encodes a URL for use in the `Location` header of a redirect response.
     * @param url The redirect URL to encode.
     * @returns The encoded redirect URL string.
     */
    public static encodeRedirectURL(url: string): string {
        return HttpResponseFacade.encodeRedirectURL(url);
    }

    /**
     * Gets the current `Content-Type` header value.
     * @returns The content type string.
     */
    public static getContentType(): string {
        return HttpResponseFacade.getContentType();
    }

    /**
     * Sends an HTTP error response to the client with the specified status code and optional message.
     * This bypasses the normal response body writing process.
     * @param status The HTTP status code (e.g., 404, 500).
     * @param message An optional message to include in the error response.
     */
    public static sendError(status: number, message?: string): void {
        HttpResponseFacade.sendError(status, message);
    }

    /**
     * Sets the character encoding to be used for the response body (e.g., 'UTF-8').
     * @param charset The character set string.
     */
    public static setCharacterEncoding(charset: string): void {
        HttpResponseFacade.setCharacterEncoding(charset);
    }

    /**
     * Sends a redirect response (status code 302 by default) to the client.
     * @param location The new URL to redirect the client to.
     */
    public static sendRedirect(location: string): void {
        HttpResponseFacade.sendRedirect(location);
    }

    /**
     * Sets the `Content-Length` header for the response.
     * @param length The size of the response body in bytes.
     */
    public static setContentLength(length: number): void {
        HttpResponseFacade.setContentLength(length);
    }

    /**
     * Sets a response header with the given name and value. If the header already exists, its value is overwritten.
     * @param name The name of the header.
     * @param value The value of the header.
     */
    public static setHeader(name: string, value: string): void {
        HttpResponseFacade.setHeader(name, value);
    }

    /**
     * Adds a response header with the given name and value. If the header already exists, a second header with the same name is added.
     * @param name The name of the header.
     * @param value The value of the header.
     */
    public static addHeader(name: string, value: string): void {
        HttpResponseFacade.addHeader(name, value);
    }

    /**
     * Sets the HTTP status code for the response.
     * @param status The integer status code (e.g., 200, 404).
     */
    public static setStatus(status: number): void {
        HttpResponseFacade.setStatus(status);
    }

    /**
     * Clears all buffers, status code, and headers from the response, allowing a new response to be generated.
     * This is only possible if the response has not yet been committed.
     */
    public static reset(): void {
        HttpResponseFacade.reset();
    }

    /**
     * Gets the value of a specific header. If multiple headers with the same name exist, it returns the first one.
     * @param name The name of the header.
     * @returns The header value string.
     */
    public static getHeader(name: string): string {
        return HttpResponseFacade.getHeader(name);
    }

    /**
     * Sets the locale for the response, which may affect language and date/time formatting.
     * @param language The language code (e.g., 'en', 'fr').
     * @param country The optional country code (e.g., 'US', 'GB').
     * @param variant The optional variant code.
     */
    public static setLocale(language: string, country?: string, variant?: string): void {
        return HttpResponseFacade.setLocale(language, country, variant);
    }

    /**
     * Gets all header values for a specific header name as an array of strings.
     * @param name The name of the header.
     * @returns An array of header values.
     */
    public static getHeaders(name: string): string[] {
        return JSON.parse(HttpResponseFacade.getHeaders(name));
    }

    /**
     * Gets the names of all headers that have been set on the response.
     * @returns An array of header names.
     */
    public static getHeaderNames(): string[] {
        return JSON.parse(HttpResponseFacade.getHeaderNames());
    }

    /**
     * Gets the currently set locale string for the response.
     * @returns The locale string.
     */
    public static getLocale(): string {
        return HttpResponseFacade.getLocale();
    }

    /**
     * Gets the underlying output stream object, wrapped in the SDK's `OutputStream` class.
     * This is useful for writing raw or large amounts of data.
     * @returns The output stream object.
     */
    public static getOutputStream(): OutputStream {
        return new OutputStream(HttpResponseFacade.getOutputStream());
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Response;
}
