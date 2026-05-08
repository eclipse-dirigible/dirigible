/**
 * @module net/decorators
 * @package @aerokit/sdk/net
 * @name Decorators
 * @overview
 * 
 * This module provides a set of decorators for defining WebSocket endpoints in a declarative manner. The decorators allow developers to annotate classes to specify that they should act as WebSocket handlers, defining the endpoint and associated metadata. This makes it easier to create and manage WebSocket-based communication within the application.
 * 
 * ### Key Features:
 * - **WebSocket Decorator**: The `@Websocket` decorator is used to mark a class as a WebSocket handler, specifying the endpoint and associated metadata.
 * 
 * ### Use Cases:
 * - **Real-Time Communication**: These decorators are primarily used for defining components that handle WebSocket connections, enabling real-time communication between clients and the server.
 * - **Integration with WebSocket Systems**: By using these decorators, developers can easily integrate their applications with WebSocket-based systems and protocols.
 * 
 * ### Example Usage:
 * ```ts
 * import { Websocket } from "@aerokit/sdk/net";
 * 
 * @Websocket({ name: "MyWebsocket", endpoint: "my-ws" })
 * class MyWebsocketHandler {
 *     // Logic to handle WebSocket connections at the 'my-ws' endpoint
 * }
 * 
 * // The WebSocket handler will be automatically registered based on the provided metadata.
 * ```
 */

const WEBSOCKET_METADATA_KEY = Symbol("websocket:metadata");

export interface WebsocketOptions {
    name: string; // e.g., "MyWebscoket"
    endpoint: string;      // e.g., "my-ws"
}

/**
 * @Websocket decorator
 * Marks an entire class as a websocket
 *
 * introduced in TypeScript 5.0, which expects a ClassDecoratorContext object.
 */
export function Websocket(options: WebsocketOptions) {
    
    return function <T extends abstract new (...args: any) => any>(target: T, context: ClassDecoratorContext<T>) {
        
        if (context.kind !== 'class') {
            throw new Error(`@Websocket can only be used on classes.`);
        }

        Object.defineProperty(target, WEBSOCKET_METADATA_KEY, {
            value: {
                name: options.name,
                endpoint: options.endpoint
            },
            writable: false,
            configurable: false,
            enumerable: true
        });
    };
}
