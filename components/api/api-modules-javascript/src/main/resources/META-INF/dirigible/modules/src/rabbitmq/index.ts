/**
 * @module rabbitmq/index
 * @package @aerokit/sdk/rabbitmq
 * @overview
 *
 * This module provides RabbitMQ messaging capabilities for the Aerokit SDK.
 * It includes consumer and producer abstractions to connect to AMQP brokers,
 * send and receive messages, and manage queues/exchanges.
 *
 * The main components of this module are:
 * - Consumer: Receives messages from queues and handles message processing.
 * - Producer: Publishes messages to exchanges or queues.
 */

export * from "./consumer";
export { Consumer as consumer } from "./consumer";
export * from "./producer";
export { Producer as producer } from "./producer";
