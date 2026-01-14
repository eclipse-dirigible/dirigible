/**
 * The Request API under the HTTP module is responsible for
 * managing standard HTTP request parameters, headers, cookies,
 * and request metadata provided to server-side scripting services.
 *
 */

import { InputStream, Streams } from "../io/streams";
import { Cookie } from "./response";

const HttpRequestFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpRequestFacade");

/**
 * Represents the HTTP Request object available within a service execution
 * context. It provides access to HTTP metadata, query parameters, request
 * body content, cookies, and security information.
 *
 * All functions in this class are static: no instance of `Request`
 * needs to be created.
 */
export class Request {

    private static TEXT_DATA: string;

    /**
     * Determines whether the current thread is handling a valid HTTP request.
     *
     * @returns `true` if called in a valid HTTP request context, otherwise `false`.
     */
    public static isValid(): boolean {
        return HttpRequestFacade.isValid();
    }

    /** Returns the HTTP method (GET, POST, PUT, DELETE, etc.). */
    public static getMethod(): string {
        return HttpRequestFacade.getMethod();
    }

    /** Returns the authenticated remote user name if available. */
    public static getRemoteUser(): string {
        return HttpRequestFacade.getRemoteUser();
    }

    /** Returns the portion of the request path following the servlet path. */
    public static getPathInfo(): string {
        return HttpRequestFacade.getPathInfo();
    }

    /** Returns the translated file system path for the request. */
    public static getPathTranslated(): string {
        return HttpRequestFacade.getPathTranslated();
    }

    /**
     * Returns the value of a specific HTTP header.
     *
     * @param name - Header name to retrieve.
     * @returns The header value or `undefined` if not found.
     */
    public static getHeader(name: string): string {
        const header = HttpRequestFacade.getHeader(name);
        return header ? header : undefined;
    }

    /**
     * Checks whether the remote user has the given role.
     *
     * @param role - The role name to check.
     */
    public static isUserInRole(role: string): boolean {
        return HttpRequestFacade.isUserInRole(role);
    }

    /**
     * Returns a request attribute value previously associated with the request.
     *
     * @param name - The attribute name.
     * @returns A string value or `undefined`.
     */
    public static getAttribute(name: string): string | undefined {
        const attribute = HttpRequestFacade.getAttribute(name);
        return attribute ? attribute : undefined;
    }

    /** Returns the authentication type if known (BASIC, CLIENT_CERT, etc.). */
    public static getAuthType(): string {
        return HttpRequestFacade.getAuthType();
    }

    /**
     * Returns all cookies sent with the request.
     *
     * @returns An array of Cookie objects.
     */
    public static getCookies(): Cookie[] {
        return JSON.parse(HttpRequestFacade.getCookies());
    }

    /** Returns all available request attribute names. */
    public static getAttributeNames(): string[] {
        return JSON.parse(HttpRequestFacade.getAttributeNames());
    }

    /** Returns the character encoding used in the request body. */
    public static getCharacterEncoding(): string {
        return HttpRequestFacade.getCharacterEncoding();
    }

    /** Returns the size of the request body in bytes, if known. */
    public static getContentLength(): number {
        return HttpRequestFacade.getContentLength();
    }

    /**
     * Returns all values of a specific header.
     *
     * @param name - Header name to retrieve.
     */
    public static getHeaders(name: string): string[] {
        return JSON.parse(HttpRequestFacade.getHeaders(name));
    }

    /** Returns the MIME content type of the request body. */
    public static getContentType(): string {
        return HttpRequestFacade.getContentType();
    }

    /** Returns the raw request body as a byte array. */
    public static getBytes(): any[] {
        return JSON.parse(HttpRequestFacade.getBytes());
    }

    /**
     * Returns the request body as text. This is computed once and cached.
     */
    public static getText() {
        if (!Request.TEXT_DATA) {
            Request.TEXT_DATA = HttpRequestFacade.getText();
        }
        return Request.TEXT_DATA;
    }

    /**
     * Returns the request body parsed as JSON if valid.
     *
     * @returns A JSON object or `undefined` if parsing fails.
     */
    public static json(): { [key: string]: any } | undefined {
        return Request.getJSON();
    }

    /** Same as json(); explicit form. */
    public static getJSON(): { [key: string]: any } | undefined {
        try {
            return JSON.parse(Request.getText());
        } catch (e) {
            return undefined;
        }
    }

    /** Returns a request parameter value. */
    public static getParameter(name: string): string {
        return HttpRequestFacade.getParameter(name);
    }

    /** Returns a map of request parameters to arrays of values. */
    public static getParameters(): { [key: string]: string[] } {
        return JSON.parse(HttpRequestFacade.getParameters());
    }

    /** Returns the allocated request resource path. */
    public static getResourcePath(): string {
        return HttpRequestFacade.getResourcePath();
    }

    /** Returns all header names. */
    public static getHeaderNames(): string[] {
        return JSON.parse(HttpRequestFacade.getHeaderNames());
    }

    /** Returns all parameter names. */
    public static getParameterNames(): string[] {
        return JSON.parse(HttpRequestFacade.getParameterNames());
    }

    /** Returns all values for a given parameter name. */
    public static getParameterValues(name: string): string[] {
        return JSON.parse(HttpRequestFacade.getParameterValues(name));
    }

    /** Returns the HTTP protocol version. */
    public static getProtocol(): string {
        return HttpRequestFacade.getProtocol();
    }

    /** Returns the transport scheme (e.g., http, https). */
    public static getScheme(): string {
        return HttpRequestFacade.getScheme();
    }

    /** Returns the context path of the request. */
    public static getContextPath(): string {
        return HttpRequestFacade.getContextPath();
    }

    /** Returns the server host name. */
    public static getServerName(): string {
        return HttpRequestFacade.getServerName();
    }

    /** Returns the server port number. */
    public static getServerPort(): number {
        return HttpRequestFacade.getServerPort();
    }

    /** Returns the full raw query string. */
    public static getQueryString(): string {
        return HttpRequestFacade.getQueryString();
    }

    /**
     * Parses the query string and returns a map of parameter keys to values.
     * If the same key appears multiple times, values are collected into arrays.
     */
    public static getQueryParametersMap(): { [key: string]: string | string[] } {
        let queryString = Request.getQueryString();
        if (!queryString)
            return {}

        queryString = decodeURI(queryString);
        let queryStringSegments = queryString.split('&');

        let queryMap: { [key: string]: any } = {};
        queryStringSegments.forEach(function (seg) {
            seg = seg.replace('amp;', '');
            const kv = seg.split('=');
            const key = kv[0].trim();
            const value = kv[1] === undefined ? true : kv[1].trim();
            if (queryMap[key] !== undefined) {
                if (!Array.isArray(queryMap[key]))
                    queryMap[key] = [queryMap[key]];
                queryMap[key].push(value);
            } else {
                queryMap[key] = value;
            }
        });
        return queryMap;
    }

    /** Returns the remote client IP address. */
    public static getRemoteAddress(): string {
        return HttpRequestFacade.getRemoteAddress();
    }

    /** Returns the remote client host name. */
    public static getRemoteHost(): string {
        return HttpRequestFacade.getRemoteHost();
    }

    /** Assigns a new attribute to the request. */
    public static setAttribute(name: string, value: string): void {
        HttpRequestFacade.setAttribute(name, value);
    }

    /** Removes an attribute from the request. */
    public static removeAttribute(name: string): void {
        HttpRequestFacade.removeAttribute(name);
    }

    /** Returns the client locale preferences. */
    public static getLocale(): any {
        return JSON.parse(HttpRequestFacade.getLocale());
    }

    /** Returns the full request URI. */
    public static getRequestURI(): string {
        return HttpRequestFacade.getRequestURI();
    }

    /** Returns `true` if the request was made over HTTPS. */
    public static isSecure(): boolean {
        return HttpRequestFacade.isSecure();
    }

    /** Returns the full request URL including protocol and host. */
    public static getRequestURL(): string {
        return HttpRequestFacade.getRequestURL();
    }

    /** Returns the internal service path for routing. */
    public static getServicePath(): string {
        return HttpRequestFacade.getServicePath();
    }

    /** Returns the remote client port number. */
    public static getRemotePort(): number {
        return HttpRequestFacade.getRemotePort();
    }

    /** Returns the local network host name. */
    public static getLocalName(): string {
        return HttpRequestFacade.getLocalName();
    }

    /** Returns the local IP address. */
    public static getLocalAddress(): string {
        return HttpRequestFacade.getLocalAddress();
    }

    /** Returns the server local port number handling the request. */
    public static getLocalPort(): number {
        return HttpRequestFacade.getLocalPort();
    }

    /**
     * Returns the request body as a binary input stream.
     *
     * Useful for processing binary uploads.
     */
    public static getInputStream(): InputStream {
        return Streams.createInputStream(HttpRequestFacade.getInputStream());
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Request;
}
