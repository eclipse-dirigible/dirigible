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
