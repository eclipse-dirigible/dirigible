/**
 * Provides an API for consuming messages from JMS-style destinations,
 * supporting both Queues (point-to-point) and Topics (publish/subscribe).
 */

const MessagingFacade = Java.type("org.eclipse.dirigible.components.api.messaging.MessagingFacade");

/**
 * The entry point for creating messaging consumers.
 * Use this class to obtain instances of Queue or Topic consumers.
 */
export class Consumer {

    /**
     * Creates a Queue consumer instance for point-to-point messaging.
     * Messages sent to this destination are consumed by only one receiver.
     *
     * @param destination The name of the queue destination (e.g., 'orders.queue').
     * @returns A {@link Queue} instance.
     */
    public static queue(destination: string): Queue {
        return new Queue(destination);
    }

    /**
     * Creates a Topic consumer instance for publish/subscribe messaging.
     * Messages sent to this destination can be consumed by multiple subscribers.
     *
     * @param destination The name of the topic destination (e.g., 'market.updates.topic').
     * @returns A {@link Topic} instance.
     */
    public static topic(destination: string): Topic {
        return new Topic(destination);
    }
}

/**
 * Represents a consumer for a Queue destination (point-to-point).
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
     * Attempts to synchronously receive a message from the queue.
     *
     * @param timeout The maximum time (in milliseconds) to wait for a message. Defaults to 1000ms.
     * @returns The received message content (usually a string or object), or null if the timeout is reached.
     */
    public receive(timeout: number = 1000) {
        return MessagingFacade.receiveFromQueue(this.destination, timeout);
    };
}

/**
 * Represents a consumer for a Topic destination (publish/subscribe).
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
     * Attempts to synchronously receive a message from the topic.
     *
     * @param timeout The maximum time (in milliseconds) to wait for a message. Defaults to 1000ms.
     * @returns The received message content (usually a string or object), or null if the timeout is reached.
     */
    public receive(timeout: number = 1000) {
        return MessagingFacade.receiveFromTopic(this.destination, timeout);
    }
}


// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = Consumer;
}