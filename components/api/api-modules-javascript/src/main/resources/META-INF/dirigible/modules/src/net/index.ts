/**
 * @module net/index
 * @package @aerokit/sdk/net
 * @overview
 * 
 * This module provides functionalities for network communication within the Aerokit SDK. It includes classes and methods for handling various network protocols, such as HTTP, SOAP, WebSockets, and more. The module also provides decorators for defining network-related functionalities in a modular architecture.
 * 
 * The main components of this module are:
 * - HTTP: Represents an HTTP client for making HTTP requests and handling responses.
 * - SOAP: Represents a SOAP client for interacting with SOAP-based web services.
 * - WebSockets: Represents a WebSocket client for establishing and managing WebSocket connections.
 * - Decorators: Provides decorators for defining network-related functionalities in a declarative manner.
 */

export * from "./soap";
export { SOAP as soap } from "./soap";
export * from "./websockets";
export { Websockets as websockets } from "./websockets";
export * as decorators from "./decorators";
export * from "./decorators";