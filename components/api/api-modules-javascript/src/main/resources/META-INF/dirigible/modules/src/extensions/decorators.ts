const EXTENSION_METADATA_KEY = Symbol("extension:metadata");

export interface ExtensionOptions {
    name: string; // e.g., "MyExtension"
    to: string;      // e.g., "my-extension-point"
}

/**
 * @Extension decorator
 * Marks an entire class as a extension
 *
 * introduced in TypeScript 5.0, which expects a ClassDecoratorContext object.
 */
export function Extension(options: ExtensionOptions) {
    
    return function <T extends abstract new (...args: any) => any>(target: T, context: ClassDecoratorContext<T>) {
        
        if (context.kind !== 'class') {
            throw new Error(`@Extension can only be used on classes.`);
        }

        Object.defineProperty(target, EXTENSION_METADATA_KEY, {
            value: {
                name: options.name,
                to: options.to
            },
            writable: false,
            configurable: false,
            enumerable: true
        });
    };
}
