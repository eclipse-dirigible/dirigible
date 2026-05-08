/**
 * @module messaging/decorators
 * @package @aerokit/sdk/messaging
 * @name Decorators
 * @overview
 * 
 * This module provides a set of decorators for defining messaging consumers in a declarative manner. The decorators allow developers to annotate classes to specify that they should act as consumers for specific messaging destinations, such as queues or topics. This makes it easier to create and manage message-driven components within the application.
 * 
 * ### Key Features:
 * - **Listener Decorator**: The `@Listener` decorator is used to mark a class as a messaging consumer, specifying the destination name and type (e.g., Queue or Topic).
 * 
 * ### Use Cases:
 * - **Message-Driven Applications**: These decorators are primarily used for defining components that react to messages received from queues or topics, enabling event-driven architectures.
 * - **Integration with Messaging Systems**: By using these decorators, developers can easily integrate their applications with various messaging systems that support JMS-style patterns.
 * 
 * ### Example Usage:
 * ```ts
 * import { Listener } from "@aerokit/sdk/messaging";
 * 
 * @Listener({ name: "orders.queue", kind: "Queue" })
 * class OrderConsumer {
 *     // Logic to consume messages from 'orders.queue'
 * }
 * 
 * @Listener({ name: "market.updates.topic", kind: "Topic" })
 * class MarketUpdatesConsumer {
 *     // Logic to consume messages from 'market.updates.topic'
 * }
 * 
 * // The consumers will be automatically registered based on the provided metadata.
 * ```
 */

const LISTENER_METADATA_KEY = Symbol("listener:metadata");

export interface ListenerOptions {
    name: string; // e.g., "MyQueue"
    kind: string;      // e.g., "Queue"
}

/**
 * @Listener decorator
 * Marks an entire class as a listener
 *
 * introduced in TypeScript 5.0, which expects a ClassDecoratorContext object.
 */
export function Listener(options: ListenerOptions) {
    
    return function <T extends abstract new (...args: any) => any>(target: T, context: ClassDecoratorContext<T>) {
        
        if (context.kind !== 'class') {
            throw new Error(`@Listener can only be used on classes.`);
        }

        Object.defineProperty(target, LISTENER_METADATA_KEY, {
            value: {
                name: options.name,
                kind: options.kind
            },
            writable: false,
            configurable: false,
            enumerable: true
        });
    };
}
