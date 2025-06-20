const SpringBeanProvider = Java.type("org.eclipse.dirigible.components.spring.SpringBeanProvider");
const Invoker = Java.type('org.eclipse.dirigible.components.engine.camel.invoke.Invoker');
const invoker = SpringBeanProvider.getBean(Invoker.class);
const CamelMessage = Java.type('org.apache.camel.Message');

export interface HeadersMap {
    [key: string]: string | string[];
}

export interface ExchangeProperties {
    [key: string]: string | string[];
}

export interface IntegrationMessage {

    constructor(message: any);

    getBody(): any;

    getExchangeProperty(propertyName: string): any;

    setExchangeProperty(propertyName: string, propertyValue: any): void;

    getExchangeProperties(): Record<string, any>;

    getBodyAsString(): string;

    setBody(body: any): void;

    getHeaders(): HeadersMap;

    getHeader(key: string): string | string[];

    setHeaders(headers: HeadersMap): void;

    setHeader(key: string, value: string | string[]): void;

    getCamelMessage(): typeof CamelMessage;
}

export class Integrations {

    public static invokeRoute(routeId: string, payload: any, headers: HeadersMap, exchangeProperties: ExchangeProperties) {
        return invoker.invokeRoute(routeId, payload, headers, exchangeProperties);
    }

    public static getInvokingRouteMessage(): IntegrationMessage {
        return __context.camelMessage;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Integrations;
}
