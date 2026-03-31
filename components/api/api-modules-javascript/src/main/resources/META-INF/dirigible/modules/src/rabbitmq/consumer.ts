/**
 * @module rabbitmq/consumer
 * @package @aerokit/sdk/rabbitmq
 * @name Consumer
 * @overview
 * 
 * The RabbitMQ Consumer module provides a facade for managing message listeners on RabbitMQ queues. It allows developers to start and stop listening for messages on specified queues, delegating the handling of incoming messages to designated components or scripts. This module abstracts the underlying Java implementation, providing a simple interface for integrating RabbitMQ message consumption into applications built on the platform.
 * 
 * ### Key Features:
 * - **Start Listening**: The `startListening` method enables the application to begin consuming messages from a specified RabbitMQ queue, with a designated handler for processing incoming messages.
 * - **Stop Listening**: The `stopListening` method allows the application to cease consuming messages from a specified queue for a given handler, providing control over message consumption.
 * 
 * ### Use Cases:
 * - **Message-Driven Architecture**: This module is ideal for applications that follow a message-driven architecture, allowing them to react to events or data changes by consuming messages from RabbitMQ queues.
 * - **Integration with External Systems**: Developers can use this module to integrate their applications with external systems that communicate via RabbitMQ, enabling seamless data exchange and event handling.
 * 
 * ### Example Usage:
 * ```ts
 * import { Consumer } from "@aerokit/sdk/rabbitmq";
 * 
 * // Start listening on a RabbitMQ queue
 * Consumer.startListening("myQueue", "myHandler");
 * 
 * // Stop listening on the RabbitMQ queue
 * Consumer.stopListening("myQueue", "myHandler");
 * ```
 */

const RabbitMQFacade = Java.type("org.eclipse.dirigible.components.api.rabbitmq.RabbitMQFacade");

export class Consumer {

	/**
	 * Starts listening for messages on a specified RabbitMQ queue.
	 * The handler is typically a service or script URI that will be executed
	 * when a message arrives.
	 *
	 * @param queue The name of the RabbitMQ queue to listen to.
	 * @param handler The URI/name of the component/script that will handle the message.
	 */
	public static startListening(queue: string, handler: string): void {
		RabbitMQFacade.startListening(queue, handler);
	}

	/**
	 * Stops the message listener previously started on a specified RabbitMQ queue
	 * for a given handler.
	 *
	 * @param queue The name of the RabbitMQ queue.
	 * @param handler The URI/name of the component/script whose listener should be stopped.
	 */
	public static stopListening(queue: string, handler: string): void {
		RabbitMQFacade.stopListening(queue, handler);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Consumer;
}