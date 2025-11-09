/**
 * RabbitMQ Producer
 *
 * This facade provides a static method to send messages to a RabbitMQ queue,
 * wrapping the underlying Java implementation provided by the `RabbitMQFacade`.
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