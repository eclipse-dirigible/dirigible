/**
 * @module rabbitmq/producer
 * @package @aerokit/sdk/rabbitmq
 * @name Producer
 * @overview
 * 
 * The RabbitMQ Producer module provides a simple facade for sending messages to RabbitMQ queues. It abstracts the underlying Java implementation, allowing developers to easily integrate message sending capabilities into their applications built on the platform. This module is designed to facilitate communication with RabbitMQ by providing a straightforward interface for sending messages to specified queues.
 * 
 * ### Key Features:
 * - **Message Sending**: The `send` method allows developers to send messages to a specified RabbitMQ queue with ease.
 * 
 * ### Use Cases:
 * - **Event-Driven Architecture**: This module is ideal for applications that follow an event-driven architecture, enabling them to publish events or data changes to RabbitMQ queues for consumption by other services or components.
 * - **Integration with External Systems**: Developers can use this module to integrate their applications with external systems that communicate via RabbitMQ, facilitating seamless data exchange and event handling.
 * 
 * ### Example Usage:
 * ```ts
 * import { Producer } from "@aerokit/sdk/rabbitmq";
 * 
 * // Send a message to a RabbitMQ queue
 * Producer.send("myQueue", "Hello, RabbitMQ!");
 * ```
 */

const RabbitMQFacade = Java.type("org.eclipse.dirigible.components.api.rabbitmq.RabbitMQFacade");

export class Producer {

    /**
     * Sends a message to the specified RabbitMQ queue.
     *
     * @param queue The name of the RabbitMQ queue to send the message to.
     * @param message The content of the message to be sent (as a string).
     */
    public static send(queue: string, message: string): void {
        RabbitMQFacade.send(queue, message);
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = Producer;
}