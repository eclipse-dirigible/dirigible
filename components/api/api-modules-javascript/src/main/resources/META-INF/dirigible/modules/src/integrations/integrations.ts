/**
 * Provides a static façade for interacting with Apache Camel routes
 * within the execution environment. This allows JavaScript code to synchronously
 * invoke integration routes and access the current message context.
 */

const SpringBeanProvider = Java.type("org.eclipse.dirigible.components.spring.SpringBeanProvider");
const Invoker = Java.type('org.eclipse.dirigible.components.engine.camel.invoke.Invoker');
const invoker = SpringBeanProvider.getBean(Invoker.class);
const CamelMessage = Java.type('org.apache.camel.Message');

/**
 * Defines the structure for Camel Message Headers, typically used for
 * standard communication metadata. Keys are strings, and values can be
 * a single string or an array of strings.
 */
export interface HeadersMap {
    [key: string]: string | string[];
}

/**
 * Defines the structure for Camel Exchange Properties, which are context-specific
 * variables used for processing within a single route execution. Keys are strings,
 * and values can be a single string or an array of strings.
 */
export interface ExchangeProperties {
    [key: string]: string | string[];
}

/**
 * Represents the Camel message currently being processed. This interface mirrors
 * key functionality of the underlying Apache Camel Message and Exchange objects.
 */
export interface IntegrationMessage {

    /**
     * Constructs an IntegrationMessage wrapper around a native message object.
     * @param message The native message object (usually a Camel Message or Exchange).
     */
    constructor(message: any);

    /**
     * Retrieves the body of the message.
     * @returns The message body (can be any type, e.g., string, object, stream).
     */
    getBody(): any;

    /**
     * Retrieves a specific property from the current Exchange context.
     * @param propertyName The name of the exchange property.
     * @returns The value of the exchange property.
     */
    getExchangeProperty(propertyName: string): any;

    /**
     * Sets a specific property on the current Exchange context.
     * @param propertyName The name of the exchange property.
     * @param propertyValue The value to set.
     */
    setExchangeProperty(propertyName: string, propertyValue: any): void;

    /**
     * Retrieves all properties from the current Exchange context.
     * @returns A map of all exchange properties.
     */
    getExchangeProperties(): Record<string, any>;

    /**
     * Retrieves the body of the message as a string.
     * @returns The message body converted to a string.
     */
    getBodyAsString(): string;

    /**
     * Sets the body of the message.
     * @param body The new body content.
     */
    setBody(body: any): void;

    /**
     * Retrieves all headers associated with the message.
     * @returns A map of headers.
     */
    getHeaders(): HeadersMap;

    /**
     * Retrieves a specific header value.
     * @param key The header key.
     * @returns The header value(s).
     */
    getHeader(key: string): string | string[];

    /**
     * Sets multiple headers on the message.
     * @param headers The map of headers to set.
     */
    setHeaders(headers: HeadersMap): void;

    /**
     * Sets a single header on the message.
     * @param key The header key.
     * @param value The header value.
     */
    setHeader(key: string, value: string | string[]): void;

    /**
     * Retrieves the underlying native Camel Message object.
     * @returns The raw Camel Message instance.
     */
    getCamelMessage(): typeof CamelMessage;
}

/**
 * The Integrations class provides utility methods for triggering and interacting
 * with predefined Apache Camel integration routes.
 */
export class Integrations {

    /**
     * Synchronously invokes a specified Camel route.
     *
     * @param routeId The unique identifier of the Camel route to be executed.
     * @param payload The initial message body/payload for the route.
     * @param headers A map of headers to set on the initial Camel Message.
     * @param exchangeProperties A map of properties to set on the Camel Exchange context.
     * @returns The final result (the body of the resulting Camel Message) after the route has completed execution.
     */
    public static invokeRoute(routeId: string, payload: any, headers: HeadersMap, exchangeProperties: ExchangeProperties) {
        return invoker.invokeRoute(routeId, payload, headers, exchangeProperties);
    }

    /**
     * Retrieves the current message being processed by the underlying integration
     * engine's context. This is typically used within a route endpoint (e.g., a script component)
     * to access or modify the message.
     *
     * Note: '__context' is assumed to be a global or context-injected variable.
     * @returns The current IntegrationMessage wrapper.
     */
    public static getInvokingRouteMessage(): IntegrationMessage {
        // @ts-ignore: __context is an implicit global provided by the environment
        return __context.camelMessage;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Integrations;
}