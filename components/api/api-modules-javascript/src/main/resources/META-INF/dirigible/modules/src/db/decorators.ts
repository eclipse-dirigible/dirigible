/**
 * @file decorators.ts
 * 
 * ECMAScript 2025-compliant ORM decorator implementation
 * Compatible with GraalJS runtime.
 * 
 * Features:
 * - Uses context.addInitializer for stable decorator timing
 * - Stores metadata in a global WeakMap cache
 * - Finalizes entity metadata once per class
 * - Defers finalization via microtask (Promise.resolve().then)
 */

// --- Decorator Context Types (based on ECMAScript Decorators proposal) ---
type ClassFieldDecoratorContext = {
  kind: "field";
  name: string | symbol;
  static: boolean;
  private: boolean;
  addInitializer(fn: () => void): void;
};

type ClassDecoratorContext = {
  kind: "class";
  name: string | symbol;
  addInitializer(fn: () => void): void;
};

// --- Metadata Models ---
export interface ColumnOptions {
  name?: string;
  type?: string;
  length?: number;
  nullable?: boolean;
  defaultValue?: string;
}

export interface OneToManyOptions {
  table?: string;
  joinColumn: string;
  cascade?: "all" | "none" | "persist" | "merge" | "remove";
  inverse?: boolean;
  lazy?: boolean;
  fetch?: "select" | "join";
  joinColumnNotNull?: boolean;
}

export interface ManyToOneOptions {
  joinColumn?: string;
  cascade?: "all" | "none" | "persist" | "merge" | "remove";
  nullable?: boolean;
  lazy?: boolean;
  fetch?: "select" | "join";
}

interface PropertyMetadata {
  propertyName: string;
  isId: boolean;
  isGenerated: boolean;
  columnOptions?: ColumnOptions;
  oneToManyOptions?: { type: Function; options: OneToManyOptions };
  manyToOneOptions?: { type: Function; options: ManyToOneOptions };
}

export interface EntityConstructor extends Function {
  new (...args: any[]): any;
  $entity_name?: string;
  $table_name?: string;
  $id_name?: string;
  $id_column?: string;
  $initialized?: boolean;
}

// --- Global Metadata Cache ---
const globalCache: WeakMap<Function, PropertyMetadata[]> =
  (globalThis as any).__decorator_metadata_cache__ ||
  ((globalThis as any).__decorator_metadata_cache__ = new WeakMap());

function getMetadataArray(constructor: Function): PropertyMetadata[] {
  if (!globalCache.has(constructor)) {
    globalCache.set(constructor, []);
  }
  return globalCache.get(constructor)!;
}

// --- Defer Helper (GraalJS-safe microtask) ---
function defer(fn: () => void): void {
  Promise.resolve().then(fn);
}

// --- Core Property Decorator Factory ---
function createPropertyDecorator(
  kind: "column" | "id" | "generated",
  options?: ColumnOptions
) {
  return function (_: any, context: ClassFieldDecoratorContext) {
    if (context.kind !== "field") {
      throw new Error(`@${kind} must apply to fields`);
    }

    context.addInitializer(function () {
      const ctor = (this as any).constructor;
      const metadataArray = getMetadataArray(ctor);

      let metadata = metadataArray.find(
        (m) => m.propertyName === context.name.toString()
      );

      if (!metadata) {
        metadata = {
          propertyName: context.name.toString(),
          isId: false,
          isGenerated: false,
        };
        metadataArray.push(metadata);
      }

      if (kind === "id") metadata.isId = true;
      if (kind === "generated") metadata.isGenerated = true;
      if (kind === "column") metadata.columnOptions = options;

      console.log(`Registered @${kind}(${context.name.toString()})`);
    });
  };
}

// --- @Entity Decorator ---
export function Entity(entityName?: string) {
  return function (value: Function, context: ClassDecoratorContext) {
    context.addInitializer(function () {
      defer(() => {
        const ctor = value as EntityConstructor;

        // Prevent duplicate registration (idempotency)
        if (ctor.$initialized) return;
        ctor.$initialized = true;

        ctor.$entity_name = entityName || ctor.name;

        const metadataArray = getMetadataArray(ctor);
        const idMetadata = metadataArray.find((m) => m.isId);

        if (idMetadata) {
          ctor.$id_name = idMetadata.propertyName;
          ctor.$id_column =
            idMetadata.columnOptions?.name ||
            idMetadata.propertyName.toUpperCase();
        }

        console.log(`[Entity] Finalized ${ctor.name}`);
      });
    });
  };
}

// --- @Table Decorator ---
export function Table(tableName?: string) {
  return function (value: Function, context: ClassDecoratorContext) {
    context.addInitializer(function () {
      const ctor = value as EntityConstructor;
      ctor.$table_name = tableName || ctor.name.toUpperCase();
    });
    return value;
  };
}

// --- Exported Property Decorators ---
export const Column = (options?: ColumnOptions) =>
  createPropertyDecorator("column", options);
export const Id = () => createPropertyDecorator("id");
export const Generated = (strategy: string) =>
  createPropertyDecorator("generated");

export function OneToMany(
  typeFunction: () => Function,
  options: OneToManyOptions
) {
  return function (_: any, context: ClassFieldDecoratorContext) {
    if (context.kind !== "field") return;

    context.addInitializer(function () {
      const ctor = (this as any).constructor;
      const metadataArray = getMetadataArray(ctor);

      let metadata = metadataArray.find(
        (m) => m.propertyName === context.name.toString()
      );

      if (!metadata) {
        metadata = {
          propertyName: context.name.toString(),
          isId: false,
          isGenerated: false,
        };
        metadataArray.push(metadata);
      }

      metadata.oneToManyOptions = { type: typeFunction(), options };
      console.log(`Registered @OneToMany(${context.name.toString()})`);
    });
  };
}

export function ManyToOne(
  typeFunction: () => Function,
  options: ManyToOneOptions = {}
) {
  return function (_: any, context: ClassFieldDecoratorContext) {
    if (context.kind !== "field") return;

    context.addInitializer(function () {
      const ctor = (this as any).constructor;
      const metadataArray = getMetadataArray(ctor);

      let metadata = metadataArray.find(
        (m) => m.propertyName === context.name.toString()
      );

      if (!metadata) {
        metadata = {
          propertyName: context.name.toString(),
          isId: false,
          isGenerated: false,
        };
        metadataArray.push(metadata);
      }

      metadata.manyToOneOptions = { type: typeFunction(), options };
      console.log(`Registered @ManyToOne(${context.name.toString()})`);
    });
  };
}
