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
