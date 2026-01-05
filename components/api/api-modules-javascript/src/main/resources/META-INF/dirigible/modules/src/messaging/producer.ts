/**
 * Provides an API for producing (sending) messages to JMS-style destinations,
 * supporting both Queues (point-to-point) and Topics (publish/subscribe).
 */

const MessagingFacade = Java.type("org.eclipse.dirigible.components.api.messaging.MessagingFacade");

/**
 * The entry point for creating messaging producers.
 * Use this class to obtain instances of Queue or Topic producers for sending messages.
 */
export class Producer {

	/**
	 * Creates a Queue producer instance for point-to-point messaging.
	 * Messages sent to this destination are intended to be consumed by a single receiver.
	 *
	 * @param destination The name of the queue destination (e.g., 'task.queue').
	 * @returns A {@link Queue} instance.
	 */
	public static queue(destination: string): Queue {
		return new Queue(destination);
	}

	/**
	 * Creates a Topic producer instance for publish/subscribe messaging.
	 * Messages sent to this destination can be consumed by multiple subscribers simultaneously.
	 *
	 * @param destination The name of the topic destination (e.g., 'sensor.data.topic').
	 * @returns A {@link Topic} instance.
	 */
	public static topic(destination: string): Topic {
		return new Topic(destination);
	}
}

/**
 * Represents a producer for a Queue destination (point-to-point).
 */
class Queue {

	private destination: string;

	/**
	 * @param destination The name of the queue destination.
	 */
	constructor(destination: string) {
		this.destination = destination;
	}

	/**
	 * Sends a message to the configured queue destination.
	 *
	 * @param message The content of the message to send (typically a string or serialized object).
	 */
	public send(message: string): void {
		MessagingFacade.sendToQueue(this.destination, message);
	}
}

/**
 * Represents a producer for a Topic destination (publish/subscribe).
 */
class Topic {

	private destination: string;

	/**
	 * @param destination The name of the topic destination.
	 */
	constructor(destination: string) {
		this.destination = destination;
	}

	/**
	 * Sends a message to the configured topic destination. All active subscribers will receive the message.
	 *
	 * @param message The content of the message to publish (typically a string or serialized object).
	 */
	public send(message: string): void {
		MessagingFacade.sendToTopic(this.destination, message);
	}
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Producer;
}