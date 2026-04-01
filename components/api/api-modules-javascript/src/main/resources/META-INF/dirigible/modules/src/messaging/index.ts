/**
 * @module messaging/index
 * @package @aerokit/sdk/messaging
 * @overview
 * 
 * This module provides functionalities for messaging within the Aerokit SDK. It includes classes and methods for producing and consuming messages from various messaging systems, as well as managing messaging configurations and connections.
 * 
 * The main components of this module are:
 * - Consumer: Represents a message consumer that can subscribe to topics and consume messages.
 * - Producer: Represents a message producer that can send messages to topics.
 * - Decorators: Provides decorators for defining message handlers and producers in a modular architecture.
 */

export { Consumer as consumer } from "./consumer";
export * from "./consumer";
export { Producer as producer } from "./producer";
export * from "./producer";
export * as decorators from "./decorators";
export * from "./decorators";