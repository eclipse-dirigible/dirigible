/**
 * API DI (Dependency Injection) Decorators
 * * Provides a hybrid implementation of decorators for Dependency Injection (DI)
 * that supports both legacy JavaScript environments (like Mozilla Rhino or older GraalJS)
 * using the `(target, propertyKey)` signature, and modern JavaScript environments
 * using the decorator metadata and `context.addInitializer`.
 */

const ComponentMetadataRegistry = Java.type(
  "org.eclipse.dirigible.components.engine.di.parser.ComponentMetadataRegistry"
);
const ComponentFacade = Java.type(
  "org.eclipse.dirigible.components.api.component.ComponentFacade"
);

/**
 * Interface defining the metadata collected for a component class.
 */
interface ComponentMetadata {
  /** The registered name of the component, defaults to the class name. */
  name?: string;
  /** A map of property keys (injection points) to their optional component names for injection. */
  injections: Map<string | symbol, string | undefined>;
}

/**
 * Extends a standard constructor function to include properties used for internal metadata storage.
 */
export interface ComponentConstructor extends Function {
  new (...args: any[]): any;
  /** Internal storage for the component's registered name. */
  __component_name?: string;
  /** Internal storage for the component's injection map. */
  __injections_map?: Map<string | symbol, string | undefined>;
}

/** Cache to store metadata, using a WeakMap for memory management. */
const componentCache: WeakMap<object, ComponentMetadata> = new WeakMap();

/**
 * Retrieves or creates the component metadata for a given class constructor.
 * Uses a WeakMap cache to ensure metadata is consistent and automatically garbage collected
 * when the constructor is no longer referenced.
 * @param constructor The class constructor object.
 * @returns The component metadata object.
 */
function getComponentMetadata(constructor: object): ComponentMetadata {
  if (typeof constructor !== "object" && typeof constructor !== "function") {
    throw new TypeError("Invalid constructor passed to getComponentMetadata");
  }

  if (!componentCache.has(constructor)) {
    componentCache.set(constructor, {
      injections: new Map(),
    });
  }
  return componentCache.get(constructor)!;
}

/**
 * Class decorator that marks a class as a Dependency Injection component.
 * It registers the component's metadata and wraps the constructor to ensure
 * dependencies are injected upon instance creation.
 *
 * Can be used as `@Component` (uses class name) or `@Component('customName')`.
 *
 * @param nameOrConstructor The optional custom component name (string) or the class constructor (Function) when used without parentheses.
 * @returns A class decorator function or the decorated class itself.
 */
export function Component(nameOrConstructor?: string | Function): Function {
  // Handle case: @Component class MyClass {} (nameOrConstructor is the constructor)
  if (typeof nameOrConstructor === "function") {
    const constructor = nameOrConstructor as ComponentConstructor;
    const metadata = getComponentMetadata(constructor);
    metadata.name = constructor.name;

    constructor.__component_name = metadata.name;
    constructor.__injections_map = metadata.injections;

    ComponentMetadataRegistry.register(metadata.name, metadata.injections);

    return class extends (constructor as any) {
      constructor(...args: any[]) {
        super(...args);
        // Perform runtime dependency injection
        ComponentFacade.injectDependencies(this);
      }
    };
  }

  // Handle case: @Component('customName') class MyClass {} (nameOrConstructor is the custom name)
  const name = nameOrConstructor;
  return function (constructor: Function): any {
    const metadata = getComponentMetadata(constructor);
    metadata.name = name || constructor.name;

    const ctor = constructor as ComponentConstructor;
    ctor.__component_name = metadata.name;
    ctor.__injections_map = metadata.injections;

    ComponentMetadataRegistry.register(metadata.name, metadata.injections);

    return class extends (constructor as any) {
      constructor(...args: any[]) {
        super(...args);
        // Perform runtime dependency injection
        ComponentFacade.injectDependencies(this);
      }
    };
  };
}

/**
 * An alias for the {@link Component} decorator, used for semantic clarity
 * in contexts where the class is explicitly intended to be injected elsewhere.
 * Its functionality is identical to {@link Component}.
 *
 * @param nameOrConstructor The optional custom component name (string) or the class constructor (Function).
 * @returns A class decorator function or the decorated class itself.
 */
export function Injected(nameOrConstructor?: string | Function): Function {
  // Logic is identical to Component
  if (typeof nameOrConstructor === "function") {
    const constructor = nameOrConstructor as ComponentConstructor;
    const metadata = getComponentMetadata(constructor);
    metadata.name = constructor.name;

    constructor.__component_name = metadata.name;
    constructor.__injections_map = metadata.injections;

    ComponentMetadataRegistry.register(metadata.name, metadata.injections);

    return class extends (constructor as any) {
      constructor(...args: any[]) {
        super(...args);
        ComponentFacade.injectDependencies(this);
      }
    };
  }

  const name = nameOrConstructor;
  return function (constructor: Function): any {
    const metadata = getComponentMetadata(constructor);
    metadata.name = name || constructor.name;

    const ctor = constructor as ComponentConstructor;
    ctor.__component_name = metadata.name;
    ctor.__injections_map = metadata.injections;

    ComponentMetadataRegistry.register(metadata.name, metadata.injections);

    return class extends (constructor as any) {
      constructor(...args: any[]) {
        super(...args);
        ComponentFacade.injectDependencies(this);
      }
    };
  };
}

/**
 * Property decorator used to mark an instance property as an injection point.
 * The runtime will look up a dependency based on the property name or the optional
 * name provided.
 *
 * It supports both modern (`context.addInitializer`) and legacy decorator APIs.
 *
 * Usage:
 * - `@Inject` propertyKey: Injects a component with the same name as `propertyKey`.
 * - `@Inject('myService')` propertyKey: Injects the component registered as 'myService'.
 *
 * @param name The optional name of the component to inject.
 * @returns A property decorator function.
 */
export function Inject(name?: string) {
  return function (...args: any[]) {
    // Modern Decorator API check (GraalJS, V8, etc.) - args: [value, context]
    if (args.length === 2) {
      const [_, context] = args;
      if (context && typeof context.addInitializer === "function") {
        context.addInitializer(function () {
          // 'this' is the class instance in the initializer
          const ctor = (this as any)?.constructor as ComponentConstructor;
          const metadata = getComponentMetadata(ctor);
          // Register the injection point (property name) and the optional target name
          metadata.injections.set(context.name, name);
          ctor.__injections_map = metadata.injections;
        });
        return;
      }
    }

    // Legacy Decorator API fallback (Rhino, older GraalJS) - args: [target, propertyKey]
    const [target, propertyKey] = args;
    const ctor = target.constructor || target;
    const metadata = getComponentMetadata(ctor);
    // Register the injection point (propertyKey) and the optional target name
    metadata.injections.set(propertyKey, name);
    (ctor as ComponentConstructor).__injections_map = metadata.injections;
  };
}