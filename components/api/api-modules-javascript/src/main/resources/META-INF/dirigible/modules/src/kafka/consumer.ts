/**
 * Provides an API for configuring and managing Kafka consumers,
 * allowing scripts to start and stop listening to specific topics.
 */
const KafkaFacade = Java.type("org.eclipse.dirigible.components.api.kafka.KafkaFacade");

/**
 * The Consumer class acts as the main entry point for creating and configuring
 * Kafka topic consumers.
 */
export class Consumer {

    /**
     * Creates a new topic configuration wrapper that can be used to start or
     * stop listening for messages on a Kafka topic.
     *
     * @param destination The name of the Kafka topic to consume messages from.
     * @param configuration Optional key-value object containing Kafka consumer properties
     * (e.g., 'group.id', 'auto.offset.reset').
     * @returns A {@link Topic} instance configured for the specified destination and properties.
     */
    public static topic(destination: string, configuration: { [key: string]: string } = {}): Topic {
        return new Topic(destination, configuration);
    }
}

/**
 * Represents a configured Kafka topic consumer capable of starting and stopping
 * background message listening.
 */
class Topic {
    private destination: string;
    private configuration: { [key: string]: string } = {};

    /**
     * @param destination The name of the Kafka topic.
     * @param configuration Optional key-value object for consumer properties.
     */
    constructor(destination: string, configuration: { [key: string]: string } = {}) {
        this.destination = destination;
        this.configuration = configuration;
    }

    /**
     * Starts listening to the configured topic in a background process.
     *
     * @param handler The path to the script or function name that will handle the incoming Kafka messages.
     * This function should accept two arguments: `message` (string) and `headers` (object).
     * @param timeout The maximum amount of time (in milliseconds) the consumer should wait for messages.
     */
    public startListening(handler: string, timeout: number): void {
        KafkaFacade.startListening(this.destination, handler, timeout, JSON.stringify(this.configuration));
    }

    /**
     * Stops the background process that is listening to the configured topic.
     * Note: Stopping is based on matching the topic and configuration, so the same
     * configuration object used in `startListening` should be used here.
     */
    public stopListening(): void {
        KafkaFacade.stopListening(this.destination, JSON.stringify(this.configuration));
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = Consumer;
}