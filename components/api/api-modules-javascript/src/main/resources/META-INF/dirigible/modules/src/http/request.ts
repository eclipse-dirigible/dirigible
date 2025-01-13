import { InputStream, Streams } from "sdk/io/streams";
import { Cookie } from "./response";

const HttpRequestFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpRequestFacade");

export class Request {

    private static TEXT_DATA: string;

    public static isValid(): boolean {
        return HttpRequestFacade.isValid();
    }

    public static getMethod(): string {
        return HttpRequestFacade.getMethod();
    }

    public static getRemoteUser(): string {
        return HttpRequestFacade.getRemoteUser();
    }

    public static getPathInfo(): string {
        return HttpRequestFacade.getPathInfo();
    }

    public static getPathTranslated(): string {
        return HttpRequestFacade.getPathTranslated();
    }

    public static getHeader(name: string): string {
        const header = HttpRequestFacade.getHeader(name)
        return header ? header : undefined;
    }

    public static isUserInRole(role: string): boolean {
        return HttpRequestFacade.isUserInRole(role);
    }

    public static getAttribute(name: string): string | undefined {
        const attribute = HttpRequestFacade.getAttribute(name)
        return attribute ? attribute : undefined;
    }

    public static getAuthType(): string {
        return HttpRequestFacade.getAuthType();
    }

    public static getCookies(): Cookie[] {
        return JSON.parse(HttpRequestFacade.getCookies());
    }

    public static getAttributeNames(): string[] {
        return JSON.parse(HttpRequestFacade.getAttributeNames());
    }

    public static getCharacterEncoding(): string {
        return HttpRequestFacade.getCharacterEncoding();
    }

    public static getContentLength(): number {
        return HttpRequestFacade.getContentLength();
    }

    public static getHeaders(name: string): string[] {
        return JSON.parse(HttpRequestFacade.getHeaders(name));
    }

    public static getContentType(): string {
        return HttpRequestFacade.getContentType();
    }

    public static getBytes(): any[] {
        return JSON.parse(HttpRequestFacade.getBytes());
    }

    public static getText() {
        if (!Request.TEXT_DATA) {
            Request.TEXT_DATA = HttpRequestFacade.getText();
        }
        return Request.TEXT_DATA;
    }

    public static json(): { [key: string]: any } | undefined {
        return Request.getJSON();
    }

    public static getJSON(): { [key: string]: any } | undefined {
        try {
            return JSON.parse(Request.getText());
        } catch (e) {
            return undefined;
        }
    }

    public static getParameter(name: string): string {
        return HttpRequestFacade.getParameter(name);
    }

    public static getParameters(): { [key: string]: string[] } {
        return JSON.parse(HttpRequestFacade.getParameters());
    }

    public static getResourcePath(): string {
        return HttpRequestFacade.getResourcePath();
    }

    public static getHeaderNames(): string[] {
        return JSON.parse(HttpRequestFacade.getHeaderNames());
    }

    public static getParameterNames(): string[] {
        return JSON.parse(HttpRequestFacade.getParameterNames());
    }

    public static getParameterValues(name: string): string[] {
        return JSON.parse(HttpRequestFacade.getParameterValues(name));
    }

    public static getProtocol(): string {
        return HttpRequestFacade.getProtocol();
    }

    public static getScheme(): string {
        return HttpRequestFacade.getScheme();
    }

    public static getContextPath(): string {
        return HttpRequestFacade.getContextPath();
    }

    public static getServerName(): string {
        return HttpRequestFacade.getServerName();
    }

    public static getServerPort(): number {
        return HttpRequestFacade.getServerPort();
    }

    public static getQueryString(): string {
        return HttpRequestFacade.getQueryString();
    }

    /**
     * Returns the query string name value pairs as JS object map. When multiple query parameters with the same name are specified,
     * it will collect theirs values in an array in the order of declaration under that name in the map.
     */
    public static getQueryParametersMap(): { [key: string]: string } {
        let queryString = Request.getQueryString();
        if (!queryString)
            return {}

        queryString = decodeURI(queryString);
        let queryStringSegments = queryString.split('&');

        let queryMap = {}
        queryStringSegments.forEach(function (seg) {
            seg = seg.replace('amp;', '');
            const kv = seg.split('=');
            const key = kv[0].trim();
            const value = kv[1] === undefined ? true : kv[1].trim();
            if (queryMap[key] !== undefined) {
                if (!Array.isArray(queryMap[key]))
                    queryMap[key] = [queryMap[key]];
                else
                    queryMap[key].push(value);
            } else {
                queryMap[key] = value;
            }
        }.bind(this));
        return queryMap;
    }

    public static getRemoteAddress(): string {
        return HttpRequestFacade.getRemoteAddress();
    }

    public static getRemoteHost(): string {
        return HttpRequestFacade.getRemoteHost();
    }

    public static setAttribute(name: string, value: string): void {
        HttpRequestFacade.setAttribute(name, value);
    }

    public static removeAttribute(name: string): void {
        HttpRequestFacade.removeAttribute(name);
    }

    public static getLocale(): any {
        return JSON.parse(HttpRequestFacade.getLocale());
    }

    public static getRequestURI(): string {
        return HttpRequestFacade.getRequestURI();
    }

    public static isSecure(): boolean {
        return HttpRequestFacade.isSecure();
    }

    public static getRequestURL(): string {
        return HttpRequestFacade.getRequestURL();
    }

    public static getServicePath(): string {
        return HttpRequestFacade.getServicePath();
    }

    public static getRemotePort(): number {
        return HttpRequestFacade.getRemotePort();
    }

    public static getLocalName(): string {
        return HttpRequestFacade.getLocalName();
    }

    public static getLocalAddress(): string {
        return HttpRequestFacade.getLocalAddress();
    }

    public static getLocalPort(): number {
        return HttpRequestFacade.getLocalPort();
    }

    public static getInputStream(): InputStream {
        return Streams.createInputStream(HttpRequestFacade.getInputStream());
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Request;
}
