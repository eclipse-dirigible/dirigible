/**
 * @file decorators.ts
 * Hybrid Decorator Implementation — works in both GraalJS and modern JS runtimes.
 */

const ComponentMetadataRegistry = Java.type("org.eclipse.dirigible.components.engine.javascript.parser.ComponentMetadataRegistry");
const ComponentFacade = Java.type("org.eclipse.dirigible.components.api.component.ComponentFacade");

interface ComponentMetadata {
  name?: string;
  injections: Map<string | symbol, string | undefined>;
}

export interface ComponentConstructor extends Function {
  new (...args: any[]): any;
  __component_name?: string;
  __injections_map?: Map<string | symbol, string | undefined>;
}

const componentCache: WeakMap<object, ComponentMetadata> = new WeakMap();

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

export function Component<T extends ComponentConstructor>(constructor: T): T;
export function Component(name?: string): <T extends ComponentConstructor>(constructor: T) => T;

export function Component(nameOrConstructor?: any): any {
  if (typeof nameOrConstructor === "function") {
    const constructor = nameOrConstructor as ComponentConstructor;
    const metadata = getComponentMetadata(constructor);
    metadata.name = constructor.name;

    constructor.__component_name = metadata.name;
    constructor.__injections_map = metadata.injections;

    ComponentMetadataRegistry.register(metadata.name, metadata.injections);

    return class extends constructor {
      constructor(...args: any[]) {
        super(...args);
        ComponentFacade.injectDependencies(this);
      }
    };
  }

  const name = nameOrConstructor as string | undefined;
  return function <T extends ComponentConstructor>(constructor: T) {
    const metadata = getComponentMetadata(constructor);
    metadata.name = name || constructor.name;

    constructor.__component_name = metadata.name;
    constructor.__injections_map = metadata.injections;

    ComponentMetadataRegistry.register(metadata.name, metadata.injections);

    return class extends constructor {
      constructor(...args: any[]) {
        super(...args);
        ComponentFacade.injectDependencies(this);
      }
    };
  };
}

export function Inject(name?: string) {
  return function (...args: any[]) {
    if (args.length === 2) {
      const [_, context] = args;
      if (context && typeof context.addInitializer === "function") {
        context.addInitializer(function () {
          const ctor = (this as any).constructor as ComponentConstructor;
          const metadata = getComponentMetadata(ctor);
          metadata.injections.set(context.name, name);
          ctor.__injections_map = metadata.injections;
        });
        return;
      }
    }

    const [target, propertyKey] = args;
    const ctor = target.constructor as ComponentConstructor;
    const metadata = getComponentMetadata(ctor);
    metadata.injections.set(propertyKey, name);
    ctor.__injections_map = metadata.injections;
  };
}
