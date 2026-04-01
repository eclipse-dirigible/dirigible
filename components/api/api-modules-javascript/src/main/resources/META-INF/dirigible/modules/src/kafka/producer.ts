/**
 * @module kafka/producer
 * @package @aerokit/sdk/kafka
 * @name Producer
 * @overview
 * 
 * The Producer module provides an API for configuring and managing Kafka producers, allowing scripts to create topics, send messages, and close the producer connection. It abstracts the complexities of Kafka producer configuration and message sending, enabling developers to easily integrate Kafka message production into their applications.
 * 
 * ### Key Features:
 * - **Topic Configuration**: The `Producer.topic` method allows developers to create a topic configuration wrapper with specified destination and properties.
 * - **Message Sending**: The `send` method on the Topic class enables sending messages with optional keys to the configured Kafka topic.
 * - **Connection Management**: The `close` method allows developers to close the Kafka producer connection pool, ensuring proper resource cleanup.
 * 
 * ### Use Cases:
 * - **Event Production**: Developers can use this module to produce messages to Kafka topics in real-time, enabling use cases such as event-driven architectures and microservices communication.
 * - **Integration with Other Systems**: By sending Kafka messages, applications can integrate with other systems that consume messages from Kafka, facilitating data flow across different components of an ecosystem.
 * 
 * ### Example Usage:
 * ```ts
 * import { Producer } from "@aerokit/sdk/kafka";
 * 
 * // Create a producer for the "orders" topic with specific configuration
 * const ordersProducer = Producer.topic("orders", { "bootstrap.servers": "localhost:9092" });
 * 
 * // Send a message with a key to the "orders" topic
 * ordersProducer.send("order123", "New order placed");
 * 
 * // Later, close the producer connection
 * Producer.close({ "bootstrap.servers": "localhost:9092" });
 * ```
 */

const KafkaFacade = Java.type("org.eclipse.dirigible.components.api.kafka.KafkaFacade");

/**
 * The Producer class serves as the main entry point for creating and configuring
 * Kafka topic producers.
 */
export class Producer {

    /**
     * Creates a new topic configuration wrapper that can be used to send messages
     * to a specific Kafka topic.
     *
     * @param destination The name of the Kafka topic to send messages to.
     * @param configuration Optional key-value object containing Kafka producer properties
     * (e.g., 'bootstrap.servers', 'acks').
     * @returns A {@link Topic} instance configured for the specified destination and properties.
     */
    public static topic(destination: string, configuration: { [key: string]: string } = {}): Topic {
        return new Topic(destination, configuration);
    }

    /**
     * Closes the Kafka producer connection pool, releasing associated resources.
     * This should be called when message sending is complete to ensure proper cleanup.
     *
     * @param configuration Optional key-value object containing the configuration
     * used to initialize the producer to be closed.
     */
    public static close(configuration: { [key: string]: string } = {}): void {
        KafkaFacade.closeProducer(configuration);
    }
}

/**
 * Represents a configured Kafka topic that can be used to send messages.
 */
class Topic {

    private destination: string;
    private configuration: { [key: string]: string };

    /**
     * @param destination The name of the Kafka topic.
     * @param configuration Key-value object for producer properties.
     */
    constructor(destination: string, configuration: { [key: string]: string }) {
        this.destination = destination;
        this.configuration = configuration;
    }

    /**
     * Sends a message with an optional key to the configured Kafka topic.
     *
     * @param key The key of the message. Messages with the same key go to the same partition.
     * @param value The content of the message to be sent.
     */
    public send(key: string, value: string): void {
        KafkaFacade.send(this.destination, key, value, JSON.stringify(this.configuration));
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Producer;
}