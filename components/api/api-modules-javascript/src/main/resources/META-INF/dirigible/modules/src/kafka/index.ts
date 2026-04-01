/**
 * @module kafka/index
 * @package @aerokit/sdk/kafka
 * @overview
 * 
 * This module provides functionalities for Kafka integration within the Aerokit SDK. It includes classes and methods for producing and consuming messages from Kafka topics, as well as managing Kafka configurations and connections.
 * 
 * The main components of this module are:
 * - Consumer: Represents a Kafka consumer that can subscribe to topics and consume messages.
 * - Producer: Represents a Kafka producer that can send messages to topics.
 */

export { Consumer as consumer } from "./consumer";
export * from "./consumer";
export { Producer as producer } from "./producer";
export * from "./producer";