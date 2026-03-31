/**
 * @module extensions/decorators
 * @package @aerokit/sdk/extensions
 * @name Decorators
 * @overview
 * 
 * This module provides a set of decorators for defining and managing extensions within the Dirigible environment. The decorators are designed to be compatible with both legacy JavaScript environments (like Mozilla Rhino or older GraalJS) and modern JavaScript environments that support the latest decorator specifications.
 * 
 * ### Key Features
 * - **Extension Decorator**: Marks a class as an extension, allowing it to be registered and associated with a specific extension point in the Dirigible application.
 * - **Metadata Storage**: Uses symbols to store extension metadata on the class, ensuring that the metadata is encapsulated and does not interfere with other properties or methods.
 * - **Hybrid Decorator Support**: The decorators are implemented to work seamlessly in both legacy and modern JavaScript environments, ensuring broad compatibility across different runtime contexts.
 * 
 * ### Use Cases
 * - Defining extensions that can be dynamically loaded and integrated into the application at runtime.
 * - Associating extensions with specific extension points to enable modular and extensible application design.
 * - Leveraging the extension mechanism to allow for third-party contributions or customizations without modifying the core application code.
 * 
 * ### Example Usage
 * ```ts
 * import { Extension } from "@aerokit/sdk/extensions";
 * 
 * @Extension({ name: "MyExtension", to: "my-extension-point" })
 * class MyExtension {
 *   // Extension implementation
 * }
 * ```
 */

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
