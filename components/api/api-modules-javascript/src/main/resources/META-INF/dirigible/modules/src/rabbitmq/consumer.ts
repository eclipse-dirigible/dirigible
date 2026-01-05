/**
 * RabbitMQ Consumer
 *
 * This facade provides static methods to start and stop listening on RabbitMQ queues,
 * wrapping the underlying Java implementation provided by the `RabbitMQFacade`.
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